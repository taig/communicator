package io.taig.communicator.internal.request

import io.taig.communicator.internal._
import io.taig.communicator.internal

import scala.concurrent.{ExecutionContext => Context}

trait Receivable
{
	this: Request[_, _ , internal.interceptor.Read] =>

	def onReceive( f: internal.event.Progress.Receive => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.receive = Some(
			( progress: internal.event.Progress.Receive ) => executor.execute( f( progress ) )
		)

		this
	}
}