package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Stream;

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
	protected void send( HttpURLConnection connection ) throws IOException {}

	@Override
	protected T receive( HttpURLConnection connection ) throws IOException
	{
		InputStream input = connection.getInputStream();

		try
		{
			state.receive();

			return read( url, new Stream.Input( input, connection.getContentLength() )
			{
				@Override
				public void update() throws IOException
				{
					if( cancelled )
					{
						throw new InterruptedIOException( "Connection cancelled" );
					}

					state.receiving( read, length );
				}
			} );
		}
		finally
		{
			input.close();
		}
	}

	protected abstract T read( URL url, Stream.Input input ) throws IOException;
}