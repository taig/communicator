package io.taig.communicator

import okhttp3.{ CacheControl, Challenge, Handshake, Headers, Protocol }

import scala.collection.JavaConversions._

sealed trait ResponseHeaders {
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
    def challenges: List[Challenge] = wrapped.challenges.toList

    @inline
    def cacheControl: CacheControl = wrapped.cacheControl

    @inline
    def sentRequestAtMillis: Long = wrapped.sentRequestAtMillis()

    @inline
    def receivedResponseAtMillis: Long = wrapped.receivedResponseAtMillis()

    lazy val cacheResponse: Option[ResponseHeaders] = {
        Option( wrapped.cacheResponse() ).map( ResponseHeaders( _ ) )
    }

    lazy val networkResponse: Option[ResponseHeaders] = {
        Option( wrapped.networkResponse() ).map( ResponseHeaders( _ ) )
    }

    lazy val priorResponse: Option[ResponseHeaders] = {
        Option( wrapped.priorResponse() ).map( ResponseHeaders( _ ) )
    }

    private[communicator] def withBody[T]( content: T ): Response[T] = new Response[T] {
        override def wrapped = ResponseHeaders.this.wrapped

        override def body = content
    }

    override def toString = {
        s">>> ${request.url.toString}\n${request.headers()}\n\n\n" +
            s"<<< $code $message\n${headers.newBuilder().removeAll( "Status" ).build()}"
    }
}

object ResponseHeaders {
    private[communicator] def apply( response: okhttp3.Response ): ResponseHeaders = new ResponseHeaders {
        override def wrapped = response
    }
}

sealed trait Response[+T] extends ResponseHeaders {
    def body: T
}