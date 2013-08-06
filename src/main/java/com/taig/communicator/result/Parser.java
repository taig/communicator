package com.taig.communicator.result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface Parser<T>
{
	public static final Ignore IGNORE = new Ignore();

	public static final Image IMAGE = new Image();

	public static final Text TEXT = new Text();

	public T parse( URL url, InputStream stream ) throws IOException;
}