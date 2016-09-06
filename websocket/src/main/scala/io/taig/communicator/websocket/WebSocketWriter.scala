package io.taig.communicator.websocket

import cats.functor.Contravariant
import cats.syntax.contravariant._
import okhttp3.RequestBody
import okio.Buffer

trait WebSocketWriter[T] extends WebSocket[T] {
    def connect( socket: WebSocket[T] ): this.type

    def disconnect(): this.type

    def isConnected: Boolean

    def sendNow( value: T ): Unit
}

object WebSocketWriter {
    /**
     * Create a simple `WebSocketWriter` instance that has to be connected to
     * a socket by the called
     */
    def apply[T: Encoder]: WebSocketWriter[T] = new BufferedSocketWriter[T]

    implicit val contravariant: Contravariant[WebSocketWriter] = {
        new Contravariant[WebSocketWriter] {
            override def contramap[A, B]( fa: WebSocketWriter[A] )( f: B ⇒ A ) = {
                new WebSocketWriter[B] {
                    override val raw = fa.raw

                    override val encoder = fa.encoder.contramap( f )

                    override def connect( socket: WebSocket[B] ) = {
                        fa.connect( WebSocket[A]( raw )( fa.encoder ) )
                        this
                    }

                    override def disconnect() = {
                        fa.disconnect()
                        this
                    }

                    override def isConnected = fa.isConnected

                    override def sendNow( value: B ) = {
                        fa.sendNow( f( value ) )
                    }

                    override def send( value: B ) = {
                        fa.send( f( value ) )
                    }

                    override def close( code: Int, reason: Option[String] ) = {
                        fa.close( code, reason )
                    }

                    override def ping( value: Option[B] ) = {
                        fa.ping( value.map( f ) )
                    }

                    override def isClosed = fa.isClosed
                }
            }
        }
    }
}

private class BufferedSocketWriter[T: Encoder] extends WebSocketWriter[T] {
    import BufferedSocketWriter.Event

    override val encoder = Encoder[T]

    var socket: Option[WebSocket[T]] = None

    val queue = collection.mutable.Queue[Event]()

    override private[websocket] def raw = socket.map( _.raw ).orNull

    override def connect( socket: WebSocket[T] ): this.type = synchronized {
        logger.debug( s"Connecting and flushing (${queue.size})" )

        this.socket = Some( socket )

        while ( queue.nonEmpty ) {
            queue.dequeue() match {
                case Event.Send( value, encoded ) ⇒
                    logger.debug {
                        s"""
                           |Flushing message
                           |  Payload: $value
                        """.stripMargin.trim
                    }

                    socket.raw.sendMessage( encoded )
                case Event.Ping( value, encoded ) ⇒
                    logger.debug {
                        s"""
                           |Flushing ping
                           |  Payload: $value
                        """.stripMargin.trim
                    }

                    socket.raw.sendPing( encoded )
                case Event.Close( code, reason ) ⇒
                    logger.debug {
                        s"""
                           |Flushing close
                           |  Code: $code
                           |  Reason: $reason
                        """.stripMargin.trim
                    }

                    socket.raw.close( code, reason )
            }
        }

        this
    }

    override def disconnect(): this.type = synchronized {
        logger.debug( "Disconnecting Writer" )

        this.socket = None
        this
    }

    override def isConnected = synchronized( socket.isDefined )

    override def send( value: T ): Unit = synchronized {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering message
                   |  Payload: $value
                """.stripMargin.trim
            }

            queue enqueue Event.Send( value, encoder.encode( value ) )
        } { _.send( value ) }
    }

    override def sendNow( value: T ): Unit = synchronized {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Dropping message (because socket is unavailable)
                   |  Payload: $value
                """.stripMargin.trim
            }
        } { _ ⇒ send( value ) }
    }

    override def ping( value: Option[T] ) = synchronized {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering ping
                   |  Payload: $value
                """.stripMargin.trim
            }

            queue enqueue Event.Ping( value, value.map( encoder.buffer ).orNull )
        } { _.ping( value ) }
    }

    override def close( code: Int, reason: Option[String] ) = synchronized {
        socket.fold[Unit] {
            logger.debug {
                s"""
                   |Buffering close
                   |  Code:   $code
                   |  Reason: $reason
                """.stripMargin.trim
            }

            queue enqueue Event.Close( code, reason.getOrElse( "" ) )
        } { _.close( code, reason ) }
    }

    override def isClosed = synchronized { socket.forall( _.isClosed ) }
}

private object BufferedSocketWriter {
    sealed trait Event

    object Event {
        case class Send[T]( value: T, encoded: RequestBody ) extends Event
        case class Ping[T]( value: Option[T], encoded: Buffer ) extends Event
        case class Close( code: Int, reason: String ) extends Event
    }
}