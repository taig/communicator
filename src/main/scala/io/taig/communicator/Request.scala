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

    def builder( url: HttpUrl ): Request.Builder = builder.url( url )

    def apply[T: Parser]( request: Request )( implicit ohc: OkHttpClient ): Task[Response[T]] = {
        Task.create { ( _, taskCallback ) ⇒
            val call = ohc.newCall( request )

            val requestCallback = new Callback {
                override def onResponse( call: Call, response: okhttp3.Response ) = {
                    try {
                        val headers = ResponseHeaders( response )
                        val stream = response.body().byteStream()
                        val content = Parser[T].parse( headers, stream )
                        taskCallback.onSuccess( headers.withBody( content ) )
                    } catch {
                        case exception: Throwable ⇒ taskCallback.onError( exception )
                    }
                }

                override def onFailure( call: Call, exception: IOException ) = {
                    taskCallback.onError( exception )
                }
            }

            call.enqueue( requestCallback )

            Cancelable { () ⇒
                requestCallback.onFailure( call, new IOException( "Canceled" ) )
                call.cancel()
            }
        }
    }

    def empty( request: Request )( implicit ohc: OkHttpClient ): Task[ResponseHeaders] = {
        Request[Unit]( request )
    }
}