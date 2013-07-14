package com.taig.communicator.method;

import com.taig.communicator.event.Event;
import com.taig.communicator.request.Request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Head extends Request<Void>
{
	public Head( URL url, Event<Void> event )
	{
		super( "HEAD", url, event );
	}

	@Override
	public HttpURLConnection connect() throws IOException
	{
		HttpURLConnection connection = super.connect();
		connection.setRequestProperty( "Accept-Encoding", "" );
		return connection;
	}

	@Override
	protected void send( HttpURLConnection connection ) throws IOException {}

	@Override
	protected Void receive( HttpURLConnection connection ) throws IOException
	{
		return null;
	}
}