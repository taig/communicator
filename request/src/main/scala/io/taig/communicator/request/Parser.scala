package io.taig.communicator.request

import okhttp3.ResponseBody

/**
 * Type class that describes how to transform an InputStream to an instance of T
 *
 * The InputStream has to be closed after processing (but also when not using
 * it)!
 *
 * @tparam T
 */
trait Parser[+T] {
    def parse( response: Response, body: ResponseBody ): T

    def map[U]( f: ( Response, T ) ⇒ U ): Parser[U] = {
        Parser.instance { ( response, body ) ⇒
            f( response, parse( response, body ) )
        }
    }
}

object Parser {
    def apply[T: Parser]: Parser[T] = implicitly[Parser[T]]

    def instance[T]( f: ( Response, ResponseBody ) ⇒ T ): Parser[T] = {
        new Parser[T] {
            override def parse( response: Response, body: ResponseBody ) =
                f( response, body )
        }
    }

    implicit val parserByteArray: Parser[Array[Byte]] = instance { ( _, body ) ⇒
        try {
            val stream = body.byteStream
            Iterator
                .continually( stream.read )
                .takeWhile( _ != -1 )
                .map( _.toByte )
                .toArray
        } finally {
            body.close()
        }
    }

    implicit val parserResponseBody: Parser[ResponseBody] =
        instance( ( _, body ) ⇒ body )

    implicit val parserUnit: Parser[Unit] =
        instance( ( _, body ) ⇒ body.close() )

    implicit val parserString: Parser[String] =
        instance( ( _, body ) ⇒ body.string )
}