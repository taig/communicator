package com.taig.communicator.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Countable
{
	public long getLength();

	public interface Stream
	{
		public static class Input extends InputStream implements Countable
		{
			protected InputStream stream;

			protected long length;

			public Input( InputStream stream )
			{
				this( stream, -1 );
			}

			public Input( ByteArrayInputStream stream )
			{
				this( stream, stream.available() );
			}

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

		public class Output extends OutputStream implements Countable
		{
			protected OutputStream stream;

			protected long length;

			public Output( OutputStream stream )
			{
				this( stream, -1 );
			}

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