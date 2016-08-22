package io.taig.communicator.phoenix

import io.circe.{ Decoder, Encoder }
import cats.syntax.xor._

case class Topic( name: String, identifier: Option[String] ) {
    override def toString = {
        name + identifier.map( ":" + _ ).getOrElse( "" )
    }
}

object Topic {
    def apply( name: String, identifier: String ): Topic = {
        new Topic( name, Some( identifier ) )
    }

    def apply( name: String ): Topic = {
        new Topic( name, None )
    }

    implicit val encoderTopic: Encoder[Topic] = {
        Encoder[String].contramap( _.toString )
    }

    implicit val decoderTopic: Decoder[Topic] = {
        Decoder[String].emap { topic ⇒
            topic.split( ":" ) match {
                case Array( name ) ⇒ Topic( name ).right
                case Array( name, identifier ) ⇒
                    Topic( name, identifier ).right
                case _ ⇒
                    s"Invalid topic format '$topic'".left
            }
        }
    }
}