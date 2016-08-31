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
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

class WebSocketReader[T] private[websocket] (
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]],
        reconnect: Option[FiniteDuration]
)(
        implicit
        c: OkHttpClient,
        d: Decoder[T]
) extends Observable[Event[T]] { self ⇒
    val channel: Observable[Event[T]] = {
        Observable.create( strategy ) { subscriber ⇒
            import subscriber.scheduler

            val proxy = reconnect.fold[Subscriber[Event[T]]]( subscriber ) {
                new ReconnectingSubscriberProxy[Event[T]](
                    this,
                    subscriber,
                    _
                )
            }

            val cancelable = WebSocket[T]( request ) {
                new WebSocketReaderListener[T]( _, proxy, subscriber )
            }.runAsync

            cancelable.onComplete {
                case Success( ( socket, message ) ) ⇒
                    WebSocketReader.handle(
                        socket,
                        proxy,
                        Event.Open( socket, message )
                    )
                case Failure( exception ) ⇒ subscriber.onError( exception )
            }

            Cancelable { () ⇒
                cancelable.onSuccess {
                    case ( socket, _ ) ⇒
                        socket.close( Close.GoingAway, Some( "Bye." ) )
                }
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
        socket:     WebSocket[T],
        subscriber: Subscriber[Event[T]],
        event:      Event[T]
    ): Unit = {
        if ( subscriber.onNext( event ) == Stop ) {
            socket.close( Close.GoingAway, Some( "Bye." ) )
        }
    }
}

private class WebSocketReaderListener[T: Decoder](
        socket:     WebSocket[T],
        proxy:      Subscriber[Event[T]],
        subscriber: Subscriber[Event[T]]
) extends WebSocketListener[T]( socket ) {
    override def onMessage( message: T ) = {
        WebSocketReader.handle(
            socket,
            proxy,
            Event.Message( message )
        )
    }

    override def onPong( payload: Option[T] ) = {
        WebSocketReader.handle(
            socket,
            proxy,
            Event.Pong( payload )
        )
    }

    override def onFailure( exception: IOException, response: Option[T] ) = {
        WebSocketReader.handle(
            socket,
            proxy,
            Event.Failure( exception, response )
        )

        proxy.onError( exception )
    }

    override def onClose( code: Int, reason: Option[String] ) = {
        WebSocketReader.handle(
            socket,
            proxy,
            Event.Close( code, reason )
        )

        if ( socket.isClosed ) {
            subscriber.onComplete()
        } else {
            proxy.onComplete()
        }
    }
}

private class ReconnectingSubscriberProxy[T](
        observable: Observable[T],
        subscriber: Subscriber[T],
        delay:      FiniteDuration
) extends Subscriber[T] {
    override implicit val scheduler = subscriber.scheduler

    override def onNext( value: T ) = subscriber.onNext( value )

    override def onError( exception: Throwable ) = {
        logger.debug( s"WebSocket failure, reconnecting in $delay", exception )

        observable
            .delaySubscription( delay )
            .unsafeSubscribeFn( subscriber )
    }

    override def onComplete() = {
        logger.debug( s"WebSocket closed, reconnecting in $delay" )

        observable
            .delaySubscription( delay )
            .unsafeSubscribeFn( subscriber )
    }
}