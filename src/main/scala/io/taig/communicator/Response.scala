package io.taig.communicator

class Response private[communicator] ( val wrapped: okhttp3.Response ) {
    @inline
    def code = wrapped.code()

    @inline
    def message = wrapped.message()

    @inline
    def headers = wrapped.headers

    @inline
    def request = wrapped.request

    @inline
    def protocol = wrapped.protocol

    @inline
    def handshake = wrapped.handshake

    @inline
    def isSuccessful = wrapped.isSuccessful

    @inline
    def isRedirect = wrapped.isRedirect

    @inline
    def challenges = wrapped.challenges

    @inline
    def cacheControl = wrapped.cacheControl

    lazy val cacheResponse: Option[Response] = Option( wrapped.cacheResponse() ).map( new Response( _ ) )

    lazy val networkResponse: Option[Response] = Option( wrapped.networkResponse() ).map( new Response( _ ) )

    lazy val priorResponse: Option[Response] = Option( wrapped.priorResponse() ).map( new Response( _ ) )

    private[communicator] def withPayload[T]( payload: T ) = new Response.Payload( wrapped, payload )

    override def toString = {
        s">>> ${request.url.toString}\n${request.headers()}\n\n\n" +
            s"<<< $code $message\n${headers.newBuilder().removeAll( "Status" ).build()}"
    }
}

object Response {
    def unapply[T]( response: Response with Payload[T] ) = Some( response.code, response.body )

    class Payload[+T] private[communicator] ( wrapped: okhttp3.Response, val body: T ) extends Response( wrapped ) {
        override def toString = super.toString + "\n\n\n" + body
    }
}