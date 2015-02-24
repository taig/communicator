package io.taig

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.request.{Handler, Parser, Plain}

import scala.concurrent.{ExecutionContext => Context, Future}
import scala.language.implicitConversions

package object communicator
{
	type CanceledIOException = exception.io.Canceled

	private[communicator] implicit def `Function0 -> Runnable`( f: => Unit ): Runnable = new Runnable
	{
		override def run() = f
	}

	implicit class RichFuture[S]( future: Future[S] )
	{
		def handle( f: S => Handler )( implicit executor: Context ) = future.flatMap( f ).asInstanceOf[Handler]

		def parse[T]( f: S => Parser[T] )( implicit executor: Context ) = future.flatMap( f ).asInstanceOf[Parser[T]]

		def plain( f: S => Plain )( implicit executor: Context ) = future.flatMap( f ).asInstanceOf[Plain]
	}

	implicit class RichOkHttpRequest( request: okhttp.Request )
	{
		def plain( client: OkHttpClient )( implicit executor: Context ): Plain =
		{
			new Plain( client, request, executor )
		}

		def plain()( implicit executor: Context, client: OkHttpClient ): Plain =
		{
			plain( client )
		}

		def handle( client: OkHttpClient, handler: result.Handler )( implicit executor: Context ): Handler =
		{
			new Handler( client, request, handler, executor )
		}

		def handle( client: OkHttpClient )( implicit executor: Context, handler: result.Handler ): Handler =
		{
			handle( client, handler )
		}

		def handle( handler: result.Handler )( implicit client: OkHttpClient, executor: Context ): Handler =
		{
			handle( client, handler )
		}

		def handle()( implicit handler: result.Handler, executor: Context, client: OkHttpClient ): Handler =
		{
			handle( client, handler )
		}

		def parse[T]( client: OkHttpClient, parser: result.Parser[T] )( implicit executor: Context ): Parser[T] =
		{
			new Parser[T]( client, request, parser, executor )
		}

		def parse[T]( client: OkHttpClient )( implicit executor: Context, parser: result.Parser[T] ): Parser[T] =
		{
			parse( client, parser )
		}

		def parse[T]( parser: result.Parser[T] )( implicit client: OkHttpClient, executor: Context ): Parser[T] =
		{
			parse( client, parser )
		}

		def parse[T]()( implicit executor: Context, parser: result.Parser[T], client: OkHttpClient ): Parser[T] =
		{
			parse( client, parser )
		}
	}

	implicit class	RichOkHttpRequestBuilder( request: okhttp.Request.Builder )
	extends			RichOkHttpRequest( request.build() )
}