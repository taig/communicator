package io.taig.communicator

import com.typesafe.scalalogging.Logger
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.taig.communicator.phoenix.message._
import io.taig.communicator.websocket._
import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT
import org.slf4j.LoggerFactory

import scala.util.{ Failure, Success }

package object phoenix {
    implicit val encoderRequest: Encoder[Outbound] = {
        Encoder.instance {
            case request: Request ⇒
                RequestBody.create( TEXT, request.asJson.noSpaces )
        }
    }

    implicit val decoderResponse: Decoder[Inbound] = {
        Decoder.instance { data ⇒
            val string = new String( data, "UTF-8" )
            decode[Response]( string ).orElse( decode[Push]( string ) )
                .fold( Failure( _ ), Success( _ ) )
        }
    }

    private[phoenix] val logger = Logger( LoggerFactory.getLogger( "phoenix" ) )
}