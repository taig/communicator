package io.taig.communicator.body

import java.io.{IOException, InterruptedIOException}

import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.internal.Util.closeQuietly
import io.taig.communicator.{Cancelable, Progress}
import okio.{Buffer, BufferedSink}

/**
 * A RequestBody wrapper that takes care of notifying the event listener and checks regularly checks on the canceled
 * flag
 *
 * @param wrapped The wrapped RequestBody, may be <code>null</code>
 * @param listener Event listener to update on progress, may be <code>null</code>
 */
class	Send( wrapped: RequestBody, listener: Progress.Send => Unit )
extends	RequestBody
with	Cancelable.Simple
{
	require(
		wrapped.contentLength() > -1 || wrapped.contentLength() == -1 && listener == null,
		"The provided RequestBody must specify a proper content length if you want to use an onSend listener"
	)

	private lazy val length = contentLength()

	@throws[InterruptedIOException]( "If the request was canceled" )
	private def update( current: Long ) =
	{
		if( listener != null )
		{
			listener( Progress.Send( current, length ) )
		}

		if( isCanceled )
		{
			cancel( current, "Request canceled on send" )
		}
	}

	override def contentLength() = wrapped.contentLength()

	override def contentType() = wrapped.contentType()

	@throws[InterruptedIOException]( "If the request was canceled" )
	override def writeTo( sink: BufferedSink ) =
	{
		var size = 0
		val temporary = new Buffer()

		try
		{
			// Trigger initial send event
			update( 0 )

			// Write wrapped data to temporary sink
			// TODO This may cause memory issues when dealing with big files
			wrapped.writeTo( temporary )

			val buffer = new Array[Byte]( 1024 * 8 )

			// Write data from the temporary sink to the actual target sink but keep the event listeners
			// updated while doing so
			Iterator
				.continually( temporary.read( buffer ) )
				.takeWhile( _ != -1 )
				.foreach( length =>
				{
					size += length
					sink.write( buffer, 0, length )
					update( size )
				} )
		}
		catch
		{
			case error: IOException if error.getMessage == "Canceled" => cancel( size )
		}
		finally
		{
			closeQuietly( temporary )
		}
	}
}