package com.taig.communicator.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adds the ability to a class to run an update routine (implemented by a child class) on a specific event.
 * <p/>
 * This interface is designated to be used with streams and should provide updates whenever a specific amount of bytes
 * has been read or written.
 *
 * @see Stream.Input
 * @see Stream.Output
 */
public interface Updateable
{
	/**
	 * Run the update routine.
	 * <p/>
	 * This method is used as a callback to be implemented by a subclass, it should therefore not be called explicitly.
	 *
	 * @throws IOException Allows the update callback to interrupt the execution.
	 */
	public void update() throws IOException;

	/**
	 * This interface serves the sole purpose to maintain a proper namespace (e.g. {@link Updateable.Stream.Input}) and
	 * is not designated to be used in any other way.
	 */
	public interface Stream extends Updateable
	{
		public static final int INTERVAL = 4096;

		/**
		 * An {@link InputStream} wrapper that implements the {@link Updateable} interface. It calls the update callback
		 * each time a specific amount of bytes has been read.
		 */
		abstract class Input extends Countable.Stream.Input implements Stream
		{
			protected int read = 0;

			public Input( InputStream stream, long length )
			{
				super( stream, length );
			}

			@Override
			public int read() throws IOException
			{
				if( this.read % INTERVAL == 0 )
				{
					update();
				}

				this.read++;
				return super.read();
			}

			@Override
			public int read( byte[] bytes ) throws IOException
			{
				return read( bytes, 0, bytes.length );
			}

			@Override
			public int read( byte[] bytes, int offset, int length ) throws IOException
			{
				int read = super.read( bytes, offset, length );

				if( this.read == 0 || INTERVAL - ( this.read % INTERVAL ) <= read )
				{
					update();
				}

				this.read += read;
				return read;
			}

			@Override
			public long skip( long n ) throws IOException
			{
				long skipped = super.skip( n );
				read += skipped;
				return skipped;
			}

			@Override
			public int available() throws IOException
			{
				if( getLength() > -1 )
				{
					long available = getLength() - read;
					return (int) Math.min( Integer.MAX_VALUE, available );
				}
				else
				{
					return super.available();
				}
			}

			@Override
			public void close() throws IOException
			{
				super.close();
			}

			@Override
			public synchronized void mark( int readLimit )
			{
				super.mark( readLimit );
			}

			@Override
			public synchronized void reset() throws IOException
			{
				super.reset();
			}

			@Override
			public boolean markSupported()
			{
				return super.markSupported();
			}
		}

		/**
		 * An {@link OutputStream} wrapper that implements the {@link Updateable} interface. It calls the update callback
		 * each time a specific amount of bytes has been written.
		 */
		abstract class Output extends Countable.Stream.Output implements Stream
		{
			protected int written = 0;

			public Output( OutputStream stream, long length )
			{
				super( stream, length );
			}

			@Override
			public void write( int b ) throws IOException
			{
				if( written % INTERVAL == 0 )
				{
					update();
				}

				written++;
				super.write( b );
			}

			@Override
			public void write( byte[] bytes ) throws IOException
			{
				write( bytes, 0, bytes.length );
			}

			@Override
			public void write( byte[] bytes, int offset, int length ) throws IOException
			{
				if( written == 0 || INTERVAL - ( written % INTERVAL ) <= length )
				{
					update();
				}

				written += length;
				super.write( bytes, offset, length );
			}

			@Override
			public void flush() throws IOException
			{
				super.flush();
			}

			@Override
			public void close() throws IOException
			{
				super.close();
			}
		}
	}
}