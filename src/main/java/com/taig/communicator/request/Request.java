package com.taig.communicator.request;

import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.method.Method;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.List;

import static com.taig.communicator.request.Header.Request.ACCEPT_CHARSET;
import static com.taig.communicator.request.Header.Request.COOKIE;

public abstract class Request<R extends Response, E extends Event<R>> implements Cancelable, Runnable
{
	public static final String CHARSET = "UTF-8";

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

	protected Header headers = new Header();

	public Request( Method.Type method, URL url, E event )
	{
		this.method = method;
		this.url = url;
		setEvent( event );
		headers.put( ACCEPT_CHARSET, CHARSET );
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

	public Header getHeader()
	{
		return headers;
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

	public Request<R, E> putHeader( String key, Object... values )
	{
		headers.put( key, values );
		return this;
	}

	public Request<R, E> addHeader( String key, Object... values )
	{
		headers.add( key, values );
		return this;
	}

	public Request<R, E> putCookie( HttpCookie... cookies )
	{
		return putHeader( COOKIE, (Object[]) cookies );
	}

	public Request<R, E> putCookie( Response response )
	{
		List<HttpCookie> cookies = response.getCookies();
		return putCookie( cookies.toArray( new HttpCookie[cookies.size()] ) );
	}

	public Request<R, E> putCookie( CookieStore store )
	{
		try
		{
			List<HttpCookie> cookies = store.get( url.toURI() );
			return addCookie( cookies.toArray( new HttpCookie[cookies.size()] ) );
		}
		catch( URISyntaxException exception )
		{
			throw new RuntimeException( "Could not convert request URL '" + url + "' to an URI" );
		}
	}

	public Request<R, E> addCookie( HttpCookie... cookies )
	{
		return addHeader( COOKIE, (Object[]) cookies );
	}

	public Request<R, E> addCookie( Response response )
	{
		List<HttpCookie> cookies = response.getCookies();
		return addCookie( cookies.toArray( new HttpCookie[cookies.size()] ) );
	}

	public Request<R, E> addCookie( CookieStore store )
	{
		try
		{
			List<HttpCookie> cookies = store.get( url.toURI() );
			return addCookie( cookies.toArray( new HttpCookie[cookies.size()] ) );
		}
		catch( URISyntaxException exception )
		{
			throw new RuntimeException( "Could not convert request URL '" + url + "' to an URI" );
		}
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

		headers.apply( connection );
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

		public void send( long total )
		{
			current = State.SEND;
			sending( 0, total );
		}

		public void sending( int current, long total )
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

		public void receiving( int current, long total )
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