package io.taig.communicator.request

import io.taig.communicator.OkHttpResponse
import okhttp3._

import scala.collection.JavaConverters._

sealed trait Response[T] {
    def wrapped: OkHttpResponse

    @inline
    def code: Int = wrapped.code()

    @inline
    def message: Option[String] = Option( wrapped.message() )

    @inline
    def headers: Headers = wrapped.headers

    @inline
    def request: okhttp3.Request = wrapped.request

    @inline
    def protocol: Protocol = wrapped.protocol

    @inline
    def handshake: Option[Handshake] = Option( wrapped.handshake )

    @inline
    def isSuccessful: Boolean = wrapped.isSuccessful

    @inline
    def isRedirect: Boolean = wrapped.isRedirect

    @inline
    def challenges: List[Challenge] =
        iterableAsScalaIterableConverter( wrapped.challenges ).asScala.toList

    @inline
    def cacheControl: CacheControl = wrapped.cacheControl

    @inline
    def sentRequestAtMillis: Long = wrapped.sentRequestAtMillis()

    @inline
    def receivedResponseAtMillis: Long = wrapped.receivedResponseAtMillis()

    @inline
    def body: T

    lazy val cacheResponse: Option[Response[T]] = {
        Option( wrapped.cacheResponse() ).map( Response( _, body ) )
    }

    lazy val networkResponse: Option[Response[T]] = {
        Option( wrapped.networkResponse() ).map( Response( _, body ) )
    }

    lazy val priorResponse: Option[Response[T]] = {
        Option( wrapped.priorResponse() ).map( Response( _, body ) )
    }

    override def toString: String =
        s"""
          |>>> ${request.url}
          |${if ( request.headers().size() == 0 ) "[No headers]" else request.headers()}
          |<<< $code${message.map( " " + _ ).getOrElse( "" )}
          |${if ( headers.size() == 0 ) "[No headers]" else headers}
        """.stripMargin.trim
}

object Response {
    def apply[T]( response: OkHttpResponse, _body: T ): Response[T] =
        new Response[T] {
            override def wrapped: OkHttpResponse = response

            override def body: T = _body
        }

    def untouched( response: OkHttpResponse ): Response[Unit] =
        Response[Unit]( response, () )

    def unapply[T]( response: Response[T] ): Option[( Int, T )] =
        Some( response.code, response.body )
}