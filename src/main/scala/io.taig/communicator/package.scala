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

	implicit class RichBuilder( builder: okhttp.Request.Builder )
	{
		def start[T]()( implicit client: OkHttpClient, parser: Parser[T], executor: ExecutionContext ) =
		{
			Request[T]( builder.build() )
		}
	}
}