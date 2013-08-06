package com.taig.communicator.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adds an explicit availability of length data to a class. This interface is intended for use with streams to overcome
 * a streams generally considered unreliable <code>available()</code> method.
 */
public interface Countable
{
	/**
	 * Retrieve the resource's total amount of bytes or <code>-1</code> if unknown.
	 *
	 * @return The resource's total amount of bytes or <code>-1</code> if unknown.
	 */
	public long getLength();

	/**
	 * This interface serves the sole purpose to maintain a proper namespace (e.g. {@link Countable.Stream.Input}) and
	 * is not designated to be used in any other way.
	 */
	public interface Stream extends Countable
	{
		/**
		 * An {@link InputStream} wrapper that implements the {@link Countable} interface. It is able to give
		 * information on its underlying resource's size.
		 */
		public static class Input extends InputStream implements Stream
		{
			private InputStream stream;

			private long length;

			/**
			 * Create an {@link Input} with unknown resource length.
			 *
			 * @param stream The {@link InputStream} to be wrapped with unknown resource length.
			 */
			public Input( InputStream stream )
			{
				this( stream, -1 );
			}

			/**
			 * Create an {@link Input} based on a {@link ByteArrayInputStream}, that provides a reliable length
			 * retrieval via its {@link InputStream#available()} method.
			 *
			 * @param stream The ByteArrayInputStream to be wrapped.
			 */
			public Input( ByteArrayInputStream stream )
			{
				this( stream, stream.available() );
			}

			/**
			 * Create an {@link Input}.
			 *
			 * @param stream The {@link InputStream} to be wrapped.
			 * @param length The length of the InputStream's underlying resource (amount of bytes) or <code>-1</code>
			 *               if unknown.
			 */
			public Input( InputStream stream, long length )
			{
				this.stream = stream;
				this.length = length;
			}

			@Override
			public long getLength()
			{
				return length;
			}

			@Override
			public int read() throws IOException
			{
				return stream.read();
			}

			@Override
			public int read( byte[] bytes ) throws IOException
			{
				return stream.read( bytes );
			}

			@Override
			public int read( byte[] bytes, int offset, int length ) throws IOException
			{
				return stream.read( bytes, offset, length );
			}

			@Override
			public long skip( long n ) throws IOException
			{
				return stream.skip( n );
			}

			@Override
			public int available() throws IOException
			{
				return stream.available();
			}

			@Override
			public void close() throws IOException
			{
				stream.close();
			}

			@Override
			public synchronized void mark( int readLimit )
			{
				stream.mark( readLimit );
			}

			@Override
			public synchronized void reset() throws IOException
			{
				stream.reset();
			}

			@Override
			public boolean markSupported()
			{
				return stream.markSupported();
			}
		}

		/**
		 * An {@link OutputStream} wrapper that implements the {@link Countable} interface. It is able to give
		 * information on its underlying resource's size.
		 */
		public class Output extends OutputStream implements Stream
		{
			private OutputStream stream;

			private long length;

			/**
			 * Create an {@link Output} with unknown resource length.
			 *
			 * @param stream The {@link OutputStream} to be wrapped with unknown resource length.
			 */
			public Output( OutputStream stream )
			{
				this( stream, -1 );
			}

			/**
			 * Create an {@link Output}.
			 *
			 * @param stream The {@link OutputStream} to be wrapped.
			 * @param length The length of the OutputStream's underlying resource (amount of bytes) or <code>-1</code>
			 *               if unknown.
			 */
			public Output( OutputStream stream, long length )
			{
				this.stream = stream;
				this.length = length;
			}

			@Override
			public long getLength()
			{
				return length;
			}

			@Override
			public void write( int b ) throws IOException
			{
				stream.write( b );
			}

			@Override
			public void write( byte[] bytes ) throws IOException
			{
				stream.write( bytes );
			}

			@Override
			public void write( byte[] bytes, int offset, int length ) throws IOException
			{
				stream.write( bytes, offset, length );
			}

			@Override
			public void flush() throws IOException
			{
				stream.flush();
			}

			@Override
			public void close() throws IOException
			{
				stream.close();
			}
		}
	}
}