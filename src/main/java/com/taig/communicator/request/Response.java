package com.taig.communicator.request;

import com.taig.communicator.data.Header;

import java.net.HttpCookie;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Representation of an HTTP response.
 */
public class Response
{
	private final URL url;

	private final int code;

	private final String message;

	private final Header headers;

	/**
	 * Create a {@link Response}.
	 *
	 * @param url     The requested {@link URL}.
	 * @param code    The HTTP response code.
	 * @param message The HTTP response message.
	 * @param headers The response headers.
	 */
	public Response( URL url, int code, String message, Map<String, List<String>> headers )
	{
		this.url = url;
		this.code = code;
		this.message = message;
		this.headers = new Header( headers.size() );

		for( Map.Entry<String, List<String>> header : headers.entrySet() )
		{
			this.headers.put( header.getKey(), header.getValue().toArray() );
		}
	}

	/**
	 * Retrieve the requested {@link URL}.
	 *
	 * @return The requested URL.
	 */
	public URL getUrl()
	{
		return url;
	}

	/**
	 * Retrieve the HTTP response code.
	 *
	 * @return The HTTP response code.
	 */
	public int getCode()
	{
		return code;
	}

	/**
	 * Retrieve the HTTP response message.
	 *
	 * @return The HTTP response message.
	 */
	public String getMessage()
	{
		return message;
	}

	/**
	 * Retrieve the response {@link Header Headers}.
	 *
	 * @return The response Headers.
	 */
	public Header getHeader()
	{
		return headers;
	}

	/**
	 * Retrieve the response {@link Header} with a specific key.
	 *
	 * @param key The key used to look up the Header.
	 * @return The Header matching the given key. May be <code>null</code>.
	 */
	public String[] getHeader( String key )
	{
		return (String[]) headers.get( key );
	}

	/**
	 * Retrieve the response headers' {@link HttpCookie HttpCookies}
	 *
	 * @return The response headers' HttpCookies.
	 */
	public List<HttpCookie> getCookies()
	{
		String[] headers = getHeader( "Set-Cookie" );

		if( headers != null )
		{
			List<HttpCookie> cookies = new ArrayList<HttpCookie>();

			for( String header : headers )
			{
				cookies.addAll( HttpCookie.parse( header ) );
			}

			return cookies;
		}

		return null;
	}

	/**
	 * Retrieve the response header's {@link HttpCookie} with a specific name.
	 *
	 * @param name The name used to look up the HttpCookie.
	 * @return The HttpCookie matching the given name. May be <code>null</code>.
	 */
	public HttpCookie getCookie( String name )
	{
		List<HttpCookie> cookies = getCookies();

		if( cookies != null )
		{
			for( HttpCookie cookie : cookies )
			{
				if( cookie.getName().equals( name ) )
				{
					return cookie;
				}
			}
		}

		return null;
	}

	/**
	 * An extension of the {@link Response} that allows direct access for the parsed response body.
	 *
	 * @param <T> The parsed response body's type.
	 */
	public static class Payload<T> extends Response
	{
		private final T result;

		public Payload( URL url, int code, String message, Map<String, List<String>> headers, T result )
		{
			super( url, code, message, headers );
			this.result = result;
		}

		/**
		 * Retrieve the parsed response body.
		 *
		 * @return The parsed response body.
		 */
		public T getPayload()
		{
			return result;
		}
	}
}