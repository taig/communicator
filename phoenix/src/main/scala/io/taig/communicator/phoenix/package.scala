package io.taig.communicator

import com.typesafe.scalalogging.Logger
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.taig.communicator.phoenix.message.{ Request, Response }
import io.taig.communicator.websocket._
import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT
import org.slf4j.LoggerFactory

import scala.util.{ Failure, Success }

package object phoenix {
    implicit val encoderRequest: Encoder[Request] = {
        Encoder.instance { request ⇒
            RequestBody.create( TEXT, request.asJson.noSpaces )
        }
    }

    implicit val decoderResponse: Decoder[Response] = {
        Decoder.instance { data ⇒
            decode[Response]( new String( data ) )
                .fold( Failure( _ ), Success( _ ) )
        }
    }

    private[phoenix] val logger = {
        Logger( LoggerFactory.getLogger( "phoenix" ) )
    }
}