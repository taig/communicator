package io.taig.communicator.websocket

import java.io.IOException

import io.taig.communicator.OkHttpRequest
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.observers.{ BufferedSubscriber, Subscriber }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

case class WebSocketReader[T] private[websocket] (
        request:        OkHttpRequest,
        strategy:       OverflowStrategy.Synchronous[Event[T]],
        reconnect:      Option[FiniteDuration],
        onConnected:    OkHttpWebSocket ⇒ Unit,
        onDisconnected: () ⇒ Unit
)(
        implicit
        client:  OkHttpClient,
        decoder: Decoder[T]
) extends Observable[Event[T]] {
    val channel: Observable[Event[T]] = Observable.unsafeCreate { subscriber ⇒
        import subscriber.scheduler

        val proxy = reconnect.fold( subscriber ) { reconnect ⇒
            new ReconnectingSubscriberProxy[Event[T]](
                subscriber,
                this,
                reconnect,
                onDisconnected
            )
        }

        val out = BufferedSubscriber.synchronous( proxy, strategy )

        val cancelable = WebSocket( request ) {
            new WebSocketReaderListener[T]( out )
        }.runAsync.andThen {
            case Success( ( socket, payload ) ) ⇒
                onConnected( socket )
                subscriber.onNext( Event.Open( socket, payload ) )
            case Failure( exception ) ⇒ subscriber.onError( exception )
        }

        Cancelable { () ⇒
            cancelable.cancel()
            cancelable.foreach {
                case ( socket, _ ) ⇒ socket.close( Close.GoingAway, "Bye." )
            }
        }
    }

    override def unsafeSubscribeFn( subscriber: Subscriber[Event[T]] ) = {
        channel.unsafeSubscribeFn( subscriber )
    }
}

object WebSocketReader {
    def apply[T](
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]] = Default.strategy,
        reconnect: Option[FiniteDuration]                 = Default.reconnect
    )(
        implicit
        client:  OkHttpClient,
        decoder: Decoder[T]
    ): WebSocketReader[T] = {
        WebSocketReader( request, strategy, reconnect, _ ⇒ (), () ⇒ () )
    }
}

private class ReconnectingSubscriberProxy[T](
        subscriber:     Subscriber[T],
        observable:     Observable[T],
        delay:          FiniteDuration,
        onDisconnected: () ⇒ Unit
) extends Subscriber[T] {
    override implicit def scheduler = subscriber.scheduler

    override def onNext( value: T ) = subscriber.onNext( value )

    override def onError( exception: Throwable ) = {
        subscriber.onError( exception )

        logger.debug( s"WebSocket failure, reconnecting in $delay", exception )

        onDisconnected()

        observable
            .delaySubscription( delay )
            .unsafeSubscribeFn( subscriber )
    }

    override def onComplete() = {
        subscriber.onComplete()

        // TODO When the server closes the connection, we want to reconnect
        // logger.debug( s"Websocket closed, reconnecting in $delay" )

        //        observable
        //            .delaySubscription( delay )
        //            .unsafeSubscribeFn( subscriber )
    }
}

private class WebSocketReaderListener[T: Decoder](
        downstream: Subscriber.Sync[Event[T]]
) extends WebSocketListener[T] {
    var socket: Option[OkHttpWebSocket] = None

    def handle( event: Event[T] ): Unit = {
        if ( downstream.onNext( event ) == Stop ) {
            socket.foreach {
                _.close( Close.GoingAway, "Bye." )
            }
        }
    }

    override def onMessage( message: T ) = {
        handle( Event.Message( message ) )
    }

    override def onPong( payload: Option[T] ) = {
        handle( Event.Pong( payload ) )
    }

    override def onClose( code: Int, reason: Option[String] ) = {
        handle( Event.Close( code, reason ) )
        downstream.onComplete()
    }

    override def onFailure( exception: IOException, response: Option[T] ) = {
        handle( Event.Failure( exception, response ) )
        downstream.onError( exception )
    }
}