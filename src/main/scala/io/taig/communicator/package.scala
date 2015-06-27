package io.taig

import com.squareup.okhttp

import scala.language.implicitConversions

package object communicator
{
	implicit private[communicator] def `Function0 -> Unit -> Runnable`( f: => Unit ): Runnable = new Runnable
	{
		override def run() = f
	}

	implicit class	RichBuilder( builder: okhttp.Request.Builder )
	extends			ops.Request
	{
		override val request = builder.build()
	}

	implicit class	RichRequest( val request: okhttp.Request )
	extends			ops.Request
}