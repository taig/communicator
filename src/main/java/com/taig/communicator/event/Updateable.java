package com.taig.communicator.event;

import com.taig.communicator.io.Countable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Updateable
{
	public void update() throws IOException;

	abstract class Input extends Countable.Stream.Input implements Updateable
	{
		protected int interval = 4096;

		protected int read = 0;

		public Input( InputStream stream, long length )
		{
			super( stream, length );
		}

		@Override
		public int read() throws IOException
		{
			int read = super.read();

			if( read >= 0 && ++read % interval == 0 )
			{
				update();
			}

			return read;
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

			if( interval - ( this.read % interval ) <= read )
			{
				update();
			}

			this.read += read;
			return read;
		}

		@Override
		public long skip( long n ) throws IOException
		{
			return super.skip( n );
		}

		@Override
		public int available() throws IOException
		{
			return super.available();
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

	abstract class Output extends Countable.Stream.Output implements Updateable
	{
		protected int interval = 4096;

		protected int written = 0;

		public Output( OutputStream stream, long length )
		{
			super( stream, length );
		}

		@Override
		public void write( int b ) throws IOException
		{
			if( ++written % interval == 0 )
			{
				update();
			}

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
			if( interval - ( this.written % interval ) <= length )
			{
				update();
			}

			this.written += length;
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