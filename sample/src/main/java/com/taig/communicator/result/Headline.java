package com.taig.communicator.result;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Headline extends Result<String>
{
	@Override
	public String process( URL url, InputStream stream ) throws IOException
	{
		return Jsoup.parse( stream, "UTF-8", url.toExternalForm() ).select( "h1" ).first().text();
	}
}