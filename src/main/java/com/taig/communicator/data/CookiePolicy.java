package com.taig.communicator.data;

import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class CookiePolicy implements java.net.CookiePolicy
{
	private static final String WILDCARD = "*";

	private Map<String, String[]> rules = new HashMap<String, String[]>();

	/**
	 * Create a new {@link CookiePolicy} that only permits certain hosts (extracted from the supplied {@link URI}), but
	 * all cookie names.
	 *
	 * @param uris The permitted URIs.
	 * @throws IllegalArgumentException If the supplied URIs are <code>null</code> or empty.
	 */
	public CookiePolicy( URI... uris )
	{
		if( uris == null || uris.length == 0 )
		{
			throw new IllegalArgumentException( "URIs are null or empty, use CookiePolicy.ACCEPT_NONE" );
		}

		for( URI uri : uris )
		{
			this.rules.put( uri.getHost(), null );
		}
	}

	/**
	 * Create a new {@link CookiePolicy} that only permits certain cookie names, but all hosts.
	 *
	 * @param names The permitted cookie names.
	 * @throws IllegalArgumentException If the supplied names are <code>null</code> or empty.
	 */
	public CookiePolicy( String... names )
	{
		if( names == null || names.length == 0 )
		{
			throw new IllegalArgumentException( "Names are null or empty, use CookiePolicy.ACCEPT_NONE" );
		}

		rules.put( WILDCARD, names );
	}

	/**
	 * Create a new {@link CookiePolicy} that only permits certain host / cookie name combinations.
	 *
	 * @param rules A {@link Map} that lists accepted hosts in its keys and the accordingly accepted cookie names as
	 *              values. A <code>null</code> {@link URI} accepts all cookies that match the given cookie names
	 *              despite their host. A <code>null</code> cookie key value accepts all cookies of a given host
	 *              despite their names.
	 * @throws IllegalArgumentException If the supplied argument is <code>null</code>, empty or has an entry where key
	 *                                  and value are <code>null</code>.
	 */
	public CookiePolicy( Map<URI, String[]> rules )
	{
		if( rules == null || rules.isEmpty() )
		{
			throw new IllegalArgumentException( "Rules are null or empty, use CookiePolicy.ACCEPT_NONE" );
		}

		for( Map.Entry<URI, String[]> rule : rules.entrySet() )
		{
			if( rule.getKey() == null && rule.getValue() == null )
			{
				throw new IllegalArgumentException( "Key and value are null, use CookiePolicy.ACCEPT_ALL" );
			}
			else if( rule.getKey() == null )
			{
				this.rules.put( WILDCARD, rule.getValue() );
			}
			else
			{
				this.rules.put( rule.getKey().getHost(), rule.getValue() );
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean shouldAccept( URI uri, HttpCookie cookie )
	{
		String[] keys = rules.get( uri.getHost() );

		if( keys == null )
		{
			return true;
		}
		else
		{
			for( String key : keys )
			{
				if( cookie.getName().equals( key ) )
				{
					return true;
				}
			}
		}

		return false;
	}
}