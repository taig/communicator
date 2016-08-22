package io.taig.communicator.websocket

import io.taig.communicator.OkHttpRequest
import monix.eval.Task
import okhttp3.OkHttpClient
import okio.Buffer

trait WebSocketWriter[T] { self ⇒
    def send( value: T ): Unit

    def ping( value: Option[T] = None ): Unit

    def close( code: Int, reason: Option[String] ): Unit
}

object WebSocketWriter {
    def apply[T: Encoder]( request: OkHttpRequest )(
        implicit
        client: OkHttpClient
    ): Task[WebSocketWriter[T]] = {
        WebSocket.pure[T]( request ).map( new OkHttpWebSocketWriter[T]( _ ) )
    }
}

private class OkHttpWebSocketWriter[T: Encoder]( socket: OkHttpWebSocket )
        extends WebSocketWriter[T] {
    override def send( value: T ) = {
        logger.debug {
            s"""
               |Sending message
               |  Payload: $value
            """.stripMargin.trim
        }

        socket.sendMessage( Encoder[T].encode( value ) )
    }

    override def ping( value: Option[T] ) = {
        logger.debug {
            s"""
               |Sending ping
               |  Payload: $value
            """.stripMargin.trim
        }

        val sink = value.map { value ⇒
            val sink = new Buffer
            val request = Encoder[T].encode( value )

            try {
                request.writeTo( sink )
                sink
            } finally {
                sink.close()
            }
        }

        socket.sendPing( sink.orNull )
    }

    override def close( code: Int, reason: Option[String] ) = {
        logger.debug {
            s"""
               |Sending close
               |  Code:   $code
               |  Reason: $reason
            """.stripMargin.trim
        }

        try {
            socket.close( code, reason.orNull )
        } catch {
            case e: IllegalStateException if e.getMessage == "closed" ⇒
                logger.debug( "Socket already closed" )
        }
    }
}

trait BufferedWebSocketWriter[T] extends WebSocketWriter[T] {
    /**
     * Send via websocket immediately or drop if unavailable
     */
    def sendNow( value: T ): Unit

    def connect( socket: OkHttpWebSocket ): this.type

    def disconnect(): this.type
}

object BufferedWebSocketWriter {
    def apply[T: Encoder](): BufferedWebSocketWriter[T] = {
        new BufferedOkHttpWebSocketWriter[T]
    }
}

private class BufferedOkHttpWebSocketWriter[T: Encoder]
        extends BufferedWebSocketWriter[T] {
    import BufferedOkHttpWebSocketWriter.Event

    var writer: Option[WebSocketWriter[T]] = None

    val queue = collection.mutable.Queue[Event[T]]()

    override def connect( socket: OkHttpWebSocket ): this.type = synchronized {
        this.writer = Some( new OkHttpWebSocketWriter[T]( socket ) )

        while ( queue.nonEmpty ) {
            queue.dequeue() match {
                case Event.Send( value )         ⇒ send( value )
                case Event.Ping( value )         ⇒ ping( value )
                case Event.Close( code, reason ) ⇒ close( code, reason )
            }
        }

        queue.clear()
        this
    }

    override def disconnect(): this.type = synchronized {
        this.writer = None
        this
    }

    override def sendNow( value: T ) = synchronized {
        writer.fold[Unit] {
            logger.debug {
                s"""
                   |Dropping message (because socket is unavailable)
                   |  Payload: $value
                """.stripMargin.trim
            }
        } { _ ⇒ send( value ) }
    }

    override def send( value: T ) = synchronized {
        writer.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering message
                   |  Payload: $value
                """.stripMargin.trim
            }

            queue enqueue Event.Send( value )
        } { _.send( value ) }
    }

    override def ping( value: Option[T] ) = synchronized {
        writer.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering ping
                   |  Payload: $value
                """.stripMargin.trim
            }

            queue enqueue Event.Ping( value )
        } { _.ping( value ) }
    }

    override def close( code: Int, reason: Option[String] ) = {
        writer.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering close
                   |  Code:   $code
                   |  Reason: $reason
                """.stripMargin.trim
            }

            queue enqueue Event.Close( code, reason )
        } { _.close( code, reason ) }

    }
}

private object BufferedOkHttpWebSocketWriter {
    sealed trait Event[+T]

    object Event {
        case class Send[T]( value: T ) extends Event[T]
        case class Ping[T]( value: Option[T] ) extends Event[T]
        case class Close( code: Int, reason: Option[String] ) extends Event[Nothing]
    }
}