package io.taig.communicator

import com.squareup.okhttp

class Response private[communicator] ( wrapped: okhttp.Response ) {
    def code = wrapped.code()

    def message = wrapped.message()

    def headers = wrapped.headers

    def request = wrapped.request

    def protocol = wrapped.protocol

    def handshake = wrapped.handshake

    def isSuccessful = wrapped.isSuccessful

    def isRedirect = wrapped.isRedirect

    def challenges = wrapped.challenges

    def cacheControl = wrapped.cacheControl

    def cacheResponse = wrapped.cacheResponse()

    def networkResponse = wrapped.networkResponse()

    def priorResponse = wrapped.priorResponse()

    def withPayload[T]( payload: T ) = new Response.Payload( wrapped, payload )

    override def toString = {
        s">>> ${request.urlString()}\n${request.headers()}\n\n\n" +
            s"<<< $code $message\n${headers.newBuilder().removeAll( "Status" ).build()}"
    }
}

object Response {
    def unapply[T]( response: Response with Payload[T] ) = Some( response.code, response.body )

    class Payload[+T] private[communicator] ( wrapped: okhttp.Response, val body: T ) extends Response( wrapped ) {
        override def toString = super.toString + "\n\n\n" + body
    }
}