package com.taig.communicator.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

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
		Collection<HttpCookie> cookies = retrieve( host );

		if( cookies.add( cookie ) )
		{
			persist( host, cookies );
		}
	}

	@Override
	public List<HttpCookie> get( URI uri )
	{
		Set<HttpCookie> cookies = new HashSet<HttpCookie>();

		if( uri != null )
		{
			cookies.addAll( retrieve( uri.getHost() ) );
		}

		cookies.addAll( retrieve( WILDCARD ) );

		return Collections.unmodifiableList( new ArrayList<HttpCookie>( cookies ) );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public List<HttpCookie> getCookies()
	{
		Set<HttpCookie> cookies = new HashSet<HttpCookie>();
		Map<String, ?> store = preferences.getAll();

		if( store != null )
		{
			for( Map.Entry<String, ?> entry : store.entrySet() )
			{
				cookies.addAll( retrieve( entry.getKey() ) );
			}
		}

		return Collections.unmodifiableList( new ArrayList<HttpCookie>( cookies ) );
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
		Collection<HttpCookie> cookies = retrieve( host );
		return cookies.remove( cookie ) && persist( host, cookies );
	}

	@Override
	public boolean removeAll()
	{
		Map<String, ?> store = preferences.getAll();
		return store != null && !store.isEmpty() && preferences.edit().clear().commit();
	}

	private boolean persist( String key, Collection<HttpCookie> cookies )
	{
		return preferences.edit().putString( key, new JSONArray( cookies ).toString() ).commit();
	}

	private Collection<HttpCookie> retrieve( String key )
	{
		Set<HttpCookie> cookies = new HashSet<HttpCookie>();

		try
		{
			JSONArray json = new JSONArray( preferences.getString( key, "[]" ) );

			for( int i = 0; i < json.length(); i++ )
			{
				cookies.addAll( HttpCookie.parse( json.getString( i ) ) );
			}
		}
		catch( JSONException exception )
		{
			Log.w( TAG, "Found and removed illegal entry in CookieStore's SharedPreferences", exception );
			preferences.edit().remove( key ).commit();
		}

		return cookies;
	}
}