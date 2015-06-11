package io.taig

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

package object communicator
{
	implicit private[communicator] def `Function0 -> Unit -> Runnable`( f: => Unit ): Runnable = new Runnable
	{
		override def run() = f
	}

	trait RichStart
	{
		protected def request: okhttp.Request

		/**
		 * Run a Request, ignoring the Response
		 */
		def run()( implicit client: OkHttpClient, executor: ExecutionContext ): Request[Nothing] = start[Nothing]()

		/**
		 * Run a Request, parse Response as String
		 */
		def start()( implicit client: OkHttpClient, executor: ExecutionContext ): Request[String] = start[String]()

		/**
		 * Run a Request, use Parser[T] to handle the Response
		 */
		def start[T]()( implicit client: OkHttpClient, parser: Parser[T], executor: ExecutionContext ): Request[T] =
		{
			Request[T]( request )
		}
	}

	implicit class	RichBuilder( builder: okhttp.Request.Builder )
	extends			RichStart
	{
		override protected val request = builder.build()
	}

	implicit class	RichRequest( protected val request: okhttp.Request )
	extends			RichStart
}