package io.taig

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient

import scala.concurrent.{ExecutionContext => Context, Future}
import scala.language.implicitConversions

package object communicator
{
	type CanceledIOException = exception.io.Canceled

	type Handler = result.Handler

	type Parser[T] = result.Parser[T]

	val Progress = new
	{
		type Send = event.Progress.Send
		def Send = event.Progress.Send

		type Receive = event.Progress.Receive
		def Receive = event.Progress.Receive
	}

	type Request[R <: Response.Plain, E <: event.Event, I <: interceptor.Interceptor[R, E]] = request.Request[R, E, I]
	def Request = request.Request

	val Response = new
	{
		type Handled = response.Handled

		type Parsed[T] = response.Parsed[T]

		type Plain = response.Plain
	}

	implicit class RichFuture[S]( future: Future[S] )
	{
		def handle( f: S => communicator.request.Handler )( implicit executor: Context ) =
		{
			future.flatMap( f ).asInstanceOf[communicator.request.Handler]
		}

		def parse[T]( f: S => communicator.request.Parser[T] )( implicit executor: Context ) =
		{
			future.flatMap( f ).asInstanceOf[communicator.request.Parser[T]]
		}

		def plain( f: S => communicator.request.Plain )( implicit executor: Context ) =
		{
			future.flatMap( f ).asInstanceOf[communicator.request.Plain]
		}
	}

	implicit class RichOkHttpRequest( request: okhttp.Request )
	{
		def plain( client: OkHttpClient )( implicit executor: Context ): communicator.request.Plain =
		{
			new communicator.request.Plain( client, request, executor )
		}

		def plain()( implicit executor: Context, client: OkHttpClient ): communicator.request.Plain =
		{
			plain( client )
		}

		def handle( client: OkHttpClient, handler: result.Handler )( implicit executor: Context ): communicator.request.Handler =
		{
			new communicator.request.Handler( client, request, handler, executor )
		}

		def handle( client: OkHttpClient )( implicit executor: Context, handler: result.Handler ): communicator.request.Handler =
		{
			handle( client, handler )
		}

		def handle( handler: result.Handler )( implicit client: OkHttpClient, executor: Context ): communicator.request.Handler =
		{
			handle( client, handler )
		}

		def handle()( implicit handler: result.Handler, executor: Context, client: OkHttpClient ): communicator.request.Handler =
		{
			handle( client, handler )
		}

		def parse[T]( client: OkHttpClient, parser: result.Parser[T] )( implicit executor: Context ): communicator.request.Parser[T] =
		{
			new communicator.request.Parser[T]( client, request, parser, executor )
		}

		def parse[T]( client: OkHttpClient )( implicit executor: Context, parser: result.Parser[T] ): communicator.request.Parser[T] =
		{
			parse( client, parser )
		}

		def parse[T]( parser: result.Parser[T] )( implicit client: OkHttpClient, executor: Context ): communicator.request.Parser[T] =
		{
			parse( client, parser )
		}

		def parse[T]()( implicit executor: Context, parser: result.Parser[T], client: OkHttpClient ): communicator.request.Parser[T] =
		{
			parse( client, parser )
		}
	}

	implicit class	RichOkHttpRequestBuilder( request: okhttp.Request.Builder )
	extends			RichOkHttpRequest( request.build() )
}