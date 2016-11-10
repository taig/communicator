package io.taig.communicator.phoenix

import io.circe.{ Decoder, Encoder }

case class Topic( name: String, identifier: Option[String] ) {
    def isSubscribedTo( topic: Topic ): Boolean = topic match {
        case Topic( `name`, `identifier` ) ⇒ true
        case Topic( `name`, None )         ⇒ true
        case _                             ⇒ false
    }

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
                case Array( name ) ⇒ Right( Topic( name ) )
                case Array( name, identifier ) ⇒
                    Right( Topic( name, identifier ) )
                case _ ⇒ Left( s"Invalid topic format '$topic'" )
            }
        }
    }
}