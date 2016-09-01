package io.taig.communicator.websocket

import io.taig.communicator.OkHttpRequest
import monix.reactive.OverflowStrategy
import okhttp3.OkHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps

trait WebSocketChannels[T] {
    def reader: WebSocketReader[T]

    def writer: WebSocketWriter[T]

    def close(): Unit
}

object WebSocketChannels {
    def apply[T: Decoder: Encoder](
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]] = Default.strategy,
        reconnect: Option[FiniteDuration]                 = Default.reconnect
    )(
        implicit
        client: OkHttpClient
    ): WebSocketChannels[T] = {
        val writer = WebSocketWriter[T]

        val reader = WebSocketReader(
            request,
            strategy,
            reconnect
        ).map {
            case open @ Event.Open( socket, _ ) ⇒
                writer.connect( socket )
                open
            case close @ Event.Close( _, _ ) ⇒
                writer.disconnect()
                close
            case event ⇒ event
        }

        new WebSocketChannelsImpl[T]( reader, writer )
    }

    def unapply[T](
        channels: WebSocketChannels[T]
    ): Option[( WebSocketReader[T], WebSocketWriter[T] )] = {
        Some( channels.reader, channels.writer )
    }
}

private class WebSocketChannelsImpl[T](
        val reader: WebSocketReader[T],
        val writer: WebSocketWriter[T]
) extends WebSocketChannels[T] {
    override def close() = writer.close( Close.Normal, None )
}