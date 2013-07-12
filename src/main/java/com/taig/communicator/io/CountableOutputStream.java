package com.taig.communicator.io;

import java.io.IOException;
import java.io.OutputStream;

public abstract class CountableOutputStream extends OutputStream
{
	protected OutputStream stream;

	protected int length;

	public CountableOutputStream( OutputStream stream )
	{
		this( stream, -1 );
	}

	public CountableOutputStream( OutputStream stream, int length )
	{
		this.stream = stream;
		this.length = length;
	}

	public int getLength()
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