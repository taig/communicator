package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.data.Data;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Parser;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class Method
{
	public enum Type
	{
		DELETE, GET, HEAD, POST, PUT
	}

	public static <T> Get<T> GET( Class<? extends Parser<T>> parser, String url ) throws MalformedURLException
	{
		return GET( parser, url, null );
	}

	public static <T> Get<T> GET( Class<? extends Parser<T>> parser, String url, Event.Payload<T> event ) throws MalformedURLException
	{
		return GET( parser, new URL( url ), event );
	}

	public static <T> Get<T> GET( Class<? extends Parser<T>> parser, URL url )
	{
		return GET( parser, url, null );
	}

	public static <T> Get<T> GET( Class<? extends Parser<T>> parser, URL url, Event.Payload<T> event )
	{
		return GET( createParser( parser ), url, event );
	}

	public static <T> Get<T> GET( Parser<T> parser, URL url, Event.Payload<T> event )
	{
		return new Get<T>( parser, url, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, String url ) throws MalformedURLException
	{
		return DELETE( parser, url, null, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, String url, Event.Payload<T> event ) throws MalformedURLException
	{
		return DELETE( parser, new URL( url ), null, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, String url, Data data ) throws MalformedURLException
	{
		return DELETE( parser, url, data, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, String url, Data data, Event.Payload<T> event ) throws MalformedURLException
	{
		return DELETE( parser, new URL( url ), data, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url )
	{
		return DELETE( parser, url, null, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url, Event.Payload<T> event )
	{
		return DELETE( parser, url, null, event );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url, Data data )
	{
		return DELETE( parser, url, data, null );
	}

	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url, Data data, Event.Payload<T> event )
	{
		return DELETE( createParser( parser ), url, data, event );
	}

	public static <T> Delete<T> DELETE( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		return new Delete<T>( parser, url, data, event );
	}

	public static Head HEAD( String url ) throws MalformedURLException
	{
		return HEAD( url, null );
	}

	public static Head HEAD( String url, Event<Response> event ) throws MalformedURLException
	{
		return HEAD( new URL( url ), event );
	}

	public static Head HEAD( URL url )
	{
		return HEAD( url, null );
	}

	public static Head HEAD( URL url, Event<Response> event )
	{
		return new Head( url, event );
	}

	public static <T> Post<T> POST( Class<? extends Parser<T>> parser, String url, Data data ) throws MalformedURLException
	{
		return POST( parser, url, data, null );
	}

	public static <T> Post<T> POST( Class<? extends Parser<T>> parser, String url, Data data, Event.Payload<T> event ) throws MalformedURLException
	{
		return POST( parser, new URL( url ), data, event );
	}

	public static <T> Post<T> POST( Class<? extends Parser<T>> parser, URL url, Data data )
	{
		return POST( parser, url, data, null );
	}

	public static <T> Post<T> POST( Class<? extends Parser<T>> parser, URL url, Data data, Event.Payload<T> event )
	{
		return new Post<T>( createParser( parser ), url, data, event );
	}

	public static <T> Post<T> POST( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		return POST( parser, url, data, event );
	}

	public static <T> Put<T> PUT( Class<? extends Parser<T>> parser, String url, Data data ) throws MalformedURLException
	{
		return PUT( parser, url, data, null );
	}

	public static <T> Put<T> PUT( Class<? extends Parser<T>> parser, String url, Data data, Event.Payload<T> event ) throws MalformedURLException
	{
		return PUT( parser, new URL( url ), data, event );
	}

	public static <T> Put<T> PUT( Class<? extends Parser<T>> parser, URL url, Data data )
	{
		return PUT( parser, url, data, null );
	}

	public static <T> Put<T> PUT( Class<? extends Parser<T>> parser, URL url, Data data, Event.Payload<T> event )
	{
		return PUT( createParser( parser ), url, data, event );
	}

	public static <T> Put<T> PUT( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		return new Put<T>( parser, url, data, event );
	}

	protected static <T> Parser<T> createParser( Class<? extends Parser<T>> type )
	{
		try
		{
			return type.getConstructor().newInstance();
		}
		catch( Exception exception )
		{
			throw new RuntimeException( "Could not instantiate default constructor of type '" + type.getName() + "'" );
		}
	}
}