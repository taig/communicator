package com.taig.communicator.request;

import com.taig.communicator.data.Header;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.method.Method;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.List;

import static com.taig.communicator.data.Header.Request.ACCEPT_CHARSET;
import static com.taig.communicator.data.Header.Request.COOKIE;

/**
 * Responsible for complete network interaction, request header specification, response header evaluation and result
 * parsing. Requests also support {@link Event} callbacks that allow to perform arbitrary actions during execution.
 * <p/>
 * This class is based on {@link HttpURLConnection HttpURLConnection's} and vaguely reveals its API.
 *
 * @param <R> The {@link Response Response's} type.
 * @param <E> The {@link Event Event's} type.
 */
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

	/**
	 * Construct a {@link Request}.
	 *
	 * @param method The HTTP {@link Method.Type}.
	 * @param url    The {@link URL} to request.
	 * @param event  The {@link Event} callbacks. May be <code>null</code>.
	 */
	public Request( Method.Type method, URL url, E event )
	{
		this.method = method;
		this.url = url;
		setEvent( event );
		headers.put( ACCEPT_CHARSET, CHARSET );
	}

	/**
	 * Retrieve the {@link Request Request's} HTTP {@link Method.Type}.
	 *
	 * @return The Request's HTTP Method.Type.
	 */
	public Method.Type getMethod()
	{
		return method;
	}

	/**
	 * Retrieve the {@link Request Request's} current {@link State}.
	 *
	 * @return The Request's current State.
	 */
	public State getState()
	{
		return state.current;
	}

	/**
	 * Retrieve the {@link Request Request's} {@link URL}.
	 *
	 * @return The Request's URL.
	 */
	public URL getUrl()
	{
		return url;
	}

	/**
	 * Retrieve the {@link Request Request's} {@link Event.Proxy}.
	 * <p/>
	 * This method is not designated for external usage. Be careful.
	 *
	 * @return The Request's Event.Proxy. May be <code>null</code>.
	 */
	public Event<R>.Proxy getEventProxy()
	{
		return event;
	}

	/**
	 * Specify the {@link Request Request's} {@link Event} callbacks.
	 *
	 * @param event The Request's Event callbacks. May be <code>null</code>.
	 * @return <code>this</code>
	 */
	public Request<R, E> setEvent( E event )
	{
		this.event = event == null ? null : event.getProxy();
		return this;
	}

	/**
	 * Whether the {@link Request} has been cancelled or not.
	 *
	 * @return <code>true</code> if the Request has been cancelled, <code>false</code> otherwise.
	 */
	public boolean isCancelled()
	{
		return cancelled;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean cancel()
	{
		this.cancelled = true;
		return true;
	}

	/**
	 * Whether the {@link Request} is currently being executed or not.
	 * <p/>
	 * The Request object is considered busy, when is {@link #getState() State} is either {@link State#START}, {@link
	 * State#CONNECT}, {@link State#SEND} or {@link State#RECEIVE}.
	 *
	 * @return <code>true</code> if the Request is currently being executed, <code>false</code> otherwise.
	 */
	public boolean isBusy()
	{
		State state = getState();
		return state == State.START || state == State.CONNECT || state == State.SEND || state == State.RECEIVE;
	}

	/**
	 * Specify whether to allow user interaction.
	 *
	 * @param allow <code>true</code> to allow user interaction, <code>false</code> otherwise. Default is set to
	 *              <code>false</code>.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setAllowUserInteraction(boolean)
	 */
	public Request<R, E> allowUserInteraction( boolean allow )
	{
		this.userInteraction = allow;
		return this;
	}

	/**
	 * Specify the <i>if modified since</i> value.
	 *
	 * @param modifiedSince The <i>if modified since</i> value. Default is undefined.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setIfModifiedSince(long)
	 */
	public Request<R, E> ifModifiedSince( int modifiedSince )
	{
		this.modifiedSince = modifiedSince;
		return this;
	}

	/**
	 * Specify whether to follow redirects.
	 *
	 * @param follow <code>true</code> to follow redirects, <code>false</code> otherwise. Default is set to
	 *               <code>false</code>.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setFollowRedirects(boolean)
	 */
	public Request<R, E> followRedirects( boolean follow )
	{
		this.redirect = follow;
		return this;
	}

	/**
	 * Specify the buffer size for the chunked streaming mode.
	 *
	 * @param chunkLength The buffer size for the chunked streaming mode. Default is undefined.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setChunkedStreamingMode(int)
	 */
	public Request<R, E> streamChunks( int chunkLength )
	{
		this.chunkLength = chunkLength;
		return this;
	}

	/**
	 * Specify the content length for the fixed length streaming mode.
	 *
	 * @param contentLength The content length for the fixed length streaming mode. Default is undefined.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setFixedLengthStreamingMode(int)
	 */
	public Request<R, E> streamFixedLength( int contentLength )
	{
		this.contentLength = contentLength;
		return this;
	}

	/**
	 * Specify the time it takes before a connect attempt aborts.
	 *
	 * @param timeout The time it takes before a connect attempt aborts (in milliseconds). Default is set to
	 *                <code>0</code> as <i>unlimited</i>.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setConnectTimeout(int)
	 */
	public Request<R, E> timeoutConnect( int timeout )
	{
		this.connectTimeout = timeout;
		return this;
	}

	/**
	 * Specify the time it takes before a download attempt aborts.
	 *
	 * @param timeout The time it takes before a download attempt aborts (in milliseconds). Default is set to
	 *                <code>0</code> as <i>unlimited</i>.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setReadTimeout(int)
	 */
	public Request<R, E> timeoutRead( int timeout )
	{
		this.readTimeout = timeout;
		return this;
	}

	/**
	 * Specify whether to use the cache or not.
	 *
	 * @param use <code>true</code> to enable caching, <code>false</code> to disable. Default is set to
	 *            <code>false</code>.
	 * @return <code>this</code>
	 * @see HttpURLConnection#setUseCaches(boolean)
	 */
	public Request<R, E> useCache( boolean use )
	{
		this.cache = use;
		return this;
	}

	/**
	 * Retrieve the {@link Request Request's} {@link Header Headers}.
	 *
	 * @return The Request's Headers.
	 */
	public Header getHeader()
	{
		return headers;
	}

	/**
	 * Add {@link Header} values to the {@link Request}, overriding Headers with the same <code>key</code>.
	 *
	 * @param key    The Header's key (such as <code>Accept-Encoding</code>).
	 * @param values The Header's values.
	 * @return <code>this</code>
	 * @see Header#put(String, Object...)
	 * @see Header.Request
	 */
	public Request<R, E> putHeader( String key, Object... values )
	{
		headers.put( key, values );
		return this;
	}

	/**
	 * Add {@link Header} values to the {@link Request}, appending to Headers with the same <code>key</code>.
	 *
	 * @param key    The Header's key (such as <code>Accept-Encoding</code>).
	 * @param values The Header's values.
	 * @return <code>this</code>
	 * @see Header#add(String, Object...)
	 * @see Header.Request
	 */
	public Request<R, E> addHeader( String key, Object... values )
	{
		headers.add( key, values );
		return this;
	}

	/**
	 * Specify the {@link Request Request's} {@link HttpCookie}-{@link Header}.
	 *
	 * @param cookies The Request's HttpCookie-Header.
	 * @return <code>this</code>
	 * @see Header.Request#COOKIE
	 */
	public Request<R, E> putCookie( HttpCookie... cookies )
	{
		return putHeader( COOKIE, (Object[]) cookies );
	}

	/**
	 * Specify the {@link Request Request's} {@link HttpCookie}-{@link Header}.
	 *
	 * @param response A {@link Response} used to retrieve its HttpCookies.
	 * @return <code>this</code>
	 * @see Header.Request#COOKIE
	 * @see Response#getCookies()
	 */
	public Request<R, E> putCookie( Response response )
	{
		List<HttpCookie> cookies = response.getCookies();
		return putCookie( cookies.toArray( new HttpCookie[cookies.size()] ) );
	}

	/**
	 * Specify the {@link Request Request's} {@link HttpCookie}-{@link Header}.
	 *
	 * @param store A {@link CookieStore} used to retrieve its HttpCookies that match this Request's {@link URL}.
	 * @return <code>this</code>
	 * @see Header.Request#COOKIE
	 */
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

	/**
	 * Append to the {@link Request Request's} {@link HttpCookie}-{@link Header}.
	 *
	 * @param cookies The Request's HttpCookie-Header.
	 * @return <code>this</code>
	 * @see Header.Request#COOKIE
	 */
	public Request<R, E> addCookie( HttpCookie... cookies )
	{
		return addHeader( COOKIE, (Object[]) cookies );
	}

	/**
	 * Append to the {@link Request Request's} {@link HttpCookie}-{@link Header}.
	 *
	 * @param response A {@link Response} used to retrieve its HttpCookies.
	 * @return <code>this</code>
	 * @see Header.Request#COOKIE
	 * @see Response#getCookies()
	 */
	public Request<R, E> addCookie( Response response )
	{
		List<HttpCookie> cookies = response.getCookies();
		return addCookie( cookies.toArray( new HttpCookie[cookies.size()] ) );
	}

	/**
	 * Append to the {@link Request Request's} {@link HttpCookie}-{@link Header}.
	 *
	 * @param store A {@link CookieStore} used to retrieve its HttpCookies that match this Request's {@link URL}.
	 * @return <code>this</code>
	 * @see Header.Request#COOKIE
	 */
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

	/**
	 * Create an {@link HttpURLConnection} and apply the available settings and {@link Header Headers}.
	 * <p/>
	 * This method does not yet truly open a connection (in the manner of {@link HttpURLConnection#connect()}.
	 *
	 * @return The configured HttpURLConnection.
	 * @throws IOException As the HttpURLConnection API specifies.
	 * @see URL#openConnection()
	 */
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
	 * Execute this {@link Request}.
	 * <p/>
	 * If an {@link Event} is specified, its finish callbacks {@link Event#onSuccess(Response)}, {@link
	 * Event#onFailure(Throwable)}, {@link Event#onSuccess(Response)} and the according {@link Event#onEvent(State)}
	 * calls won't be executed.
	 *
	 * @return The {@link Response} object that keeps the server's response meta data as well as the payload.
	 * @throws IOException As the HttpURLConnection API specifies.
	 * @see #run()
	 */
	public R request() throws IOException
	{
		HttpURLConnection connection = null;

		try
		{
			state.start();
			connection = connect();
			connection.connect();
			state.connect();

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

	/**
	 * Execute this {@link Request}.
	 *
	 * @see #request()
	 */
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

	/**
	 * Send request data, retrieve and parse the response and create a {@link Response} object.
	 *
	 * @param connection The {@link HttpURLConnection} used for client-/server-communication.
	 * @return The created Response object.
	 * @throws IOException As the HttpURLConnection API specifies.
	 */
	protected abstract R talk( HttpURLConnection connection ) throws IOException;

	/**
	 * Internal helper class to manage the current {@link State} and triggering of the {@link Event.Proxy}.
	 */
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

		public void connect()
		{
			current = State.CONNECT;

			if( event != null )
			{
				event.connect();
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

		public void sending( int current, long total )
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