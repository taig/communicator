package io.taig.communicator.internal.request

import io.taig.communicator.internal.event.Progress
import io.taig.communicator.internal.interceptor.Read

import scala.concurrent.{ExecutionContext => Context}

trait Receivable
{
	this: Request[_, _ , Read] =>

	def onReceive( f: Progress.Receive => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.receive = Some( ( progress: Progress.Receive ) =>
		{
			executor.execute( new Runnable
			{
				override def run() = f( progress )
			} )
		} )

		this
	}
}