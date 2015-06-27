package io.taig.communicator.ops

import scala.concurrent.ExecutionContext

trait Future[T]
{
	val future: scala.concurrent.Future[T]

	def onSuccess[U]( f: T => U )( implicit context: ExecutionContext ): future.type =
	{
		future.onSuccess{ case value => f( value ) }
		future
	}

	def onFailure[U]( f: Throwable => U )( implicit context: ExecutionContext ): future.type =
	{
		future.onFailure{ case exception => f( exception ) }
		future
	}
}