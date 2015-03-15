package io.taig

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import _root_.io.taig.communicator.internal._

import scala.concurrent.{ExecutionContext => Context, Future}
import scala.language.{existentials, implicitConversions}
import scala.util.{Failure, Success, Try}

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

	trait RichConstructor
	{
		protected type R = request.Request[_, _, _]

		@inline
		implicit protected def build[R]( f: okhttp.Request => R )( implicit executor: Context ): R

		def plain( client: OkHttpClient )( implicit executor: Context ): request.Plain =
		{
			Request.plain( client, _: okhttp.Request )
		}

		def plain()( implicit executor: Context, client: OkHttpClient ): request.Plain =
		{
			Request.plain( _: okhttp.Request )
		}

		def handle( client: OkHttpClient, handler: result.Handler )( implicit executor: Context ): request.Handler =
		{
			Request.handle( client, _: okhttp.Request, handler )
		}

		def handle( client: OkHttpClient )( implicit executor: Context, handler: result.Handler ): request.Handler =
		{
			Request.handle( client, _: okhttp.Request )
		}

		def handle( handler: result.Handler )( implicit client: OkHttpClient, executor: Context ): request.Handler =
		{
			Request.handle( _: okhttp.Request, handler )
		}

		def handle()( implicit handler: result.Handler, executor: Context, client: OkHttpClient ): request.Handler =
		{
			Request.handle( _: okhttp.Request )
		}

		def parse[T]( client: OkHttpClient, parser: result.Parser[T] )( implicit executor: Context ): request.Parser[T] =
		{
			Request.parse( client, _: okhttp.Request, parser )
		}

		def parse[T]( client: OkHttpClient )( implicit executor: Context, parser: result.Parser[T] ): request.Parser[T] =
		{
			Request.parse( client, _: okhttp.Request )
		}

		def parse[T]( parser: result.Parser[T] )( implicit client: OkHttpClient, executor: Context ): request.Parser[T] =
		{
			Request.parse( _: okhttp.Request, parser )
		}

		def parse[T]()( implicit executor: Context, parser: result.Parser[T], client: OkHttpClient ): request.Parser[T] =
		{
			Request.parse( _: okhttp.Request )
		}
	}

	implicit class	RichOkHttpRequest( request: okhttp.Request )
	extends			RichConstructor
	{
		@inline
		override implicit protected def build[R]( f: ( okhttp.Request ) => R )( implicit executor: Context ): R =
		{
			f( request )
		}
	}

	implicit class	RichOkHttpRequestBuilder( builder: okhttp.Request.Builder )( implicit executor: Context )
	extends			RichConstructor
	{
		@inline
		override implicit protected def build[R]( f: ( okhttp.Request ) => R )( implicit executor: Context ): R =
		{
			f( builder.build() )
		}
	}
}