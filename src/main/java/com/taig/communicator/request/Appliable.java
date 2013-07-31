package com.taig.communicator.request;

import java.net.HttpURLConnection;

public interface Appliable
{
	public void apply( HttpURLConnection connection );
}