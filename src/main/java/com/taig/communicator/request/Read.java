package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.io.Updateable;
import com.taig.communicator.method.Method;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class Read<R extends Response, E extends Event<R>, T> extends Request<R, E>
{
	public Read( Method.Type method, URL url, E event )
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
	protected R talk( HttpURLConnection connection ) throws IOException
	{
		return summarize(
			url,
			connection.getResponseCode(),
			connection.getResponseMessage(),
			connection.getHeaderFields(),
			receive( connection ) );
	}

	protected T receive( HttpURLConnection connection ) throws IOException
	{
		int length = connection.getContentLength();
		Updateable.Stream.Input input = new Receive( connection.getInputStream(), length );

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

	protected abstract T read( URL url, Updateable.Stream.Input input ) throws IOException;

	protected abstract R summarize( URL url, int code, String message, Map<String, List<String>> headers, T body );

	protected class Receive extends Updateable.Stream.Input
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

			state.receiving( read, getLength() );
		}
	}
}