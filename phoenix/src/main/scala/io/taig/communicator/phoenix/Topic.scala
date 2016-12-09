package io.taig.communicator.phoenix

import cats.implicits._
import io.circe.{ Decoder, Encoder }

case class Topic( name: String, identifier: Option[String] ) {
    def isSubscribedTo( topic: Topic ): Boolean = topic match {
        case Topic( `name`, `identifier` ) ⇒ true
        case Topic( `name`, None )         ⇒ true
        case _                             ⇒ false
    }

    def serialize = name + identifier.map( ":" + _ ).getOrElse( "" )

    override def toString = s"Topic($serialize)"
}

object Topic {
    implicit val encoderTopic: Encoder[Topic] = {
        Encoder[String].contramap( _.serialize )
    }

    implicit val decoderTopic: Decoder[Topic] = {
        Decoder[String].emap { topic ⇒
            Either.fromOption( parse( topic ), "Invalid format" )
        }
    }

    val Phoenix = Topic( "phoenix" )

    val Pattern = "(\\w+)(?::(\\w+))?".r

    def apply( name: String, identifier: String ): Topic = {
        Topic( name, Some( identifier ) )
    }

    def apply( name: String ): Topic = Topic( name, None )

    def parse( topic: String ): Option[Topic] = {
        topic match {
            case Pattern( name, identifier ) ⇒
                Some( Topic( name, Option( identifier ) ) )
            case _ ⇒ None
        }
    }
}