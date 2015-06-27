package io.taig.communicator

import java.io.IOException
import java.net.URL

import com.squareup.okhttp
import com.squareup.okhttp._

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

trait	Request
extends	Features[Response]
{
	def parse[T: Parser]()( implicit executor: ExecutionContext ): Request.Payload[T] = Request.Payload.Impl(
		map( _.withPayload( implicitly[Parser[T]] ) ),
		interceptor,
		call
	)
}

object Request
{
	trait	Payload[+T]
	extends	Request
	with	Features[Response.Payload[T]]

	private[communicator] case class Impl(
		request: okhttp.Request,
		client: OkHttpClient,
		executor: ExecutionContext
	)
	extends Request
	{
		override val interceptor = new Interceptor( request )

		override val call =
		{
			val client = this.client.clone()
			client.networkInterceptors().add( interceptor )
			client.newCall( request )
		}

		override val wrapped = Future
		{
			try
			{
				new Response( call.execute() )
			}
			catch
			{
				case error: IOException if call.isCanceled => throw new exception.io.Canceled( error )
			}
		}( executor )
	}

	object Payload
	{
		private[communicator] case class Impl[T: Parser](
			wrapped: Future[Response.Payload[T]],
			interceptor: Interceptor,
			call: Call
		)
		extends Payload[T]
	}

	def prepare(): okhttp.Request.Builder = new okhttp.Request.Builder()

	def prepare( url: String ): okhttp.Request.Builder = prepare().url( url )

	def prepare( url: URL ): okhttp.Request.Builder = prepare().url( url )

	def apply( request: okhttp.Request )( implicit client: OkHttpClient, executor: ExecutionContext ): Request =
	{
		new Impl( request, client, executor )
	}
}