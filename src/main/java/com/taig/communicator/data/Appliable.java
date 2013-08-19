package com.taig.communicator.data;

import java.net.HttpURLConnection;

/**
 * A class that implements this interface may be able to transfer its state into an {@link HttpURLConnection}
 * configuration.
 */
public interface Appliable
{
	/**
	 * Transfer the current state of this object into an {@link HttpURLConnection HttpURLConnection's} configuration.
	 *
	 * @param connection The HttpURLConnection that will be configured.
	 */
	public void apply( HttpURLConnection connection );
}