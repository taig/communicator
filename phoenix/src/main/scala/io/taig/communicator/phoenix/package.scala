package io.taig.communicator

import com.typesafe.scalalogging.Logger
import io.circe.Json
import io.circe.parser._
import io.taig.communicator.websocket.Codec
import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT
import org.slf4j.LoggerFactory

import scala.util.{ Failure, Success }

package object phoenix {
    implicit val codecJson: Codec[Json] = Codec.instance(
        json ⇒ RequestBody.create( TEXT, json.noSpaces ),
        data ⇒ parse( new String( data ) ).fold( Failure( _ ), Success( _ ) )
    )

    private[phoenix] val logger = {
        Logger( LoggerFactory.getLogger( "phoenix" ) )
    }
}