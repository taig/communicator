package com.taig.communicator.request;

import com.taig.communicator.io.CountableInputStream;

import java.io.InputStream;

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
