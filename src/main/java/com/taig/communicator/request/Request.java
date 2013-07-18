package com.taig.communicator.request;

import android.util.Log;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.method.Method;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.CookieStore;
import java.net.*;
import java.util.*;

import static com.taig.communicator.request.Header.Request.ACCEPT_CHARSET;
import static com.taig.communicator.request.Header.Request.COOKIE;

public abstract class Request<R extends Response, E extends Event<R>> implements Cancelable, Runnable
{
	public static final String CHARSET = "UTF-8";

	protected static final String TAG = Request.class.getName();

	protected Method.Type method;

	protected URL url;

	protected Event<R>.Proxy event;

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

	public Request( Method.Type method, URL url, E event )
	{
		this.method = method;
		this.url = url;
		setEvent( event );
		setHeader( ACCEPT_CHARSET, CHARSET );
	}

	public Method.Type getMethod()
	{
		return method;
	}

	public State getState()
	{
		return state.current;
	}

	public URL getUrl()
	{
		return url;
	}

	public Request<R, E> setEvent( E event )
	{
		this.event = event == null ? null : event.getProxy();
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

	public boolean isBusy()
	{
		State state = getState();
		return state == State.START || state == State.SEND || state == State.RECEIVE;
	}

	public Request<R, E> addHeader( String key, String value )
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

	public Request<R, E> setHeader( String key, String value )
	{
		Collection<String> values = new ArrayList<String>();
		values.add( value );
		return setHeaders( key, values );
	}

	public Request<R, E> addHeaders( String key, Collection<String> values )
	{
		for( String value : values )
		{
			addHeader( key, value );
		}

		return this;
	}

	public Request<R, E> setHeaders( String key, Collection<String> values )
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

	public Request<R, E> setHeaders( Map<String, Collection<String>> headers )
	{
		this.headers = headers;
		return this;
	}

	public Request<R, E> addCookie( HttpCookie cookie )
	{
		return addHeader( COOKIE, cookie.toString() );
	}

	public Request<R, E> addCookie( String key, String value )
	{
		HttpCookie cookie = new HttpCookie( key, value );
		cookie.setVersion( 0 );
		return addCookie( cookie );
	}

	public Request<R, E> setCookie( HttpCookie cookie )
	{
		return setHeader( COOKIE, cookie.toString() );
	}

	public Request<R, E> setCookie( String key, String value )
	{
		HttpCookie cookie = new HttpCookie( key, value );
		cookie.setVersion( 0 );
		return setCookie( cookie );
	}

	public Request<R, E> addCookies( Collection<HttpCookie> cookies )
	{
		for( HttpCookie cookie : cookies )
		{
			addCookie( cookie );
		}

		return this;
	}

	public Request<R, E> addCookies( CookieStore store )
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

	public Request<R, E> addCookies( Response response )
	{
		List<HttpCookie> cookies = response.getCookies();

		if( cookies != null )
		{
			addCookies( cookies );
		}

		return this;
	}

	public Request<R, E> setCookies( Collection<HttpCookie> cookies )
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

	public Request<R, E> setCookies( CookieStore store )
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

	public Request<R, E> setCookies( Response response )
	{
		return setCookies( response.getCookies() );
	}

	public Request<R, E> allowUserInteraction( boolean allow )
	{
		this.userInteraction = allow;
		return this;
	}

	public Request<R, E> ifModifiedSince( int modifiedSince )
	{
		this.modifiedSince = modifiedSince;
		return this;
	}

	public Request<R, E> followRedirects( boolean follow )
	{
		this.redirect = follow;
		return this;
	}

	public Request<R, E> streamChunks( int chunkLength )
	{
		this.chunkLength = chunkLength;
		return this;
	}

	public Request<R, E> streamFixedLength( int contentLength )
	{
		this.contentLength = contentLength;
		return this;
	}

	public Request<R, E> timeoutConnect( int timeout )
	{
		this.connectTimeout = timeout;
		return this;
	}

	public Request<R, E> timeoutRead( int timeout )
	{
		this.readTimeout = timeout;
		return this;
	}

	public Request<R, E> useCache( boolean use )
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
		connection.setRequestMethod( method.name() );
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
	public R request() throws IOException
	{
		HttpURLConnection connection = null;

		try
		{
			state.start();
			connection = connect();
			connection.connect();

			if( cancelled )
			{
				throw new InterruptedIOException( "Connection cancelled" );
			}

			Log.d( "ASDF", "start talking" );
			R response = talk( connection );
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
			if( connection != null )
			{
				connection.disconnect();
			}
		}
	}

	public void run()
	{
		try
		{
			state.success( request() );
		}
		catch( InterruptedIOException exception )
		{
			// Don't fail/finish cancelled requests.
		}
		catch( IOException exception )
		{
			state.failure( exception );
		}
	}

	protected abstract R talk( HttpURLConnection connection ) throws IOException;

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

		public void send( int total )
		{
			current = State.SEND;
			sending( 0, total );
		}

		public void sending( int current, int total )
		{
			if( event != null )
			{
				event.send( current, total );
			}
		}

		public void receive( int total )
		{
			current = State.RECEIVE;
			receiving( 0, total );
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

		public void success( R response )
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