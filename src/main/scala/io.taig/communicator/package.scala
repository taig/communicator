package io.taig

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.internal._

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
		def handle( f: S => request.Handler )( implicit executor: Context ) =
		{
			future.flatMap( f ).asInstanceOf[request.Handler]
		}

		def parse[T]( f: S => request.Parser[T] )( implicit executor: Context ) =
		{
			future.flatMap( f ).asInstanceOf[request.Parser[T]]
		}

		def plain( f: S => request.Plain )( implicit executor: Context ) =
		{
			future.flatMap( f ).asInstanceOf[request.Plain]
		}
	}

	implicit class RichOkHttpRequest( wrapped: okhttp.Request )
	{
		def plain( client: OkHttpClient )( implicit executor: Context ): request.Plain =
		{
			new request.Plain( client, wrapped, executor )
		}

		def plain()( implicit executor: Context, client: OkHttpClient ): request.Plain =
		{
			plain( client )
		}

		def handle( client: OkHttpClient, handler: result.Handler )( implicit executor: Context ): request.Handler =
		{
			new request.Handler( client, wrapped, handler, executor )
		}

		def handle( client: OkHttpClient )( implicit executor: Context, handler: result.Handler ): request.Handler =
		{
			handle( client, handler )
		}

		def handle( handler: result.Handler )( implicit client: OkHttpClient, executor: Context ): request.Handler =
		{
			handle( client, handler )
		}

		def handle()( implicit handler: result.Handler, executor: Context, client: OkHttpClient ): request.Handler =
		{
			handle( client, handler )
		}

		def parse[T]( client: OkHttpClient, parser: result.Parser[T] )( implicit executor: Context ): request.Parser[T] =
		{
			new request.Parser[T]( client, wrapped, parser, executor )
		}

		def parse[T]( client: OkHttpClient )( implicit executor: Context, parser: result.Parser[T] ): request.Parser[T] =
		{
			parse( client, parser )
		}

		def parse[T]( parser: result.Parser[T] )( implicit client: OkHttpClient, executor: Context ): request.Parser[T] =
		{
			parse( client, parser )
		}

		def parse[T]()( implicit executor: Context, parser: result.Parser[T], client: OkHttpClient ): request.Parser[T] =
		{
			parse( client, parser )
		}
	}

	implicit class	RichOkHttpRequestBuilder( request: okhttp.Request.Builder )
	extends			RichOkHttpRequest( request.build() )
}