package io.taig.communicator

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

package object experimental
{
	implicit private[communicator] def `Function0 -> Unit -> Runnable`( f: => Unit ): Runnable = new Runnable
	{
		override def run() = f
	}

	implicit class RichBuilder( builder: okhttp.Request.Builder )
	{
		def start[T]()( implicit client: OkHttpClient, parser: io.taig.communicator.experimental.Parser[T], executor: ExecutionContext ) =
		{
			Request[T]( builder.build() )
		}
	}
}