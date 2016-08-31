package io.taig.communicator.websocket

import io.taig.communicator.OkHttpRequest
import monix.reactive.OverflowStrategy
import okhttp3.OkHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps

trait WebSocketChannels[I, O] { self ⇒
    def reader: WebSocketReader[I]

    def writer: BufferedWebSocketWriter[O]

    def close(): Unit
}

object WebSocketChannels {
    def apply[I: Decoder, O: Encoder](
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[I]] = Default.strategy,
        reconnect: Option[FiniteDuration]                 = Default.reconnect
    )(
        implicit
        client: OkHttpClient
    ): BufferedWebSocketChannels[I, O] = {
        val writer = BufferedWebSocketWriter()

        val reader = ???
        //        val reader = WebSocketReader(
        //            request,
        //            strategy,
        //            reconnect,
        //            socket ⇒ writer.connect( socket ),
        //            () ⇒ writer.disconnect()
        //        )

        new BufferedWebSocketChannels[I, O]( reader, writer )
    }

    def symmetric[T: Encoder: Decoder](
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[Event[T]] = OverflowStrategy.Unbounded,
        reconnect: Option[FiniteDuration]                 = Some( 3 seconds )
    )(
        implicit
        client: OkHttpClient
    ): WebSocketChannels[T, T] = {
        WebSocketChannels[T, T]( request, strategy, reconnect )
    }

    def unapply[I, O](
        channels: WebSocketChannels[I, O]
    ): Option[( WebSocketReader[I], WebSocketWriter[O] )] = {
        Some( channels.reader, channels.writer )
    }
}

class BufferedWebSocketChannels[I, O](
        val reader: WebSocketReader[I],
        val writer: BufferedWebSocketWriter[O]
) extends WebSocketChannels[I, O] {
    override def close() = writer.close( Close.Normal, None )
}