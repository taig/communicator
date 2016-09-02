package io.taig.communicator

import cats.syntax.contravariant._
import com.typesafe.scalalogging.Logger
import io.circe.Json
import io.circe.parser._
import io.taig.communicator.websocket._
import org.slf4j.LoggerFactory

package object phoenix {
    implicit val decoderJson: Decoder[Json] = new Decoder[Json] {
        override def decode( value: Array[Byte] ) = {
            parse( new String( value, "UTF-8" ) ).toTry
        }
    }

    implicit val encoderJson: Encoder[Json] = {
        Encoder[String].contramap( _.noSpaces )
    }

    private[phoenix] val logger = Logger( LoggerFactory.getLogger( "phoenix" ) )
}