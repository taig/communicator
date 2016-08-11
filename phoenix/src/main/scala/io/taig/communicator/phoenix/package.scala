package io.taig.communicator

import io.circe.Json
import io.circe.parser._
import io.taig.communicator.websocket.Codec
import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT

import scala.util.{ Failure, Success }

package object phoenix {
    implicit val codecJson: Codec[Json] = Codec.instance(
        json ⇒ RequestBody.create( TEXT, json.noSpaces ),
        data ⇒ parse( new String( data ) ).fold( Failure( _ ), Success( _ ) )
    )
}