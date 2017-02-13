package io.taig.communicator.phoenix

import java.util.concurrent.TimeUnit.MILLISECONDS

import cats.syntax.either._
import io.circe.parser._
import io.circe.syntax._
import io.circe.Json
import io.taig.communicator.{ OkHttpRequest, OkHttpWebSocket }
import io.taig.phoenix.models._
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.concurrent.duration.Duration.{ Inf, Infinite }

class Phoenix(
        socket:     OkHttpWebSocket,
        val stream: Observable[Inbound],
        connection: Cancelable,
        heartbeat:  Cancelable,
        timeout:    Duration
) extends io.taig.phoenix.Phoenix[Observable, Task] {
    override def join(
        topic:   Topic,
        payload: Json  = Json.Null
    ): Task[Either[Option[Response.Error], Channel]] =
        Channel.join( topic, payload )(
            socket,
            stream.filter( topic isSubscribedTo _.topic ),
            timeout
        )

    override def close(): Unit = {
        val close = socket.close( 1000, null )

        if ( close ) logger.debug( "Closing connection gracefully" )
        else {
            logger.debug {
                "Cancelling connection, because socket can not be closed " +
                    "gracefully"
            }

            connection.cancel()
        }
    }
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[WebSocket.Event] = OverflowStrategy.Unbounded,
        heartbeat: Option[FiniteDuration]                        = Default.heartbeat
    )(
        implicit
        ohc: OkHttpClient,
        s:   Scheduler
    ): Task[Phoenix] = Task.defer {
        var heartbeats = Cancelable.empty

        val observable = WebSocket( request, strategy )
            .doOnNext {
                case WebSocket.Event.Open( _, _ ) ⇒
                    logger.debug( s"Opened socket connection" )
                case WebSocket.Event.Message( Right( message ) ) ⇒
                    logger.debug( s"Received message: $message" )
                case WebSocket.Event.Closing( code, _ ) ⇒
                    logger.debug( s"Closing connection: $code" )
                case WebSocket.Event.Closed( code, _ ) ⇒
                    logger.debug( s"Closed connection: $code" )
                case _ ⇒ //
            }
            .doOnError( logger.error( "WebSocket connection failed", _ ) )
            .doOnTerminate { _ ⇒
                logger.debug( "Terminated connection" )
                synchronized( heartbeats.cancel() )
            }
            .doOnSubscriptionCancel { () ⇒
                logger.debug( "Cancelled connection" )
                synchronized( heartbeats.cancel() )
            }
            .publish

        val connection = observable.connect()

        val timeout = ohc.readTimeoutMillis match {
            case 0            ⇒ Inf
            case milliseconds ⇒ Duration( milliseconds.toLong, MILLISECONDS )
        }

        observable.collect {
            case WebSocket.Event.Open( socket, _ ) ⇒ synchronized {
                heartbeats = heartbeat.fold( heartbeats ) { interval ⇒
                    this.heartbeat( interval )
                        .doOnSubscriptionCancel { () ⇒
                            logger.debug( "Cancelling heartbeat" )
                        }
                        .foreach { request ⇒
                            logger.debug( s"Sending heartbeat: $request" )
                            socket.send( request.asJson.noSpaces )
                            ()
                        }
                }

                val stream = observable.collect {
                    case WebSocket.Event.Message( Right( message ) ) ⇒
                        decode[Inbound]( message ).valueOr( throw _ )
                }

                new Phoenix(
                    socket,
                    stream,
                    connection,
                    heartbeats,
                    timeout
                )
            }
        }.firstL
    }

    def send(
        topic:   Topic,
        event:   Event,
        payload: Json  = Json.Null,
        ref:     Ref   = Ref.unique()
    )(
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: Duration
    ): Task[Option[Response]] = {
        val request = Request( topic, event, payload, ref )

        val channel = stream
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL

        val withTimeout = timeout match {
            case _: Infinite ⇒ channel
            case timeout: FiniteDuration ⇒
                channel.timeout( timeout ).onErrorRecover {
                    case _: TimeoutException ⇒ None
                }
        }

        val send = Task {
            socket.send( request.asJson.noSpaces )
        }

        Task.mapBoth( withTimeout, send )( ( left, _ ) ⇒ left )
    }

    def heartbeat( interval: FiniteDuration ): Observable[Request] =
        Observable.intervalWithFixedDelay( interval, interval ).map { _ ⇒
            Request( Topic.Phoenix, Event( "heartbeat" ) )
        }
}