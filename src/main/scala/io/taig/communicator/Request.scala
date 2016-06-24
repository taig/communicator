package io.taig.communicator

import java.io.IOException
import java.net.URL

import monix.eval.Task
import monix.execution.Cancelable
import okhttp3._

object Request {
    type Builder = okhttp3.Request.Builder

    def builder: Request.Builder = new Request.Builder()

    def builder( url: String ): Request.Builder = builder.url( url )

    def builder( url: URL ): Request.Builder = builder.url( url )

    def apply[T: Parser]( request: Request )( implicit ohc: OkHttpClient ): Task[Response[T]] = {
        Task.create { ( _, callback ) ⇒
            val call = ohc.newCall( request )

            call.enqueue {
                new Callback {
                    override def onResponse( call: Call, response: okhttp3.Response ) = {
                        val headers = ResponseHeaders( response )
                        val content = Parser[T].parse( headers, response.body().byteStream() )
                        callback.onSuccess( headers.withBody( content ) )
                    }

                    override def onFailure( call: Call, exception: IOException ) = {
                        callback.onError( exception )
                    }
                }
            }

            Cancelable( () ⇒ call.cancel() )
        }
    }

    def empty( request: Request )( implicit ohc: OkHttpClient ): Task[ResponseHeaders] = {
        Request[Unit]( request )
    }
}