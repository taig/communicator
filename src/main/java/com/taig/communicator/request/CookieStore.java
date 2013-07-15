package com.taig.communicator.request;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class CookieStore implements java.net.CookieStore
{
	protected static final String TAG = CookieStore.class.getName();

	protected static final String WILDCARD = "*";

	protected SharedPreferences preferences;

	public CookieStore( Context context )
	{
		this( context, "com.taig.communicator.CookieStore" );
	}

	public CookieStore( Context context, String preference )
	{
		this( context.getSharedPreferences( preference, Context.MODE_PRIVATE ) );
	}

	public CookieStore( SharedPreferences preferences )
	{
		this.preferences = preferences;
	}

	@Override
	public void add( URI uri, HttpCookie cookie )
	{
		String host = uri == null ? WILDCARD : uri.getHost();
		Set<String> cookies = preferences.getStringSet( host, new HashSet<String>() );
		cookies.add( cookie.toString() );
	}

	public void add( Response<?> response )
	{
		try
		{
			URI uri = response.getURL().toURI();
			List<HttpCookie> cookies = response.getCookies();

			if( cookies != null )
			{
				for( HttpCookie cookie : cookies )
				{
					add( uri, cookie );
				}
			}
		}
		catch( URISyntaxException exception )
		{
			Log.w( TAG, "The cookies of a Response were dropped because the associated " +
						"URL (" + response.getURL() + ") could not be converted to an URI", exception );
		}
	}

	@Override
	public List<HttpCookie> get( URI uri )
	{
		List<HttpCookie> cookies = new ArrayList<HttpCookie>();
		Set<String> store = preferences.getStringSet( uri.getHost(), null );

		if( store != null )
		{
			for( String cookie : store )
			{
				cookies.addAll( HttpCookie.parse( cookie ) );
			}
		}

		return Collections.unmodifiableList( cookies );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public List<HttpCookie> getCookies()
	{
		List<HttpCookie> cookies = new ArrayList<HttpCookie>();
		Map<String, ?> store = preferences.getAll();

		if( store != null )
		{
			for( Map.Entry<String, ?> entry : store.entrySet() )
			{
				if( entry.getValue() instanceof Collection )
				{
					for( String cookie : (Set<String>) entry.getValue() )
					{
						try
						{
							cookies.addAll( HttpCookie.parse( cookie ) );
						}
						catch( IllegalArgumentException exception )
						{
							preferences.edit().remove( entry.getKey() ).commit();
							Log.w( TAG, "Found and removed illegal entry in CookieStore's SharedPreferences", exception );
						}
					}
				}
			}
		}

		return Collections.unmodifiableList( cookies );
	}

	@Override
	public List<URI> getURIs()
	{
		List<URI> uris = new ArrayList<URI>();
		Map<String, ?> store = preferences.getAll();

		if( store != null )
		{
			for( String host : store.keySet() )
			{
				try
				{
					if( !host.equals( WILDCARD ) )
					{
						uris.add( new URI( null, host, null, null ) );
					}
				}
				catch( URISyntaxException exception )
				{
					preferences.edit().remove( host ).commit();
					Log.w( TAG, "Found and removed illegal entry in CookieStore's SharedPreferences", exception );
				}
			}
		}

		return Collections.unmodifiableList( uris );
	}

	@Override
	public boolean remove( URI uri, HttpCookie cookie )
	{
		String host = uri == null ? WILDCARD : uri.getHost();
		Set<String> cookies = preferences.getStringSet( host, null );

		if( cookies != null )
		{
			if( cookies.remove( cookie.toString() ) )
			{
				return preferences.edit().putStringSet( host, cookies ).commit();
			}
		}

		return false;
	}

	@Override
	public boolean removeAll()
	{
		Map<String, ?> store = preferences.getAll();

		if( store != null && !store.isEmpty() )
		{
			return preferences.edit().clear().commit();
		}

		return false;
	}
}