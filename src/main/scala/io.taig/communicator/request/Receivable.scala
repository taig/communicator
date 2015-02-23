package io.taig.communicator.request

import io.taig.communicator.event.Progress
import io.taig.communicator._

import scala.concurrent.{ExecutionContext => Context}

trait Receivable
{
	this: Request[_, _ , interceptor.Read] =>

	def onReceive( f: Progress.Receive => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.receive = Option( ( progress: Progress.Receive ) => executor.execute( f( progress ) ) )
		this
	}
}