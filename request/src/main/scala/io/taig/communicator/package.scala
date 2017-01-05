package io.taig

import org.slf4j.LoggerFactory

import scala.language.implicitConversions

package object communicator {
    implicit private[communicator] def function0ToRunnable(
        f: () ⇒ Unit
    ): Runnable = new Runnable { override def run() = f() }

    private[communicator] val logger = LoggerFactory.getLogger( "request" )

    type OkHttpRequest = okhttp3.Request

    object OkHttpRequest {
        type Builder = okhttp3.Request.Builder
    }

    type OkHttpResponse = okhttp3.Response

    type OkHttpRequestBody = okhttp3.RequestBody

    type OkHttpMultipartBody = okhttp3.MultipartBody

    object OkHttpMultipartBody {
        type Builder = okhttp3.MultipartBody.Builder
    }

    type OkHttpPart = okhttp3.MultipartBody.Part

    type OkHttpWebSocket = okhttp3.WebSocket

    type OkHttpWebSocketListener = okhttp3.WebSocketListener

    type MediaType = okhttp3.MediaType

    object MediaType {
        val Jpeg = parse( "image/jpeg" )

        val Json = parse( "application/json" )

        val Png = parse( "image/png" )

        @inline
        def parse( value: String ): MediaType = okhttp3.MediaType.parse( value )
    }
}