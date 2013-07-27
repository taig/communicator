package com.taig.communicator.request;

import android.graphics.Bitmap;
import com.taig.communicator.io.Countable;

import java.io.*;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static com.taig.communicator.request.Header.CRLF;
import static com.taig.communicator.request.Header.Request.*;

public abstract class Data<C extends ContentType> extends Countable.Stream.Input
{
	protected C contentType;

	public Data( ByteArrayInputStream stream, C contentType )
	{
		super( stream );
		this.contentType = contentType;
	}

	public Data( InputStream stream, int length, C contentType )
	{
		super( stream, length );
		this.contentType = contentType;
	}

	public C getContentType()
	{
		return contentType;
	}

	public static class Form extends Data<ContentType.Form>
	{
		public Form( InputStream stream, int length )
		{
			super( stream, length, ContentType.FORM );
		}

		public Form( Parameter parameters )
		{
			super( new ByteArrayInputStream( parameters.toString().getBytes() ), ContentType.FORM );
		}
	}

	public static class Multipart extends Data<ContentType.Multipart>
	{
		public Multipart( InputStream stream, int length, ContentType.Multipart contentType )
		{
			super( stream, length, contentType );
		}

		public static class Builder
		{
			protected ContentType.Multipart contentType;

			protected LinkedList<Stream.Input> streams = new LinkedList<Stream.Input>();

			public Builder()
			{
				this( new ContentType.Multipart() );
			}

			public Builder( ContentType.Multipart contentType )
			{
				this.contentType = contentType;
			}

			public static Header getParameterHeader( String name )
			{
				return getParameterHeader( name, null, null );
			}

			public static Header getParameterHeader( String name, String mime, String charset )
			{
				Header headers = new Header();
				headers.put( CONTENT_DISPOSITION, "form-data", "name=\"" + name + "\"" );

				if( mime != null )
				{
					headers.put( CONTENT_TYPE, mime );
				}

				if( charset != null )
				{
					headers.add( CONTENT_TYPE, "charset=" + charset );
				}

				return headers;
			}

			public Builder addParameter( String key, Object value )
			{
				return addParameter( key, value, null );
			}

			public Builder addParameter( String key, Object value, String charset )
			{
				Parameter parameter = new Parameter();
				parameter.put( key, value );
				return addParameters( parameter, charset );
			}

			public Builder addParameters( Parameter parameters )
			{
				return addParameters( parameters, null );
			}

			public Builder addParameters( Parameter parameters, String charset )
			{
				for( Map.Entry<String, Object> parameter : parameters.entrySet() )
				{
					String value = parameter.getValue().toString();

					addInputStream(
						getParameterHeader( parameter.getKey(), "text/plain", charset ),
						new Stream.Input( new ByteArrayInputStream( value.getBytes() ), value.length() ) );
				}

				return this;
			}

			public Builder addBinaryFile( String name, File file ) throws IOException
			{
				return addBinaryFile( name, file, URLConnection.guessContentTypeFromName( file.getName() ) );
			}

			public Builder addBinaryFile( String name, File file, String mime ) throws IOException
			{
				Header headers = getParameterHeader( name, mime, null );
				headers.put( CONTENT_TRANSFER_ENCODING, "binary" );
				return addFile( headers, file );
			}

			public Builder addTextFile( String name, File file, String charset ) throws IOException
			{
				return addFile( getParameterHeader( name, "text/plain", charset ), file );
			}

			public Builder addFile( Header headers, File file ) throws IOException
			{
				headers.add( CONTENT_DISPOSITION, "filename=\"" + file.getName() + "\"" );
				return addInputStream( headers, new Stream.Input(
					new FileInputStream( file ),
					(int) Math.min( file.length(), Integer.MAX_VALUE ) ) );
			}

			public Builder addBinaryData( String name, byte[] data, String mime ) throws IOException
			{
				return addInputStream(
					getParameterHeader( name, mime, null ),
					new Stream.Input( new ByteArrayInputStream( data ), data.length ) );
			}

			public Builder addImage( String name, Bitmap image )
			{
				return this;
			}

			public Builder addInputStream( Header headers, Stream.Input stream )
			{
				String prefix = contentType.getSeparatingBoundary() + headers.mkString( "; " ) + CRLF;
				String suffix = CRLF;

				int length = stream.getLength();

				if( length >= 0 )
				{
					length += prefix.length();
					length += suffix.length();
				}

				streams.add(
					new Stream.Input(
						new SequenceInputStream(
							new SequenceInputStream( new ByteArrayInputStream( prefix.getBytes() ), stream ),
							new ByteArrayInputStream( suffix.getBytes() ) ), length ) );

				return this;
			}

			public Data<ContentType.Multipart> build()
			{
				if( !streams.isEmpty() )
				{
					String suffix = contentType.getTerminatingBoundary();
					InputStream stream = new ByteArrayInputStream( suffix.getBytes() );
					int length = suffix.length();

					for( Iterator<Stream.Input> iterator = streams.descendingIterator(); iterator.hasNext(); )
					{
						Stream.Input current = iterator.next();
						stream = new SequenceInputStream( current, stream );

						if( current.getLength() >= 0 && length >= 0 )
						{
							length += current.getLength();
						}
						else
						{
							length = -1;
						}
					}

					return new Multipart( stream, length, contentType );
				}
				else
				{
					return new Multipart( new ByteArrayInputStream( new byte[] { } ), 0, contentType );
				}
			}
		}
	}
}