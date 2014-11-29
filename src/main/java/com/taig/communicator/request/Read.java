package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.io.CancelledIOException;
import com.taig.communicator.io.Updateable;
import com.taig.communicator.method.Method;
import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * A {@link Request} that expects to receive a response body (such as {@link Method.Type#GET}).
 *
 * @param <R> The {@link Response Response's} type.
 * @param <E> The {@link Event Event's} type.
 * @param <T> The {@link Parser parsed} response's type.
 */
public abstract class Read<R extends Response, E extends Event<R>, T> extends Request<R, E>
{
	protected Progress progress = null;

	/**
	 * Create a {@link Read} object.
	 *
	 * @param method The HTTP {@link Method.Type}.
	 * @param url    The {@link URL} to request.
	 * @param event  The {@link Event} callbacks. May be <code>null</code>.
	 */
	public Read( Method.Type method, URL url, E event )
	{
		super( method, url, event );
	}

	@Override
	protected LoadingState getLoadingState()
	{
		return new LoadingState();
	}

	/**
	 * Retrieve the {@link Request Request's} reading {@link Progress}.
	 * 
	 * @return The Request's reading Progress or <code>null</code> if no reading was done yet.
	 */
	public Progress getReadProgress()
	{
		return progress;
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

	/**
	 * Wrap the {@link HttpURLConnection HttpURLConnection's} {@link InputStream} with an {@link
	 * Updateable.Stream.Input} and parse the incoming response body.
	 *
	 * @param connection The HttpURLConnection used to obtain the connection's {@link InputStream}.
	 * @return The parsed response body.
	 * @throws IOException As the HttpURLConnection API specifies.
	 */
	protected T receive( HttpURLConnection connection ) throws IOException
	{
		InputStream stream;

		try
		{
			stream = connection.getInputStream();
		}
		catch( IOException exception )
		{
			stream = connection.getErrorStream();
		}

		if( stream == null )
		{
			throw new IOException( "Could not obtain an InputStream from the connection" );
		}

		Updateable.Stream.Input input = new Receive( stream, connection.getContentLength() );

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

	/**
	 * Read and parse the connection's {@link InputStream}.
	 *
	 * @param url   The requested {@link URL}.
	 * @param input The connection's InputStream.
	 * @return The parsed response body.
	 * @throws IOException As the HttpURLConnection API specifies.
	 */
	protected abstract T read( URL url, InputStream input ) throws IOException;

	/**
	 * Use the received data to build up a {@link Response} object.
	 *
	 * @param url     The requested {@link URL}.
	 * @param code    The HTTP status code.
	 * @param message The HTTP status message.
	 * @param headers The response headers.
	 * @param body    The parsed response body.
	 * @return A Response that represents that received data.
	 */
	protected abstract R summarize( URL url, int code, String message, Map<String, List<String>> headers, T body );

	protected class LoadingState extends Request<R, E>.LoadingState
	{
		@Override
		public void receive()
		{
			super.receive();

			progress = new Progress( 0, -1 );
		}

		@Override
		public void receiving( int current, long total )
		{
			progress.setCurrent( current );
			progress.setTotal( total );

			super.receiving( current, total );
		}
	}

	/**
	 * An internal {@link Updateable.Stream.Input} class used to trigger the {@link Event#onReceive(long, long)}
	 * callback and to regularly check if the {@link Request} has been cancelled.
	 */
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
				throw new CancelledIOException( (int) getLength() );
			}

			state.receiving( read, getLength() );
		}
	}
}