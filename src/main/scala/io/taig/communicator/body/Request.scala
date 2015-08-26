package io.taig.communicator.body

import _root_.io.taig.communicator.Progress
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.internal.Util.closeQuietly
import okio.{ Buffer, BufferedSink }

/**
 * A RequestBody wrapper that takes care of notifying the event listener
 *
 * @param wrapped The wrapped RequestBody, may be <code>null</code>
 * @param event Event listener to update on progress, may be <code>null</code>
 */
class Request( wrapped: RequestBody, var event: Progress.Send ⇒ Unit )
        extends RequestBody {
    require(
        wrapped.contentLength() > -1 || wrapped.contentLength() == -1 && event == null,
        "The provided RequestBody must specify a proper content length if you want to use an onSend listener"
    )

    private lazy val length = contentLength()

    private def update( current: Long ) = if ( event != null ) event( Progress.Send( current, length ) )

    override def contentLength() = wrapped.contentLength()

    override def contentType() = wrapped.contentType()

    override def writeTo( sink: BufferedSink ) = {
        var size = 0
        val temporary = new Buffer()

        try {
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
                .foreach( length ⇒
                    {
                        size += length
                        sink.write( buffer, 0, length )
                        update( size )
                    } )
        }
        finally {
            closeQuietly( temporary )
        }
    }
}