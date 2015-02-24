package io.taig.communicator.request

import _root_.io.taig.communicator

import scala.concurrent.{ExecutionContext => Context}

trait Receivable
{
	this: Request[_, _ , communicator.interceptor.Read] =>

	def onReceive( f: communicator.event.Progress.Receive => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.receive = Some( ( progress: communicator.event.Progress.Receive ) =>
		{
			executor.execute( new Runnable
			{
				override def run() = f( progress )
			} )
		} )

		this
	}
}