package io.taig

import _root_.io.taig.communicator.request.{Handler, Parser, Plain}

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

		def request( f: S => Plain )( implicit executor: Context ) = future.flatMap( f ).asInstanceOf[Plain]
	}
}