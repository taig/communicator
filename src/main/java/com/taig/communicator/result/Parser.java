package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A Parser is used by a {@link com.taig.communicator.request.Request} to translate the server's response into a type
 * of
 * your choice.
 *
 * @param <T> The type that the server's response will be translated to.
 */
public interface Parser<T>
{
	/**
	 * @see Ignore
	 */
	public static final Ignore IGNORE = new Ignore();

	/**
	 * @see Image
	 */
	public static final Image IMAGE = new Image();

	/**
	 * @see Text
	 */
	public static final Text TEXT = new Text();

	/**
	 * Parse the server's response stream.
	 *
	 * @param url    The {@link URL} that has been requested in order to retrieve the incoming response. This parameter
	 *               may be useful to replace relative links in an HTML response into absolute URLs.
	 * @param stream The actual {@link InputStream} that delivers the server's response. You do not have to close this
	 *               stream, the Request object will close the stream when this method finished execution.
	 * @return The parsed result.
	 * @throws IOException If an networking error occurs during interaction with the stream.
	 */
	public T parse( URL url, InputStream stream ) throws IOException;
}