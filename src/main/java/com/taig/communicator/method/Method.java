package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.request.Data;
import com.taig.communicator.result.Result;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class Method
{
	public static <T> Get<T> GET( Class<? extends Result<T>> result, String url ) throws MalformedURLException
	{
		return GET( result, url, null );
	}

	public static <T> Get<T> GET( Class<? extends Result<T>> result, String url, Event<T> event ) throws MalformedURLException
	{
		return GET( result, new URL( url ), event );
	}

	public static <T> Get<T> GET( Class<? extends Result<T>> result, URL url )
	{
		return GET( result, url, null );
	}

	public static <T> Get<T> GET( Class<? extends Result<T>> result, URL url, Event<T> event )
	{
		return new Get<T>( Result.newInstance( result ), url, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, String url ) throws MalformedURLException
	{
		return DELETE( result, url, null, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, String url, Event<T> event ) throws MalformedURLException
	{
		return DELETE( result, new URL( url ), null, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, String url, Data data ) throws MalformedURLException
	{
		return DELETE( result, url, data, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, String url, Data data, Event<T> event ) throws MalformedURLException
	{
		return DELETE( result, new URL( url ), data, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, URL url )
	{
		return DELETE( result, url, null, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, URL url, Event<T> event )
	{
		return DELETE( result, url, null, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, URL url, Data data )
	{
		return DELETE( result, url, data, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Result<T>> result, URL url, Data data, Event<T> event )
	{
		return new Delete<T>( Result.newInstance( result ), url, data, event );
	}

	public static Head HEAD( String url ) throws MalformedURLException
	{
		return HEAD( url, null );
	}

	public static Head HEAD( String url, Event<Void> event ) throws MalformedURLException
	{
		return HEAD( new URL( url ), event );
	}

	public static Head HEAD( URL url )
	{
		return HEAD( url, null );
	}

	public static Head HEAD( URL url, Event<Void> event )
	{
		return new Head( url, event );
	}

	public static <T> Post<T> POST( Class<? extends Result<T>> result, String url, Data data ) throws MalformedURLException
	{
		return POST( result, url, data, null );
	}

	public static <T> Post<T> POST( Class<? extends Result<T>> result, String url, Data data, Event<T> event ) throws MalformedURLException
	{
		return POST( result, new URL( url ), data, event );
	}

	public static <T> Post<T> POST( Class<? extends Result<T>> result, URL url, Data data )
	{
		return POST( result, url, data, null );
	}

	public static <T> Post<T> POST( Class<? extends Result<T>> result, URL url, Data data, Event<T> event )
	{
		return new Post<T>( Result.newInstance( result ), url, data, event );
	}

	public static <T> Put<T> PUT( Class<? extends Result<T>> result, String url, Data data ) throws MalformedURLException
	{
		return PUT( result, url, data, null );
	}

	public static <T> Put<T> PUT( Class<? extends Result<T>> result, String url, Data data, Event<T> event ) throws MalformedURLException
	{
		return PUT( result, new URL( url ), data, event );
	}

	public static <T> Put<T> PUT( Class<? extends Result<T>> result, URL url, Data data )
	{
		return PUT( result, url, data, null );
	}

	public static <T> Put<T> PUT( Class<? extends Result<T>> result, URL url, Data data, Event<T> event )
	{
		return new Put<T>( Result.newInstance( result ), url, data, event );
	}
}