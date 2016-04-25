package io.taig.communicator

import java.io.InputStream
import java.nio.charset.Charset

import okhttp3.MediaType

import scala.io.Source

trait Parser[T] {
    def parse( response: Response, stream: InputStream ): T
}

object Parser {
    def apply[T: Parser]: Parser[T] = implicitly[Parser[T]]

    implicit val parserNothing = new Parser[Nothing] {
        override def parse( response: Response, stream: InputStream ) = null.asInstanceOf[Nothing]
    }

    implicit val parserUnit = new Parser[Unit] {
        override def parse( response: Response, stream: InputStream ) = null.asInstanceOf[Unit]
    }

    implicit val parserString = new Parser[String] {
        override def parse( response: Response, stream: InputStream ) = {
            val charset = Option( response.headers.get( "Content-Type" ) )
                .map( MediaType.parse )
                .map( _.charset() )
                .flatMap( Option.apply )
                .getOrElse( Charset.forName( "UTF-8" ) )

            Source.fromInputStream( stream, charset.displayName() ).mkString
        }
    }
}