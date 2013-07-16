package com.taig.communicator.request;

import android.util.Log;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.io.Cancelable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.CookieStore;
import java.net.*;
import java.util.*;

import static com.taig.communicator.request.Header.Request.COOKIE;

public abstract class Request<T> implements Cancelable, Runnable
{
	protected static final String TAG = Request.class.getName();

	protected String method;

	protected URL url;

	protected Event<T>.Proxy event;

	protected LoadingState state = new LoadingState();

	protected boolean cancelled = false;

	protected boolean cache = false;

	protected int chunkLength = -1;

	protected int connectTimeout = 0;

	protected int contentLength = -1;

	protected int modifiedSince = 0;

	protected int readTimeout = 0;

	protected boolean redirect = false;

	protected boolean userInteraction = false;

	protected Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();

	public Request( String method, URL url, Event<T> event )
	{
		this.method = method;
		this.url = url;
		setEvent( event );
	}

	public State getState()
	{
		return state.current;
	}

	public URL getUrl()
	{
		return url;
	}

	public Event<T> getEvent()
	{
		return event.getEvent();
	}

	public Request<T> setEvent( Event<T> event )
	{
		this.event = event == null ? null : event.new Proxy();
		return this;
	}

	@Override
	public void cancel()
	{
		this.cancelled = true;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

	public Request<T> addHeader( String key, String value )
	{
		Collection<String> values = this.headers.get( key );

		if( values == null )
		{
			setHeader( key, value );
		}
		else
		{
			values.add( value );
		}

		return this;
	}

	public Request<T> setHeader( String key, String value )
	{
		Collection<String> values = new ArrayList<String>();
		values.add( value );
		return setHeaders( key, values );
	}

	public Request<T> addHeaders( String key, Collection<String> values )
	{
		for( String value : values )
		{
			addHeader( key, value );
		}

		return this;
	}

	public Request<T> setHeaders( String key, Collection<String> values )
	{
		if( values == null )
		{
			this.headers.remove( key );
		}
		else
		{
			this.headers.put( key, values );
		}

		return this;
	}

	public Request<T> setHeaders( Map<String, Collection<String>> headers )
	{
		this.headers = headers;
		return this;
	}

	public Request<T> addCookie( HttpCookie cookie )
	{
		return addHeader( COOKIE, cookie.toString() );
	}

	public Request<T> addCookie( String key, String value )
	{
		HttpCookie cookie = new HttpCookie( key, value );
		cookie.setVersion( 0 );
		return addCookie( cookie );
	}

	public Request<T> setCookie( HttpCookie cookie )
	{
		return setHeader( COOKIE, cookie.toString() );
	}

	public Request<T> setCookie( String key, String value )
	{
		HttpCookie cookie = new HttpCookie( key, value );
		cookie.setVersion( 0 );
		return setCookie( cookie );
	}

	public Request<T> addCookies( Collection<HttpCookie> cookies )
	{
		for( HttpCookie cookie : cookies )
		{
			addCookie( cookie );
		}

		return this;
	}

	public Request<T> addCookies( CookieStore store )
	{
		try
		{
			addCookies( store.get( url.toURI() ) );
		}
		catch( URISyntaxException exception )
		{
			Log.w( TAG, "The cookies of a CookieStore couldn't be added to a Request because the associated " +
						"URL (" + url + ") could not be converted to an URI", exception );
		}

		return this;
	}

	public Request<T> addCookies( Response<?> response )
	{
		List<HttpCookie> cookies = response.getCookies();

		if( cookies != null )
		{
			addCookies( cookies );
		}

		return this;
	}

	public Request<T> setCookies( Collection<HttpCookie> cookies )
	{
		Collection<String> values = null;

		if( cookies != null )
		{
			values = new ArrayList<String>();

			for( HttpCookie cookie : cookies )
			{
				values.add( cookie.toString() );
			}
		}

		return setHeaders( COOKIE, values );
	}

	public Request<T> setCookies( CookieStore store )
	{
		try
		{
			List<HttpCookie> cookies = store.get( url.toURI() );
			setCookies( cookies.isEmpty() ? null : cookies );
		}
		catch( URISyntaxException exception )
		{
			Log.w( TAG, "The cookies of a CookieStore couldn't be added to a Request because the associated " +
						"URL (" + url + ") could not be converted to an URI", exception );
		}

		return this;
	}

	public Request<T> setCookies( Response<?> response )
	{
		return setCookies( response.getCookies() );
	}

	public Request<T> allowUserInteraction( boolean allow )
	{
		this.userInteraction = allow;
		return this;
	}

	public Request<T> ifModifiedSince( int modifiedSince )
	{
		this.modifiedSince = modifiedSince;
		return this;
	}

	public Request<T> followRedirects( boolean follow )
	{
		this.redirect = follow;
		return this;
	}

	public Request<T> streamChunks( int chunkLength )
	{
		this.chunkLength = chunkLength;
		return this;
	}

	public Request<T> streamFixedLength( int contentLength )
	{
		this.contentLength = contentLength;
		return this;
	}

	public Request<T> timeoutConnect( int timeout )
	{
		this.connectTimeout = timeout;
		return this;
	}

	public Request<T> timeoutRead( int timeout )
	{
		this.readTimeout = timeout;
		return this;
	}

	public Request<T> useCache( boolean use )
	{
		this.cache = use;
		return this;
	}

	public HttpURLConnection connect() throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setAllowUserInteraction( userInteraction );
		connection.setConnectTimeout( connectTimeout );
		connection.setIfModifiedSince( modifiedSince );
		connection.setInstanceFollowRedirects( redirect );
		connection.setReadTimeout( readTimeout );
		connection.setRequestMethod( method );
		connection.setUseCaches( cache );

		if( chunkLength > 0 )
		{
			connection.setChunkedStreamingMode( chunkLength );
		}

		if( contentLength >= 0 )
		{
			connection.setFixedLengthStreamingMode( contentLength );
		}

		for( Map.Entry<String, Collection<String>> header : this.headers.entrySet() )
		{
			StringBuilder builder = new StringBuilder();
			String delimiter = header.getKey().equals( COOKIE ) ? "; " : ",";

			for( Iterator<String> iterator = header.getValue().iterator(); iterator.hasNext(); )
			{
				builder.append( iterator.next() ).append( iterator.hasNext() ? delimiter : "" );
			}

			connection.setRequestProperty( header.getKey(), builder.toString() );
		}

		return connection;
	}

	/**
	 * Execute this request.
	 * <p/>
	 * If an {@link Event} is specified, its finish callbacks {@link Event#onSuccess(Response)}, {@link
	 * Event#onFailure(Throwable)}, {@link Event#onSuccess(Response)} and the according {@link
	 * Event#onEvent(com.taig.communicator.event.State)} calls will not be executed.
	 *
	 * @return The {@link Response} object that keeps the connection response meta data (such as response code) and the
	 * payload that will be <code>null</code> if the HTTP server returned an error.
	 * @throws IOException
	 */
	public Response<T> request() throws IOException
	{
		HttpURLConnection connection = connect();

		try
		{
			if( cancelled )
			{
				throw new InterruptedIOException( "Connection cancelled" );
			}

			state.start();
			send( connection );
			Response<T> response = new Response<T>(
				url,
				connection.getResponseCode(),
				connection.getResponseMessage(),
				connection.getHeaderFields(),
				receive( connection )
			);
			state.success();
			return response;
		}
		catch( InterruptedIOException exception )
		{
			state.cancel( exception );
			throw exception;
		}
		catch( IOException exception )
		{
			state.failure();
			throw exception;
		}
		finally
		{
			connection.disconnect();
		}
	}

	public void run()
	{
		try
		{
			state.success( request() );
		}
		catch( IOException exception )
		{
			state.failure( exception );
		}
	}

	protected abstract void send( HttpURLConnection connection ) throws IOException;

	protected abstract T receive( HttpURLConnection connection ) throws IOException;

	protected class LoadingState
	{
		protected State current = State.IDLE;

		public void start()
		{
			current = State.START;

			if( event != null )
			{
				event.start();
			}
		}

		public void cancel( InterruptedIOException exception )
		{
			current = State.CANCEL;

			if( event != null )
			{
				event.cancel( exception );
			}
		}

		public void send()
		{
			current = State.SEND;
		}

		public void sending( int current, int total )
		{
			if( event != null )
			{
				event.send( current, total );
			}
		}

		public void receive()
		{
			current = State.RECEIVE;
		}

		public void receiving( int current, int total )
		{
			if( event != null )
			{
				event.receive( current, total );
			}
		}

		public void success()
		{
			current = State.SUCCESS;
		}

		public void success( Response<T> response )
		{
			if( event != null )
			{
				event.success( response );
			}
		}

		public void failure()
		{
			current = State.FAILURE;
		}

		public void failure( Throwable error )
		{
			if( event != null )
			{
				event.failure( error );
			}
		}
	}
}