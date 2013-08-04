package com.taig.communicator.concurrent;

import android.util.Log;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.request.Request;
import com.taig.communicator.request.Response;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class Communicator implements Executor, Cancelable
{
	protected static final String TAG = Communicator.class.getName();

	protected QueuedPool<Request> pool;

	protected Thread[] threads;

	protected boolean closed = false;

	protected CookiePolicy policy = CookiePolicy.ACCEPT_NONE;

	protected CookieStore store;

	public Communicator( int connections )
	{
		if( connections < 1 )
		{
			throw new IllegalArgumentException( "At least 1 connection has to be granted" );
		}

		this.pool = new QueuedPool<Request>( connections );
		this.threads = new Thread[connections];

		for( int i = 0; i < connections; i++ )
		{
			threads[i] = new Thread();
			threads[i].start();
		}
	}

	/**
	 * Remove all queued {@link Request Requests} but finish all active Requests.
	 *
	 * @see #close()
	 */
	public void stop()
	{
		pool.clear();
	}

	/**
	 * Remove all queued {@link Request Requests} and cancel all active Requests.
	 *
	 * @see #closeNow()
	 */
	@Override
	public void cancel()
	{
		stop();

		for( Request request : pool.getPool() )
		{
			request.cancel();
		}
	}

	/**
	 * Remove all queued {@link Request Requests} but finish all active Requests.
	 * <p/>
	 * This method terminates all active Threads after they finished active Requests. It is not possible to execute new
	 * Requests after this call.
	 *
	 * @see #stop()
	 */
	public void close()
	{
		closed = true;
		stop();
	}

	/**
	 * Remove all queued {@link Request Requests} and cancel all active Requests.
	 * <p/>
	 * This method interrupts all active Threads immediately. It is not possible to execute new Requests after this
	 * call.
	 *
	 * @see #cancel()
	 */
	public void closeNow()
	{
		close();
		cancel();

		for( Thread thread : threads )
		{
			thread.cancel();
		}
	}

	public boolean isClosed()
	{
		return closed;
	}

	public boolean isTerminated()
	{
		for( Thread thread : threads )
		{
			if( thread.isAlive() )
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Enable Communicator's cookie management. All accepted cookies will be passed along with following requests (if
	 * the hosts matches).
	 *
	 * @param store
	 * @param policy
	 * @throws IllegalArgumentException If the supplied CookiePolicy is null.
	 */
	public void accept( CookieStore store, CookiePolicy policy )
	{
		if( policy == null )
		{
			throw new IllegalArgumentException( "CookiePolicy may not be null" );
		}

		this.store = store;
		this.policy = policy;
	}

	@Override
	public void execute( Runnable runnable )
	{
		if( runnable instanceof Request )
		{
			request( (Request) runnable );
		}
		else
		{
			throw new IllegalArgumentException( "Please provide a " + Request.class.getName() + " object" );
		}
	}

	public void request( Request request )
	{
		request( request, false );
	}

	public void request( Request request, boolean skipQueue )
	{
		if( isClosed() )
		{
			throw new RejectedExecutionException( "Communicator has already been closed" );
		}

		pool.add( request, skipQueue );
	}

	protected class Thread extends java.lang.Thread implements Cancelable
	{
		protected Request request;

		@Override
		@SuppressWarnings( "unchecked" )
		public void run()
		{
			try
			{
				while( !closed )
				{
					// Wait for pool access.
					request = pool.promote();

					try
					{
						// Add available cookies to the request.
						if( store != null )
						{
							request.addCookie( store );
						}

						// Perform request.
						Response response = request.request();
						request.getEventProxy().success( response );

						// Handle cookies.
						List<HttpCookie> cookies = response.getCookies();

						if( cookies != null )
						{
							URI uri = request.getUrl().toURI();

							for( HttpCookie cookie : cookies )
							{
								if( policy.shouldAccept( uri, cookie ) )
								{
									store.add( uri, cookie );
								}
							}
						}
					}
					catch( URISyntaxException exception )
					{
						Log.w( TAG, "The cookies of a Response were dropped because the associated URL could not be " +
							"converted to an URI", exception );
					}
					catch( InterruptedIOException exception )
					{
						// Don't fail/finish cancelled requests.
					}
					catch( IOException exception )
					{
						request.getEventProxy().failure( exception );
					}
					finally
					{
						// Leave pool.
						pool.demote( request );
						request = null;
					}
				}
			}
			catch( InterruptedException exception )
			{
				if( request != null )
				{
					request.cancel();
					pool.demote( request );
				}
			}
		}

		@Override
		public void cancel()
		{
			if( request == null )
			{
				interrupt();
			}
		}
	}
}