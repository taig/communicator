package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class Parser<T>
{
	public abstract T parse( URL url, InputStream stream ) throws IOException;

	public static <T> Parser<T> newInstance( Class<? extends Parser<T>> result )
	{
		try
		{
			return result.getConstructor().newInstance();
		}
		catch( Exception exception )
		{
			throw new RuntimeException( "Could not instantiate default constructor of type '" + result.getName() + "'" );
		}
	}
}