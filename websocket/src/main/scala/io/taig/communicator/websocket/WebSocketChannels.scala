package io.taig.communicator.websocket

import io.taig.communicator.OkHttpRequest
import monix.reactive.OverflowStrategy
import okhttp3.OkHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps

trait WebSocketChannels[T] {
    def writer: BufferedWebSocketWriter[T]

    def reader: WebSocketReader[T]

    def close(): Unit
}

object WebSocketChannels {
    def apply[T: Codec](
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]] = OverflowStrategy.Unbounded,
        reconnect: Option[FiniteDuration]                 = Some( 3 seconds )
    )(
        implicit
        client: OkHttpClient
    ): WebSocketChannels[T] = {
        val writer = BufferedWebSocketWriter()

        val reader = WebSocketReader(
            request,
            strategy,
            reconnect,
            socket ⇒ writer.connect( socket ),
            () ⇒ writer.disconnect()
        )

        new OkHttpWebSocketChannels[T]( writer, reader )
    }

    def unapply[T](
        channels: WebSocketChannels[T]
    ): Option[( WebSocketWriter[T], WebSocketReader[T] )] = {
        Some( channels.writer, channels.reader )
    }
}

private class OkHttpWebSocketChannels[T](
        val writer: BufferedWebSocketWriter[T],
        val reader: WebSocketReader[T]
) extends WebSocketChannels[T] {
    override def close() = writer.close( Close.Normal, None )
}