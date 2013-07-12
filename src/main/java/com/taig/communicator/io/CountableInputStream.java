package com.taig.communicator.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class CountableInputStream extends java.io.InputStream
{
	protected InputStream stream;

	protected int length;

	public CountableInputStream( InputStream stream )
	{
		this( stream, -1 );
	}

	public CountableInputStream( InputStream stream, int length )
	{
		this.stream = stream;
		this.length = length;
	}

	public int getLength()
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