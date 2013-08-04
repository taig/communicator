package com.taig.communicator.data;

import java.net.HttpURLConnection;

public interface Appliable
{
	public void apply( HttpURLConnection connection );
}