package com.taig.communicator.request;

import com.taig.communicator.data.Data;
import com.taig.communicator.event.Event;
import com.taig.communicator.io.Updateable;
import com.taig.communicator.method.Method;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A {@link Request} that expects to send payload along with the request headers (such as {@link Method.Type#POST}).
 *
 * @param <R> The {@link Response Response's} type.
 * @param <E> The {@link Event Event's} type.
 * @param <T> The {@link com.taig.communicator.result.Parser parsed} response's type.
 */
public abstract class Write<R extends Response, E extends Event<R>, T> extends Read<R, E, T>
{
	private Data data;

	/**
	 * Create a {@link Write} object.
	 *
	 * @param method The HTTP {@link Method.Type}.
	 * @param url    The {@link URL} to request.
	 * @param data   The {@link Data} to serve as request body. May be <code>null</code>.
	 * @param event  The {@link Event} callbacks. May be <code>null</code>.
	 */
	public Write( Method.Type method, URL url, Data data, E event )
	{
		super( method, url, event );
		this.data = data;
	}

	/**
	 * Retrieve the {@link Data} that will serve as request body.
	 *
	 * @return The Data that will serve as request body.
	 */
	public Data getData()
	{
		return data;
	}

	/**
	 * Specify the {@link Data} that will serve as request body.
	 *
	 * @param data The Data that will serve as request body. May be <code>null</code>.
	 */
	public void setData( Data data )
	{
		this.data = data;
	}

	@Override
	public HttpURLConnection connect() throws IOException
	{
		HttpURLConnection connection = super.connect();

		if( data != null )
		{
			data.apply( connection );
		}

		connection.setDoOutput( true );
		return connection;
	}

	@Override
	protected R talk( HttpURLConnection connection ) throws IOException
	{
		send( connection );
		return super.talk( connection );
	}

	/**
	 * Wrap the {@link HttpURLConnection HttpURLConnection's} {@link OutputStream} with an {@link
	 * Updateable.Stream.Output}.
	 *
	 * @param connection The HttpURLConnection used to obtain the connection's {@link OutputStream}.
	 * @throws IOException As the HttpURLConnection API specifies.
	 */
	protected void send( HttpURLConnection connection ) throws IOException
	{
		if( data != null )
		{
			Updateable.Stream.Output output = new Send( connection.getOutputStream(), data.getLength() );

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

	/**
	 * Write the connection's {@link OutputStream}.
	 *
	 * @param output The connection's OutputStream.
	 * @param data   The data to write.
	 * @throws IOException
	 */
	protected void write( Updateable.Stream.Output output, InputStream data ) throws IOException
	{
		byte[] buffer = new byte[1024];

		for( int read = 0; read != -1; read = data.read( buffer ) )
		{
			output.write( buffer, 0, read );
		}
	}

	/**
	 * An internal {@link Updateable.Stream.Output} class used to trigger the {@link Event#onSend(long, long)}
	 * callback and to regularly check if the {@link Request} has been cancelled.
	 */
	protected class Send extends Updateable.Stream.Output
	{
		public Send( OutputStream stream, long length )
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

			state.sending( written, getLength() );
		}
	}
}