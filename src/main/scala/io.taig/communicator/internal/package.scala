package io.taig.communicator

import scala.language.implicitConversions

package object internal
{
	implicit def `Function0 -> Unit -> Runnable`( f: => Unit ): Runnable = new Runnable
	{
		override def run() = f
	}
}