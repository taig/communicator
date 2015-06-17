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

	sealed trait RichStart
	{
		protected def request: okhttp.Request

		def start()( implicit client: OkHttpClient, executor: ExecutionContext ): Request = Request( request )
	}

	implicit class	RichBuilder( builder: okhttp.Request.Builder )
	extends			RichStart
	{
		override protected val request = builder.build()
	}

	implicit class	RichRequest( protected val request: okhttp.Request )
	extends			RichStart
}