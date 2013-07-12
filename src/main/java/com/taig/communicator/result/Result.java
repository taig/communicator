package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

public abstract class Result<T>
{
	public abstract T process( InputStream stream ) throws IOException;

	public static <T> Result<T> newInstance( Class<? extends Result<T>> result )
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