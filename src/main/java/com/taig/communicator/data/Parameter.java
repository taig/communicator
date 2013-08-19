package com.taig.communicator.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link Map Map&lt;String, Object&gt;} that acts as container for parameters to send along with HTTP requests.
 */
public class Parameter extends HashMap<String, Object>
{
	/**
	 * {@inheritDoc}
	 */
	public Parameter()
	{
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public Parameter( int capacity )
	{
		super( capacity );
	}

	/**
	 * {@inheritDoc}
	 */
	public Parameter( int capacity, float loadFactor )
	{
		super( capacity, loadFactor );
	}

	/**
	 * {@inheritDoc}
	 */
	public Parameter( Map<? extends String, ?> parameters )
	{
		super( parameters );
	}

	/**
	 * Construct a {@link Parameter} object with one predefined key/value pair.
	 *
	 * @param key   The Parameter's key.
	 * @param value The Parameter's values.
	 */
	public Parameter( String key, Object value )
	{
		super( 1 );
		put( key, value );
	}

	/**
	 * Create an URL encoded String representation of the {@link Parameter}.
	 *
	 * @param charset The charset to apply via {@link URLEncoder#encode(String, String)}. May be <code>null</code>.
	 * @return The URL encoded String representation of the Parameter.
	 */
	public String mkString( String charset )
	{
		try
		{
			StringBuilder builder = new StringBuilder();

			for( Iterator<Map.Entry<String, Object>> iterator = entrySet().iterator(); iterator.hasNext(); )
			{
				Map.Entry<String, Object> parameter = iterator.next();
				String value = parameter.getValue().toString();

				builder
					.append( parameter.getKey() )
					.append( "=" )
					.append( charset == null ? value : URLEncoder.encode( value, charset ) )
					.append( iterator.hasNext() ? "&" : "" );
			}

			return builder.toString();
		}
		catch( UnsupportedEncodingException exception )
		{
			throw new RuntimeException( exception.getMessage() );
		}
	}
}