package com.taig.communicator.request;

import com.taig.communicator.io.CountableInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class Data extends CountableInputStream
{
	protected ContentType contentType;

	public Data( InputStream stream )
	{
		this( stream, -1 );
	}

	public Data( InputStream stream, int length )
	{
		this( stream, length, ContentType.MULTI );
	}

	public Data( InputStream stream, int length, ContentType contentType )
	{
		super( stream, length );
		this.contentType = contentType;
	}

	public ContentType getContentType()
	{
		return contentType;
	}

	public static Data from( Map<String, String> parameters )
	{
		try
		{
			StringBuilder builder = new StringBuilder();

			for( Map.Entry<String, String> parameter : parameters.entrySet() )
			{
				builder
					.append( parameter.getKey() )
					.append( "=" )
					.append( URLEncoder.encode( parameter.getValue(), "UTF-8" ) )
					.append( "&" );
			}

			if( builder.length() > 0 )
			{
				builder.deleteCharAt( builder.length() - 1 );
			}

			String query = builder.toString();

			return new Data( new ByteArrayInputStream( query.getBytes() ), query.length(), ContentType.FORM );
		}
		catch( UnsupportedEncodingException exception )
		{
			throw new RuntimeException( exception.getMessage() );
		}
	}

	public enum ContentType
	{
		FORM( "application/x-www-form-urlencoded" ), MULTI( "multipart/form-data" );

		private String content;

		private ContentType( String content )
		{
			this.content = content;
		}

		@Override
		public String toString()
		{
			return content;
		}
	}
}