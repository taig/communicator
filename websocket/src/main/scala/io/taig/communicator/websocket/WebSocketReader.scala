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

class WebSocketReader[T] private[websocket] (
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]],
        reconnect: Option[FiniteDuration]
)(
        implicit
        c: OkHttpClient,
        d: Decoder[T]
) extends Observable[Event[T]] {
    val channel: Observable[Event[T]] = Observable.unsafeCreate { subscriber ⇒
        import subscriber.scheduler

        val proxy = reconnect.fold( subscriber ) { reconnect ⇒
            new ReconnectingSubscriberProxy[Event[T]](
                subscriber,
                this,
                reconnect
            )
        }

        val out = BufferedSubscriber.synchronous( proxy, strategy )

        val cancelable = WebSocket( request ) { socket ⇒
            new WebSocketReaderListener[T]( socket, out )
        }.runAsync

        cancelable.onComplete {
            case Success( ( socket, message ) ) ⇒
                WebSocketReader.handle(
                    socket,
                    out,
                    Event.Open( socket, message )
                )
            case Failure( exception ) ⇒ out.onError( exception )
        }

        Cancelable { () ⇒
            cancelable.onSuccess {
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
        c: OkHttpClient,
        d: Decoder[T]
    ): WebSocketReader[T] = {
        new WebSocketReader( request, strategy, reconnect )
    }

    private[websocket] def handle[T](
        socket:     OkHttpWebSocket,
        downstream: Subscriber.Sync[Event[T]],
        event:      Event[T]
    ): Unit = {
        if ( downstream.onNext( event ) == Stop ) {
            socket.close( Close.GoingAway, "Bye." )
        }
    }
}

private class WebSocketReaderListener[T: Decoder](
        socket:     OkHttpWebSocket,
        downstream: Subscriber.Sync[Event[T]]
) extends WebSocketListener[T]( socket ) {
    override def onMessage( message: T ) = {
        WebSocketReader.handle(
            socket,
            downstream,
            Event.Message( message )
        )
    }

    override def onPong( payload: Option[T] ) = {
        WebSocketReader.handle(
            socket,
            downstream,
            Event.Pong( payload )
        )
    }

    override def onFailure( exception: IOException, response: Option[T] ) = {
        WebSocketReader.handle(
            socket,
            downstream,
            Event.Failure( exception, response )
        )

        downstream.onError( exception )
    }

    override def onClose( code: Int, reason: Option[String] ) = {
        WebSocketReader.handle(
            socket,
            downstream,
            Event.Close( code, reason )
        )

        downstream.onComplete()
    }
}

private class ReconnectingSubscriberProxy[T](
        subscriber: Subscriber[T],
        observable: Observable[T],
        delay:      FiniteDuration
) extends Subscriber[T] {
    override implicit def scheduler = subscriber.scheduler

    override def onNext( value: T ) = subscriber.onNext( value )

    override def onError( exception: Throwable ) = {
        logger.debug( s"WebSocket failure, reconnecting in $delay", exception )

        observable
            .delaySubscription( delay )
            .unsafeSubscribeFn( subscriber )
    }

    override def onComplete() = {
        subscriber.onComplete()

        // TODO When the server closes the connection, we want to reconnect
        //        logger.debug( s"Websocket closed, reconnecting in $delay" )

        //        observable
        //            .delaySubscription( delay )
        //            .unsafeSubscribeFn( subscriber )
    }
}