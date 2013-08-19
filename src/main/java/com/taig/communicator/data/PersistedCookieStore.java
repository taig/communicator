package com.taig.communicator.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * An implementation of {@link CookieStore} that persists all stored cookie via Android's {@link SharedPreferences}.
 */
public class PersistedCookieStore implements CookieStore
{
	private static final String TAG = PersistedCookieStore.class.getName();

	private static final String WILDCARD = "*";

	private SharedPreferences preferences;

	/**
	 * Construct a {@link PersistedCookieStore} with a default value for its {@link SharedPreferences} name and the
	 * operating mode set to {@link Context#MODE_PRIVATE}.
	 *
	 * @param context The {@link Context}.
	 */
	public PersistedCookieStore( Context context )
	{
		this( context, "com.taig.communicator.PersistedCookieStore", Context.MODE_PRIVATE );
	}

	/**
	 * Construct a {@link PersistedCookieStore}.
	 *
	 * @param context    The {@link Context}.
	 * @param preference The name of the {@link SharedPreferences}.
	 * @param mode       The operation mode of the SharedPreferences.
	 */
	public PersistedCookieStore( Context context, String preference, int mode )
	{
		this( context.getSharedPreferences( preference, mode ) );
	}

	/**
	 * Construct a {@link PersistedCookieStore}.
	 *
	 * @param preferences The {@link SharedPreferences}.
	 */
	public PersistedCookieStore( SharedPreferences preferences )
	{
		this.preferences = preferences;
	}

	@Override
	public void add( URI uri, HttpCookie cookie )
	{
		String host = uri == null ? WILDCARD : uri.getHost();
		Set<String> cookies = preferences.getStringSet( host, new HashSet<String>() );
		cookies.add( cookie.toString() );
		preferences.edit().putStringSet( host, cookies ).commit();
	}

	@Override
	public List<HttpCookie> get( URI uri )
	{
		Set<String> store = preferences.getStringSet( uri.getHost(), new HashSet<String>() );
		store.addAll( preferences.getStringSet( WILDCARD, new HashSet<String>() ) );
		List<HttpCookie> cookies = new ArrayList<HttpCookie>();

		for( String cookie : store )
		{
			cookies.addAll( HttpCookie.parse( cookie ) );
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
		return store != null && !store.isEmpty() && preferences.edit().clear().commit();
	}
}