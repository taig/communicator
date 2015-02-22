package io.taig.communicator

import java.io.InterruptedIOException

trait Cancelable
{
	def cancel(): Unit

	def isCanceled: Boolean

	@throws[InterruptedIOException]
	protected def cancel( transferred: Long, message: String = "Request canceled" ) =
	{
		val error = new InterruptedIOException( message )
		error.bytesTransferred = transferred.toInt
		throw error
	}
}

private[communicator] object Cancelable
{
	trait	Simple
	extends	Cancelable
	{
		@volatile
		private var canceled = false

		override def cancel() = canceled = true

		override def isCanceled = canceled
	}
}