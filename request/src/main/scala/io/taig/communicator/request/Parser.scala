package io.taig.communicator.request

import io.taig.communicator.OkHttpResponse
import okhttp3.ResponseBody

/**
  * Type class that describes how to transform an InputStream to an instance of T
  *
  * The InputStream has to be closed after processing (but also when not using
  * it)!
  *
  * @tparam T
  */
trait Parser[T] {
  def parse(response: OkHttpResponse): T

  def map[U](f: T ⇒ U): Parser[U] = Parser.instance { response ⇒
    f(parse(response))
  }

  def mapResponse[U](f: (OkHttpResponse, T) ⇒ U): Parser[U] =
    Parser.instance { response ⇒
      f(response, parse(response))
    }
}

object Parser {
  def apply[T: Parser]: Parser[T] = implicitly[Parser[T]]

  def instance[T](f: OkHttpResponse ⇒ T): Parser[T] =
    new Parser[T] {
      override def parse(response: OkHttpResponse) = f(response)
    }

  implicit val parserByteArray: Parser[Array[Byte]] = instance { response ⇒
    val body = response.body

    try {
      val stream = body.byteStream
      Iterator
        .continually(stream.read)
        .takeWhile(_ != -1)
        .map(_.toByte)
        .toArray
    } finally body.close()
  }

  implicit val parserResponseBody: Parser[ResponseBody] =
    instance(_.body)

  implicit val parserUnit: Parser[Unit] =
    instance(_.body.close())

  implicit val parserString: Parser[String] =
    instance(_.body.string())
}
