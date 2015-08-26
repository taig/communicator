package io.taig.communicator

import java.io.IOException
import java.net.URL

import com.squareup.okhttp
import com.squareup.okhttp._

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.util.Try

trait Request[T]
        extends Future[Response with Response.Payload[T]] {
    implicit def parser: Parser[T]

    def wrapped: Future[Response with Response.Payload[T]]

    def call: Call

    def interceptor: Interceptor

    def isCanceled = call.isCanceled

    def cancel() = call.cancel()

    def onSend[U]( f: Progress.Send ⇒ U )( implicit executor: ExecutionContext ): this.type = {
        interceptor.onSend( ( progress: Progress.Send ) ⇒ executor.execute( f( progress ): Unit ) )
        this
    }

    def onReceive[U]( f: Progress.Receive ⇒ U )( implicit executor: ExecutionContext ): this.type = {
        interceptor.onReceive( ( progress: Progress.Receive ) ⇒ executor.execute( f( progress ): Unit ) )
        this
    }

    override def onComplete[U]( f: Try[Response with Response.Payload[T]] ⇒ U )( implicit executor: ExecutionContext ): Unit = {
        wrapped.onComplete( f )
    }

    override def isCompleted = wrapped.isCompleted

    override def value = wrapped.value

    @throws[Exception]
    override def result( atMost: Duration )( implicit permit: CanAwait ) = wrapped.result( atMost )

    @throws[InterruptedException]
    @throws[TimeoutException]
    override def ready( atMost: Duration )( implicit permit: CanAwait ) = {
        wrapped.ready( atMost )
        this
    }
}

object Request {
    private[communicator] case class Impl[T](
        request:  okhttp.Request,
        client:   OkHttpClient,
        executor: ExecutionContext
    )( implicit val parser: Parser[T] )
            extends Request[T] {
        override val interceptor = new Interceptor( request )

        override val call = {
            val client = this.client.clone()
            client.networkInterceptors().add( interceptor )
            client.newCall( request )
        }

        override val wrapped = Future {
            try {
                val response = call.execute()
                val content = parser.parse( new Response( response ), response.body().byteStream() )
                new Response.Payload( response, content )
            }
            catch {
                case error: IOException if call.isCanceled ⇒ throw new exception.io.Canceled( error )
            }
        }( executor )
    }

    def prepare(): okhttp.Request.Builder = new okhttp.Request.Builder()

    def prepare( url: String ): okhttp.Request.Builder = prepare().url( url )

    def prepare( url: URL ): okhttp.Request.Builder = prepare().url( url )

    def apply[T: Parser]( request: okhttp.Request )( implicit client: OkHttpClient, executor: ExecutionContext ): Request[T] = {
        new Impl( request, client, executor )
    }
}