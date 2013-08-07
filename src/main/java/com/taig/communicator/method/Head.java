package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.request.Read;
import com.taig.communicator.request.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.taig.communicator.method.Method.*;
import static com.taig.communicator.data.Header.Request.ACCEPT_ENCODING;

/**
 * The HEAD method is identical to GET except that the server MUST NOT return a message-body in the response. The
 * meta information contained in the HTTP headers in response to a HEAD request SHOULD be identical to the information
 * sent in response to a GET request. This method can be used for obtaining meta information about the entity implied by
 * the request without transferring the entity-body itself. This method is often used for testing hypertext links for
 * validity, accessibility, and recent modification.
 *
 * The response to a HEAD request MAY be cacheable in the sense that the information contained in the response MAY be
 * used to update a previously cached entity from that resource. If the new field values indicate that the cached entity
 * differs from the current entity (as would be indicated by a change in Content-Length, Content-MD5, ETag or
 * Last-Modified), then the cache MUST treat the cache entry as stale.
 *
 * @see Method#HEAD( java.net.URL, com.taig.communicator.event.Event )
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html</a>
 */
public class Head extends Read<Response, Event<Response>, Void>
{
	public Head( URL url, Event<Response> event )
	{
		super( Type.HEAD, url, event );
		headers.put( ACCEPT_ENCODING, "" );
	}

	@Override
	protected Void read( URL url, InputStream input ) throws IOException
	{
		return null;
	}

	@Override
	protected Response summarize( URL url, int code, String message, Map<String, List<String>> headers, Void body )
	{
		return new Response( url, code, message, headers );
	}
}