package com.taig.communicator.concurrent;

import android.util.Log;
import com.taig.communicator.event.Event;
import com.taig.communicator.io.Cancelable;
import com.taig.communicator.io.CancelledIOException;
import com.taig.communicator.request.Response;

import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * A wrapper class that aims to provide a consistent API for {@link java.lang.Runnable Runnable} and {@link
 * com.taig.communicator.request.Request Request} objects designed to be used by {@link Communicator}.
 *
 * @param <R>
 */
public abstract class Wrapper<R extends java.lang.Runnable> implements java.lang.Runnable, Cancelable
{
	protected final R runnable;

	/**
	 * Construct a {@link Wrapper}.
	 *
	 * @param runnable The {@link java.lang.Runnable Runnable} to wrap.
	 */
	public Wrapper( R runnable )
	{
		this.runnable = runnable;
	}

	/**
	 * Construct a {@link Wrapper}.
	 * <p/>
	 * If the supplied {@link java.lang.Runnable Runnable} is an instance of {@link
	 * com.taig.communicator.request.Request Request}, this method will return a {@link Wrapper.Request} object,
	 * otherwise a simple {@link Wrapper.Runnable} object.
	 *
	 * @param runnable The Runnable to wrap.
	 * @param store    The {@link CookieStore} to apply if the Runnable is a Request. May be <code>null</code>.
	 * @param policy   The {@link CookiePolicy} to apply if the Runnable is a Request.
	 * @return
	 */
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

	/**
	 * A {@link Wrapper} class for {@link java.lang.Runnable Runnable} objects.
	 */
	public static class Runnable extends Wrapper<java.lang.Runnable>
	{
		/**
		 * Construct a {@link java.lang.Runnable Runnable} {@link Wrapper}.
		 *
		 * @param runnable The Runnable to wrap.
		 */
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

	/**
	 * A {@link Wrapper} class for {@link com.taig.communicator.request.Request Request} objects.
	 *
	 * @param <R> The Request's {@link Response} type.
	 * @param <E> The Request's {@link Event} type.
	 */
	public static class Request<R extends Response, E extends Event<R>> extends Wrapper<com.taig.communicator.request.Request<R, E>>
	{
		private static final String TAG = Request.class.getName();

		private CookieStore store;

		private CookiePolicy policy;

		/**
		 * Construct a {@link com.taig.communicator.request.Request Request} {@link Wrapper}.
		 *
		 * @param request The Request object to wrap.
		 * @param store   The {@link CookieStore} to apply if the Runnable is a Request. May be <code>null</code>.
		 * @param policy  The {@link CookiePolicy} to apply if the Runnable is a Request.
		 */
		public Request( com.taig.communicator.request.Request<R, E> request, CookieStore store, CookiePolicy policy )
		{
			super( request );
			this.store = store;
			this.policy = policy;
		}

		/**
		 * Retrieve the {@link Request Wrapper's} {@link CookieStore}. May be <code>null</code>.
		 *
		 * @return The Wrapper's CookieStore.
		 */
		public CookieStore getCookieStore()
		{
			return store;
		}

		/**
		 * Specify the {@link Request Wrapper's} {@link CookieStore}.
		 *
		 * @param store The Wrapper's CookieStore.
		 */
		public void setCookieStore( CookieStore store )
		{
			this.store = store;
		}

		/**
		 * Retrieve the {@link Request Wrapper's} {@link CookiePolicy}.
		 *
		 * @return The Wrapper's CookiePolicy.
		 */
		public CookiePolicy getCookiePolicy()
		{
			return policy;
		}

		/**
		 * Specify the {@link Request Wrapper's} {@link CookiePolicy}.
		 *
		 * @param policy The Wrapper's CookiePolicy.
		 */
		public void setCookiePolicy( CookiePolicy policy )
		{
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
			catch( CancelledIOException exception )
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