package io.taig.communicator.websocket

import io.taig.communicator.websocket.BufferedOkHttpWebSocket.Event
import okhttp3.RequestBody
import okio.Buffer

private class BufferedOkHttpWebSocket( var socket: Option[OkHttpWebSocket] = None )
        extends OkHttpWebSocket {
    private val buffer = collection.mutable.ListBuffer[Event]()

    private[websocket] def inject( socket: OkHttpWebSocket ): Unit = {
        this.socket = Some( socket )

        buffer.foreach {
            case Event.Send( message )       ⇒ sendMessage( message )
            case Event.Ping( payload )       ⇒ sendPing( payload )
            case Event.Close( code, reason ) ⇒ close( code, reason )
        }

        buffer.clear()
    }

    private[websocket] def disable(): Unit = {
        socket = None
    }

    override def sendMessage( message: RequestBody ) = {
        socket.fold[Unit]( buffer += Event.Send( message ) ) {
            _.sendMessage( message )
        }
    }

    override def sendPing( payload: Buffer ) = {
        socket.fold[Unit]( buffer += Event.Ping( payload ) ) { socket ⇒
            socket.sendPing( payload )
        }
    }

    def close( code: Int, reason: String ): Unit = {
        socket.fold[Unit]( buffer += Event.Close( code, reason ) ) {
            _.close( code, reason )
        }
    }
}

private object BufferedOkHttpWebSocket {
    sealed trait Event

    object Event {
        case class Send( message: RequestBody ) extends Event
        case class Ping( payload: Buffer ) extends Event
        case class Close( code: Int, reason: String ) extends Event
    }
}