package com.taig.communicator.concurrent;

import android.util.Log;
import com.taig.communicator.event.Event;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.request.Request;
import com.taig.communicator.request.Response;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.List;

public abstract class Wrapper<R extends java.lang.Runnable> implements java.lang.Runnable, Cancelable
{
	protected final R runnable;

	public Wrapper( R runnable )
	{
		this.runnable = runnable;
	}

	@SuppressWarnings( "unchecked" )
	public static Wrapper newInstance( java.lang.Runnable runnable, CookieStore store, CookiePolicy policy )
	{
		if( runnable instanceof com.taig.communicator.request.Request )
		{
			return new Request( (com.taig.communicator.request.Request) runnable, store, policy );
		}
		else
		{
			return new Runnable( runnable );
		}
	}

	public static class Runnable extends Wrapper<java.lang.Runnable>
	{
		public Runnable( java.lang.Runnable runnable )
		{
			super( runnable );
		}

		@Override
		public void run()
		{
			runnable.run();
		}

		@Override
		public boolean cancel()
		{
			return false;
		}
	}

	public static class Request<R extends Response, E extends Event<R>> extends Wrapper<com.taig.communicator.request.Request<R, E>>
	{
		private static final String TAG = Request.class.getName();

		protected CookieStore store;

		protected CookiePolicy policy;

		public Request( com.taig.communicator.request.Request<R, E> request, CookieStore store, CookiePolicy policy )
		{
			super( request );
			this.store = store;
			this.policy = policy;
		}

		@Override
		public void run()
		{
			Event<R>.Proxy event = runnable.getEventProxy();

			try
			{
				// Add available cookies to the request.
				if( store != null )
				{
					runnable.addCookie( store );
				}

				// Perform request.
				R response = runnable.request();

				if( event != null )
				{
					event.success( response );
				}

				// Handle cookies.
				List<HttpCookie> cookies = response.getCookies();

				if( cookies != null )
				{
					URI uri = response.getUrl().toURI();

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
				if( event != null )
				{
					event.failure( exception );
				}
			}
		}

		@Override
		public boolean cancel()
		{
			runnable.cancel();
			return true;
		}
	}
}