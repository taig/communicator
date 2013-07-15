package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Read<T> extends Request<T>
{
	public Read( String method, URL url, Event<T> event )
	{
		super( method, url, event );
	}

	@Override
	public HttpURLConnection connect() throws IOException
	{
		HttpURLConnection connection = super.connect();
		connection.setDoInput( true );
		return connection;
	}

	@Override
	protected void send( HttpURLConnection connection ) throws IOException {}

	@Override
	protected T receive( HttpURLConnection connection ) throws IOException
	{
		Updateable.Input input = new Receive( connection.getInputStream(), connection.getContentLength() );

		try
		{
			state.receive();
			return read( url, input );
		}
		finally
		{
			input.close();
		}
	}

	protected abstract T read( URL url, Updateable.Input input ) throws IOException;

	protected class Receive extends Updateable.Input
	{
		public Receive( InputStream stream, int length )
		{
			super( stream, length );
		}

		@Override
		public void update() throws IOException
		{
			if( cancelled )
			{
				throw new InterruptedIOException( "Connection cancelled" );
			}

			state.receiving( read, length );
		}
	}
}