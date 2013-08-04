package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.request.Request;
import com.taig.communicator.request.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.taig.communicator.method.Method.*;
import static com.taig.communicator.data.Header.Request.ACCEPT_ENCODING;

public class Head extends Request<Response, Event<Response>>
{
	public Head( URL url, Event<Response> event )
	{
		super( Type.HEAD, url, event );
		headers.put( ACCEPT_ENCODING, "" );
	}

	@Override
	protected Response talk( HttpURLConnection connection ) throws IOException
	{
		return new Response(
			url,
			connection.getResponseCode(),
			connection.getResponseMessage(),
			connection.getHeaderFields() );
	}
}