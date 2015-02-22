package io.taig.communicator.body

import java.io.InterruptedIOException

import com.squareup.okhttp.ResponseBody
import io.taig.communicator.{Cancelable, Progress}
import okio._

/**
 * A ResponseBody wrapper that takes care of notifying the event listener and checks regularly checks on the canceled
 * flag
 *
 * @param wrapped The wrapped ResponseBody
 * @param listener Event listener to update on progress, may be <code>null</code>
 */
class	Receive( wrapped: ResponseBody, listener: Progress.Receive => Unit )
extends	ResponseBody
with	Cancelable.Simple
{
	/**
	 * Prevent recreating the length object on every listener call
	 */
	private lazy val length = contentLength() match
	{
		case -1 => None
		case length => Some( length )
	}

	@throws[InterruptedIOException]( "If the request was canceled" )
	private def update( current: Long ) =
	{
		if( listener != null )
		{
			listener( Progress.Receive( current, length ) )
		}

		if( isCanceled )
		{
			cancel( current, "Request canceled on receive" )
		}
	}

	override def source() = Okio.buffer( new Source( wrapped.source() ) )

	override def contentLength() = wrapped.contentLength()

	override def contentType() = wrapped.contentType()

	private class	Source( wrapped: okio.BufferedSource )
	extends			ForwardingSource( wrapped )
	{
		private var current = 0L

		override def read( sink: Buffer, count: Long ) = super.read( sink, count ) match
		{
			case count if count != -1 =>
			{
				current += count
				update( current )
				count
			}
			case _ => -1
		}
	}
}