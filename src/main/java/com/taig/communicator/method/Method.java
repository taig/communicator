package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.data.Data;
import com.taig.communicator.request.Request;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Parser;

import java.net.URL;

/**
 * A collection of several factory method to easily instantiate {@link Request Requests}. Using on of these methods is
 * the recommended way to create a Request.
 */
public abstract class Method
{
	/**
	 * The supported HTTP Method types.
	 */
	public enum Type
	{
		/**
		 * @see Delete
		 */
		DELETE,

		/**
		 * @see Get
		 */
		GET,

		/**
		 * @see Head
		 */
		HEAD,

		/**
		 * @see Post
		 */
		POST,

		/**
		 * @see Put
		 */
		PUT
	}

	/**
	 * Create an HTTP GET {@link Request}.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Get}.
	 */
	public static <T> Get<T> GET( Class<? extends Parser<T>> parser, URL url )
	{
		return GET( parser, url, null );
	}

	/**
	 * Create an HTTP GET {@link Request} with {@link Event} callbacks.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Get}.
	 */
	public static <T> Get<T> GET( Class<? extends Parser<T>> parser, URL url, Event.Payload<T> event )
	{
		return GET( createParser( parser ), url, event );
	}

	/**
	 * Create an HTTP GET {@link Request} with {@link Event} callbacks.
	 * <p/>
	 * This method accepts an actual {@link Parser} object (instead of a Parser {@link Class}) for complex use cases
	 * when a simple Parser with default constructor is not sufficient.
	 *
	 * @param parser The Parser used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Get}.
	 */
	public static <T> Get<T> GET( Parser<T> parser, URL url, Event.Payload<T> event )
	{
		return new Get<T>( parser, url, event );
	}

	/**
	 * Create an HTTP DELETE {@link Request}.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Delete}.
	 */
	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url )
	{
		return DELETE( parser, url, null, null );
	}

	/**
	 * Create an HTTP DELETE {@link Request} with {@link Event} callbacks.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Delete}.
	 */
	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url, Event.Payload<T> event )
	{
		return DELETE( parser, url, null, event );
	}

	/**
	 * Create an HTTP DELETE {@link Request} with payload {@link Data}.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Delete}.
	 */
	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url, Data data )
	{
		return DELETE( parser, url, data, null );
	}

	/**
	 * Create an HTTP DELETE {@link Request} with payload {@link Data} and {@link Event} callbacks.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Delete}.
	 */
	public static <T> Delete<T> DELETE( Class<? extends Parser<T>> parser, URL url, Data data, Event.Payload<T> event )
	{
		return DELETE( createParser( parser ), url, data, event );
	}

	/**
	 * Create an HTTP DELETE {@link Request} with payload {@link Data} and {@link Event} callbacks.
	 * <p/>
	 * This method accepts an actual {@link Parser} object (instead of a Parser {@link Class}) for complex use cases
	 * when a simple Parser with default constructor is not sufficient.
	 *
	 * @param parser The Parser used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Delete}.
	 */
	public static <T> Delete<T> DELETE( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		return new Delete<T>( parser, url, data, event );
	}

	/**
	 * Create an HTTP HEAD {@link Request}.
	 *
	 * @param url The resource's {@link URL}.
	 * @return An instance of {@link Head}.
	 */
	public static Head HEAD( URL url )
	{
		return HEAD( url, null );
	}

	/**
	 * Create an HTTP HEAD {@link Request} with {@link Event} callbacks.
	 *
	 * @param url   The resource's {@link URL}.
	 * @param event The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @return An instance of {@link Head}.
	 */
	public static Head HEAD( URL url, Event<Response> event )
	{
		return new Head( url, event );
	}

	/**
	 * Create an HTTP POST {@link Request}.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Post}.
	 */
	public static <T> Post<T> POST( Class<? extends Parser<T>> parser, URL url, Data data )
	{
		return POST( parser, url, data, null );
	}

	/**
	 * Create an HTTP POST {@link Request} with {@link Event} callbacks.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Post}.
	 */
	public static <T> Post<T> POST( Class<? extends Parser<T>> parser, URL url, Data data, Event.Payload<T> event )
	{
		return POST( createParser( parser ), url, data, event );
	}

	/**
	 * Create an HTTP POST {@link Request} with {@link Event} callbacks.
	 * <p/>
	 * This method accepts an actual {@link Parser} object (instead of a Parser {@link Class}) for complex use cases
	 * when a simple Parser with default constructor is not sufficient.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Post}.
	 */
	public static <T> Post<T> POST( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		return new Post<T>( parser, url, data, event );
	}

	/**
	 * Create an HTTP PUT {@link Request}.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Put}.
	 */
	public static <T> Put<T> PUT( Class<? extends Parser<T>> parser, URL url, Data data )
	{
		return PUT( parser, url, data, null );
	}

	/**
	 * Create an HTTP PUT {@link Request}.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Put}.
	 */
	public static <T> Put<T> PUT( Class<? extends Parser<T>> parser, URL url, Data data, Event.Payload<T> event )
	{
		return PUT( createParser( parser ), url, data, event );
	}

	/**
	 * Create an HTTP PUT {@link Request}.
	 * <p/>
	 * This method accepts an actual {@link Parser} object (instead of a Parser {@link Class}) for complex use cases
	 * when a simple Parser with default constructor is not sufficient.
	 *
	 * @param parser The {@link Class} of a {@link Parser} used to evaluate the server's response.
	 * @param url    The resource's {@link URL}.
	 * @param data   The payload Data that will be added to the Request body. May be <code>null</code>.
	 * @param event  The Event callbacks that will be executed during the request. May be <code>null</code>.
	 * @param <T>    The resource's type after successful parsing.
	 * @return An instance of {@link Put}.
	 */
	public static <T> Put<T> PUT( Parser<T> parser, URL url, Data data, Event.Payload<T> event )
	{
		return new Put<T>( parser, url, data, event );
	}

	/**
	 * Create an instance of a {@link Parser} via reflection.
	 *
	 * @param type The class of the Parser to instantiate.
	 * @return The instantiated Parser.
	 * @throws RuntimeException If the given Parser class does not have a default constructor.
	 */
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