package com.taig.communicator.request;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Parameter extends HashMap<String, Object>
{
	public Parameter()
	{
		super();
	}

	public Parameter( int capacity )
	{
		super( capacity );
	}

	public Parameter( int capacity, float loadFactor )
	{
		super( capacity, loadFactor );
	}

	public Parameter( Map<? extends String, ?> parameters )
	{
		super( parameters );
	}

	public String mkString( String charset )
	{
		try
		{
			StringBuilder builder = new StringBuilder();

			for( Iterator<Map.Entry<String, Object>> iterator = entrySet().iterator(); iterator.hasNext(); )
			{
				Map.Entry<String, Object> parameter = iterator.next();

				builder
					.append( parameter.getKey() )
					.append( "=" )
					.append( URLEncoder.encode( parameter.getValue().toString(), charset ) )
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