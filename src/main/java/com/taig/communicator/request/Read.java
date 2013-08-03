package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;
import com.taig.communicator.method.Method;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Read<T> extends Request<Response.Payload<T>, Event.Payload<T>>
{
	public Read( Method.Type method, URL url, Event.Payload<T> event )
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
	protected Response.Payload<T> talk( HttpURLConnection connection ) throws IOException
	{
		return new Response.Payload<T>(
			url,
			connection.getResponseCode(),
			connection.getResponseMessage(),
			connection.getHeaderFields(),
			receive( connection ) );
	}

	protected T receive( HttpURLConnection connection ) throws IOException
	{
		int length = connection.getContentLength();
		Updateable.Input input = new Receive( connection.getInputStream(), length );

		try
		{
			state.receive( length );
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
		public Receive( InputStream stream, long length )
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