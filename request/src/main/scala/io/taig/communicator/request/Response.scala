package io.taig.communicator.request

import okhttp3._

import scala.collection.JavaConverters._

sealed trait Response {
    def wrapped: okhttp3.Response

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

    lazy val cacheResponse: Option[Response] = {
        Option( wrapped.cacheResponse() ).map( Response( _ ) )
    }

    lazy val networkResponse: Option[Response] = {
        Option( wrapped.networkResponse() ).map( Response( _ ) )
    }

    lazy val priorResponse: Option[Response] = {
        Option( wrapped.priorResponse() ).map( Response( _ ) )
    }

    def withBody[T]( content: T ): Response.With[T] = new Response.With[T] {
        override def wrapped = Response.this.wrapped

        override def body = content
    }

    override def toString = {
        s"""
          |>>> ${request.url}
          |${if ( request.headers().size() == 0 ) "[No headers]" else request.headers()}
          |<<< $code${message.map( " " + _ ).getOrElse( "" )}
          |${if ( headers.size() == 0 ) "[No headers]" else headers}
        """.stripMargin.trim
    }
}

object Response {
    sealed trait With[+T] extends Response {
        def body: T
    }

    object With {
        def unapply[T]( response: Response.With[T] ): Option[( Int, T )] =
            Some( response.code, response.body )
    }

    def apply( response: okhttp3.Response ): Response =
        new Response { override def wrapped = response }

    def unapply( response: Response ): Option[Int] = Some( response.code )
}