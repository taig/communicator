package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.Updateable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.taig.communicator.request.Header.Request.CONTENT_LENGTH;
import static com.taig.communicator.request.Header.Request.CONTENT_TYPE;

public abstract class Write<T> extends Read<T>
{
	protected Data data;

	public Write( String method, URL url, Data data, Event<T> event )
	{
		super( method, url, event );
		setData( data );
	}

	public Data getData()
	{
		return data;
	}

	public Write<T> setData( Data data )
	{
		this.data = data;

		if( data != null )
		{
			setHeader( CONTENT_TYPE, data.getContentType().toString() );
			setHeader( CONTENT_LENGTH, data.getLength() > 0 ? String.valueOf( data.getLength() ) : "0" );
		}
		else
		{
			removeHeaders( CONTENT_TYPE );
			setHeader( CONTENT_LENGTH, "0" );
		}

		return null;
	}

	@Override
	public HttpURLConnection connect() throws IOException
	{
		HttpURLConnection connection = super.connect();
		connection.setDoOutput( true );
		return connection;
	}

	@Override
	protected void send( HttpURLConnection connection ) throws IOException
	{
		if( data != null )
		{
			Updateable.Output output = new Send( connection.getOutputStream(), data.getLength() );

			try
			{
				state.send();
				write( output, data );
			}
			finally
			{
				output.close();
				data.close();
			}
		}
	}

	protected void write( Updateable.Output output, InputStream data ) throws IOException
	{
		byte[] buffer = new byte[1024];

		for( int read = 0; read != -1; read = data.read( buffer ) )
		{
			output.write( buffer, 0, read );
		}
	}

	protected class Send extends Updateable.Output
	{
		public Send( OutputStream stream, int length )
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

			state.sending( written, length );
		}
	}
}