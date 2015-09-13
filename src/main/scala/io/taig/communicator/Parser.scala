package io.taig.communicator

import java.io.InputStream
import java.nio.charset.Charset

import com.squareup.okhttp.MediaType

import scala.io.Source

trait Parser[T] {
    def parse( response: Response, stream: InputStream ): T
}

object Parser {
    implicit val `Parser[Nothing]` = new Parser[Nothing] {
        override def parse( response: Response, stream: InputStream ) = null.asInstanceOf[Nothing]
    }

    implicit val `Parser[Unit]` = new Parser[Unit] {
        override def parse( response: Response, stream: InputStream ) = null.asInstanceOf[Unit]
    }

    implicit val `Parser[String]` = new Parser[String] {
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