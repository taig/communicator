package io.taig.communicator.websocket

import io.taig.communicator.OkHttpRequest
import monix.eval.Task
import okhttp3.OkHttpClient
import okio.Buffer

trait WebSocketWriter[T] {
    def send( value: T ): Unit

    def ping( value: Option[T] = None ): Unit

    def close( code: Int, reason: Option[String] ): Unit
}

object WebSocketWriter {
    def apply[T: Codec]( request: OkHttpRequest )(
        implicit
        client: OkHttpClient
    ): Task[WebSocketWriter[T]] = {
        WebSocket( request )( new WebSocketListenerNoop[T] ).map {
            case ( socket, _ ) ⇒ new OkHttpWebSocketWriter[T]( socket )
        }
    }
}

private class OkHttpWebSocketWriter[T: Codec]( socket: OkHttpWebSocket )
        extends WebSocketWriter[T] {
    override def send( value: T ) = {
        socket.sendMessage( Codec[T].encode( value ) )
    }

    override def ping( value: Option[T] ) = {
        val sink = value.map { value ⇒
            val sink = new Buffer
            val request = Codec[T].encode( value )

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
        socket.close( code, reason.orNull )
    }
}

trait BufferedWebSocketWriter[T] extends WebSocketWriter[T] {
    def connect( socket: OkHttpWebSocket ): this.type

    def disconnect(): this.type
}

object BufferedWebSocketWriter {
    def apply[T: Codec](): BufferedWebSocketWriter[T] = {
        new BufferedOkHttpWebSocketWriter[T]
    }
}

private class BufferedOkHttpWebSocketWriter[T: Codec]
        extends BufferedWebSocketWriter[T] {
    import BufferedOkHttpWebSocketWriter.Event

    var socket: Option[OkHttpWebSocket] = None

    val queue = collection.mutable.Queue[Event[T]]()

    override def connect( socket: OkHttpWebSocket ): this.type = synchronized {
        this.socket = Some( socket )

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
        this.socket = None
        this
    }

    override def send( value: T ) = synchronized {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering message
                   |  Paylaod: $value
                """.stripMargin.trim
            }

            queue enqueue Event.Send( value )
        } { socket ⇒
            logger.debug {
                s"""
                   |Sending message
                   |  Payload: $value
                """.stripMargin.trim
            }

            socket.sendMessage( Codec[T].encode( value ) )
        }
    }

    override def ping( value: Option[T] ) = synchronized {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering ping
                   |  Payload: $value
                """.stripMargin.trim
            }

            queue enqueue Event.Ping( value )
        } { socket ⇒
            logger.debug {
                s"""
                   |Sending ping
                   |  Payload: $value
                """.stripMargin.trim
            }

            val sink = value.map { value ⇒
                val sink = new Buffer
                val request = Codec[T].encode( value )

                try {
                    request.writeTo( sink )
                    sink
                } finally {
                    sink.close()
                }
            }

            socket.sendPing( sink.orNull )
        }
    }

    override def close( code: Int, reason: Option[String] ) = {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering close
                   |  Code:   $code
                   |  Reason: $reason
                """.stripMargin.trim
            }

            queue enqueue Event.Close( code, reason )
        } { socket ⇒
            logger.debug {
                s"""
                   |Sending close
                   |  Code:   $code
                   |  Reason: $reason
            """.stripMargin.trim
            }

            socket.close( code, reason.orNull )
        }

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