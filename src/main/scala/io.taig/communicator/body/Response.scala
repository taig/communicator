package io.taig.communicator.body

import java.io.InterruptedIOException

import com.squareup.okhttp.ResponseBody
import io.taig.communicator
import okio._

/**
 * A ResponseBody wrapper that takes care of notifying the event listener and checks regularly checks on the canceled
 * flag
 *
 * @param wrapped Wrapped ResponseBody
 * @param event Event listener to update on progress, may be <code>null</code>
 */
class	Response( wrapped: ResponseBody, event: Option[communicator.event.Progress.Receive => Unit], zipped: Boolean )
extends	ResponseBody
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
	private def update( current: Long ) = event.foreach( _( communicator.event.Progress.Receive( current, length ) ) )

	override def source() =
	{
		if( zipped )
		{
			Okio.buffer( new GzipSource( new Source( wrapped.source() ) ) )
		}
		else
		{
			Okio.buffer( new Source( wrapped.source() ) )
		}
	}

	override def contentLength() = wrapped.contentLength()

	override def contentType() = wrapped.contentType()

	private class	Source( wrapped: okio.BufferedSource )
	extends			ForwardingSource( wrapped )
	{
		private var current = 0L

		override def read( sink: Buffer, count: Long ) =
		{
			if( current == 0 )
			{
				update( 0 )
			}

			super.read( sink, count ) match
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
}