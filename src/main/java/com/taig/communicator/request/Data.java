package com.taig.communicator.request;

import com.taig.communicator.io.Countable;

import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Map;

public class Data<C extends Data.ContentType> extends Countable.Stream.Input
{
	protected C contentType;

	public Data( InputStream stream, int length, C contentType )
	{
		super( stream, length );
		this.contentType = contentType;
	}

	public C getContentType()
	{
		return contentType;
	}

	public static Data<ContentType.Multipart> from( InputStream stream )
	{
		return from( stream, -1 );
	}

	public static Data<ContentType.Multipart> from( InputStream stream, int length )
	{
		return new Data<ContentType.Multipart>( stream, length, ContentType.MULTIPART );
	}

	public static Data<ContentType.Multipart> from( File file ) throws FileNotFoundException
	{
		return from( new FileInputStream( file ), new BigDecimal( file.length() ).intValueExact() );
	}

	public static Data<ContentType> from( Map<String, String> parameters )
	{
		try
		{
			StringBuilder builder = new StringBuilder();

			for( Map.Entry<String, String> parameter : parameters.entrySet() )
			{
				builder
					.append( parameter.getKey() )
					.append( "=" )
					.append( URLEncoder.encode( parameter.getValue(), Request.CHARSET ) )
					.append( "&" );
			}

			if( builder.length() > 0 )
			{
				builder.deleteCharAt( builder.length() - 1 );
			}

			ByteArrayInputStream stream = new ByteArrayInputStream( builder.toString().getBytes() );
			return new Data<ContentType>( stream, stream.available(), ContentType.FORM );
		}
		catch( UnsupportedEncodingException exception )
		{
			throw new RuntimeException( exception.getMessage() );
		}
	}

	public static class ContentType
	{
		public static final ContentType FORM = new ContentType( "application/x-www-form-urlencoded" );

		public static final Multipart MULTIPART = new Multipart();

		protected String type;

		protected ContentType( String type )
		{
			this.type = type;
		}

		public String getType()
		{
			return type;
		}

		@Override
		public String toString()
		{
			return type;
		}

		public static class Multipart extends ContentType
		{
			public static final String CLRF = "\r\n";

			protected String boundary;

			protected Multipart()
			{
				this( Long.toHexString( System.currentTimeMillis() ) );
			}

			protected Multipart( String boundary )
			{
				super( "multipart/form-data" );
				this.boundary = boundary;
			}

			public String getBoundary()
			{
				return boundary;
			}

			@Override
			public String toString()
			{
				return String.format( "%s; boundary=%s", super.toString(), boundary );
			}
		}
	}
}