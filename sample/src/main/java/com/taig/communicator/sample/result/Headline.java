package com.taig.communicator.sample.result;

import com.taig.communicator.request.Request;
import com.taig.communicator.result.Parser;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Headline implements Parser<String>
{
	@Override
	public String parse( URL url, InputStream stream ) throws IOException
	{
		return Jsoup.parse( stream, Request.CHARSET, url.toExternalForm() ).select( "h1" ).first().text();
	}
}