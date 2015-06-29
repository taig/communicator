package io.taig.communicator.ops

import scala.concurrent.ExecutionContext
import scala.util.Try

trait Future[T]
{
	val future: scala.concurrent.Future[T]

	/**
	 * Alternative to Future.onSuccess(), allowing for method chaining
	 * 
	 * Please note that the order of your chained callbacks does not guarantee to be triggered in the same order. This
	 * depends on the ExecutionContext.
	 */
	def done[U]( pf: PartialFunction[T, U] )( implicit context: ExecutionContext ): future.type =
	{
		future.onSuccess( pf )
		future
	}

	/**
	 * Alternative to Future.onFailure(), allowing for method chaining
	 *
	 * Please note that the order of your chained callbacks does not guarantee to be triggered in the same order. This
	 * depends on the ExecutionContext.
	 */
	def fail[U]( pf: PartialFunction[Throwable, U] )( implicit context: ExecutionContext ): future.type =
	{
		future.onFailure( pf )
		future
	}

	/**
	 * Alternative to Future.onComplete(), allowing for method chaining
	 *
	 * Please note that the order of your chained callbacks does not guarantee to be triggered in the same order. This
	 * depends on the ExecutionContext.
	 */
	def always[U]( pf: PartialFunction[Try[T], U] )( implicit context: ExecutionContext ): future.type =
	{
		future.onComplete( pf )
		future
	}
}