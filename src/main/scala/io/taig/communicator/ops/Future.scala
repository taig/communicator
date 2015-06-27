package io.taig.communicator.ops

import scala.concurrent.ExecutionContext
import scala.util.Try

trait Future[T]
{
	val future: scala.concurrent.Future[T]

	/**
	 * Alternative to [[scala.concurrent.Future.onSuccess()]], allowing for method chaining
	 * 
	 * Please note that the order of your chained callbacks does not guarantee to be triggered in the same order. This
	 * depends on the [[scala.concurrent.ExecutionContext]].
	 */
	def done[U]( pf: PartialFunction[T, U] )( implicit context: ExecutionContext ): future.type =
	{
		future.onSuccess( pf )
		future
	}

	/**
	 * Alternative to [[scala.concurrent.Future.onFailure()]], allowing for method chaining
	 *
	 * Please note that the order of your chained callbacks does not guarantee to be triggered in the same order. This
	 * depends on the [[scala.concurrent.ExecutionContext]].
	 */
	def failed[U]( pf: PartialFunction[Throwable, U] )( implicit context: ExecutionContext ): future.type =
	{
		future.onFailure( pf )
		future
	}

	/**
	 * Alternative to [[scala.concurrent.Future.onComplete()]], allowing for method chaining
	 *
	 * Please note that the order of your chained callbacks does not guarantee to be triggered in the same order. This
	 * depends on the [[scala.concurrent.ExecutionContext]].
	 */
	def always[U]( pf: PartialFunction[Try[T], U] )( implicit context: ExecutionContext ): future.type =
	{
		future.onComplete( pf )
		future
	}
}