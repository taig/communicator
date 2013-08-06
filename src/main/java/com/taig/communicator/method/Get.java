package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.io.Updateable;
import com.taig.communicator.request.Read;
import com.taig.communicator.request.Response;
import com.taig.communicator.result.Parser;

import java.io.IOException;
import java.net.URL;

import static com.taig.communicator.method.Method.*;

/**
 * The GET method means retrieve whatever information (in the form of an entity) is identified by the Request-URI. If
 * the Request-URI refers to a data-producing process, it is the produced data which shall be returned as the entity in
 * the response and not the source text of the process, unless that text happens to be the output of the process.
 * <p/>
 * The semantics of the GET method change to a "conditional GET" if the request message includes an If-Modified-Since,
 * If-Unmodified-Since, If-Match, If-None-Match, or If-Range header field. A conditional GET method requests that the
 * entity be transferred only under the circumstances described by the conditional header field(s). The conditional GET
 * method is intended to reduce unnecessary network usage by allowing cached entities to be refreshed without requiring
 * multiple requests or transferring data already held by the client.
 * <p/>
 * The semantics of the GET method change to a "partial GET" if the request message includes a Range header field. A
 * partial GET requests that only part of the entity be transferred, as described in section
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35">14.35</a>. The partial GET method is
 * intended to reduce unnecessary network usage by allowing partially-retrieved entities to be completed without
 * transferring data already held by the client.
 * <p/>
 * The response to a GET request is cacheable if and only if it meets the requirements for HTTP caching described in
 * section 13.
 * <p/>
 * See section <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec15.html#sec15.1.3">15.1.3</a> for security
 * considerations when used for forms.
 *
 * @param <R> The {@link Response} type (created in {@link #summarize(java.net.URL, int, String, java.util.Map, Object)}).
 * @param <E> The {@link Event} type.
 * @param <T> The resource's type as generated by the supplied {@link Parser}.
 * @see Method#GET( com.taig.communicator.result.Parser, java.net.URL, com.taig.communicator.event.Event.Payload )
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html</a>
 */
public abstract class Get<R extends Response, E extends Event<R>, T> extends Read<R, E, T>
{
	private Parser<T> parser;

	public Get( Parser<T> parser, URL url, E event )
	{
		super( Type.GET, url, event );
		this.parser = parser;
	}

	@Override
	protected T read( URL url, Updateable.Input input ) throws IOException
	{
		return parser.parse( url, input );
	}
}