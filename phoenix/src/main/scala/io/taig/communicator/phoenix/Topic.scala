package io.taig.communicator.phoenix

import io.circe.{ Decoder, Encoder }
import cats.syntax.xor._

case class Topic( name: String, identifier: String )

object Topic {
    implicit val encoderTopic: Encoder[Topic] = {
        Encoder[String].contramap {
            case Topic( name, identifier ) ⇒ s"$name:$identifier"
        }
    }

    implicit val decoderTopic: Decoder[Topic] = {
        Decoder[String].emap { topic ⇒
            topic.split( ":" ) match {
                case Array( name, identifier ) ⇒ Topic( name, identifier ).right
                case _                         ⇒ s"Invalid topic format '$topic'".left
            }
        }
    }
}