package io.taig.communicator

import java.io.InputStream
import java.nio.charset.Charset

import okhttp3.MediaType

import scala.io.Source

/**
 * Type class that describes how to transform an InputStream to an instance of T
 *
 * The InputStream has to be closed after processing (but also when not using it)!
 *
 * @tparam T
 */
trait Parser[+T] {
    def parse( response: Response, stream: InputStream ): T

    def map[U]( f: T ⇒ U ): Parser[U] = Parser.instance { ( response, stream ) ⇒
        f( parse( response, stream ) )
    }
}

object Parser {
    def apply[T: Parser]: Parser[T] = implicitly[Parser[T]]

    def instance[T]( f: ( Response, InputStream ) ⇒ T ): Parser[T] = new Parser[T] {
        override def parse( response: Response, stream: InputStream ) = f( response, stream )
    }

    implicit val parserInputStream: Parser[InputStream] = instance( ( _, stream ) ⇒ stream )

    implicit val parserUnit: Parser[Unit] = instance( ( _, stream ) ⇒ stream.close() )

    implicit val parserString: Parser[String] = instance { ( response, stream ) ⇒
        try {
            val charset = for {
                contentType ← Option( response.headers.get( "Content-Type" ) )
                mediaType ← Option( MediaType.parse( contentType ) )
                charset ← Option( mediaType.charset() )
            } yield charset

            Source.fromInputStream(
                stream,
                charset.getOrElse( Charset.forName( "UTF-8" ) ).displayName()
            ).mkString
        } finally {
            stream.close()
        }
    }
}