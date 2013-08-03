package com.taig.communicator.request;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import com.taig.communicator.io.Countable;
import com.taig.communicator.method.Method;
import com.taig.communicator.result.Text;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static com.taig.communicator.method.Method.POST;
import static com.taig.communicator.request.Header.CRLF;
import static com.taig.communicator.request.Header.Request.*;
import static org.apache.http.protocol.HTTP.CONTENT_ENCODING;

public abstract class Data<C extends ContentType> extends Countable.Stream.Input implements Appliable
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

	@Override
	public void apply( HttpURLConnection connection )
	{
		if( connection.getRequestProperty( CONTENT_TYPE ) == null )
		{
			connection.setRequestProperty( CONTENT_TYPE, contentType.toString() );
		}

		if( connection.getRequestProperty( CONTENT_LENGTH ) == null )
		{
			if( length > 0 )
			{
				connection.setRequestProperty( CONTENT_LENGTH, String.valueOf( length ) );
				connection.setFixedLengthStreamingMode( length );
			}
			else
			{
				connection.setRequestProperty( CONTENT_LENGTH, "0" );
			}
		}
	}

	public static class Form extends Data<ContentType.Form>
	{
		protected String charset;

		public Form( InputStream stream, int length )
		{
			super( stream, length, ContentType.FORM );
		}

		public Form( Parameter parameters )
		{
			this( parameters, null );
		}

		public Form( Parameter parameters, String charset )
		{
			super( new ByteArrayInputStream( parameters.mkString( charset ).getBytes() ), ContentType.FORM );
			this.charset = charset;
		}

		public String getEncoding()
		{
			return charset;
		}

		@Override
		public void apply( HttpURLConnection connection )
		{
			super.apply( connection );

			if( charset != null && connection.getRequestProperty( CONTENT_ENCODING ) == null )
			{
				connection.setRequestProperty( CONTENT_ENCODING, charset );
			}
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
				Header headers = new Header();
				headers.put( CONTENT_DISPOSITION, "form-data", "name=\"" + name + "\"" );
				return headers;
			}

			public static Header getParameterHeader( String name, String charset )
			{
				Header headers = getParameterHeader( name );
				headers.put( CONTENT_TYPE, "text/plain" );

				if( charset != null )
				{
					headers.add( CONTENT_TYPE, "charset=" + charset );
				}

				return headers;
			}

			public static Header getParameterHeader( String name, String mime, boolean binary )
			{
				Header headers = getParameterHeader( name );

				if( mime != null )
				{
					headers.put( CONTENT_TYPE, mime );
				}

				if( binary )
				{
					headers.put( CONTENT_TRANSFER_ENCODING, "binary" );
				}

				return headers;
			}

			public Builder addParameter( Parameter parameters )
			{
				return addParameter( parameters, null );
			}

			public Builder addParameter( Parameter parameters, String charset )
			{
				for( Map.Entry<String, Object> parameter : parameters.entrySet() )
				{
					String value = parameter.getValue().toString();

					addInputStream(
						getParameterHeader( parameter.getKey(), charset ),
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
				return addFile( getParameterHeader( name, mime, true ), file );
			}

			public Builder addBinaryFile( String name, String fileName, long length, InputStream stream, String mime ) throws IOException
			{
				return addFile( getParameterHeader( name, mime, true ), fileName, length, stream );
			}

			public Builder addTextFile( String name, File file, String charset ) throws IOException
			{
				return addFile( getParameterHeader( name, charset ), file );
			}

			public Builder addTextFile( String name, String fileName, long length, InputStream stream, String charset ) throws IOException
			{
				return addFile( getParameterHeader( name, charset ), fileName, length, stream );
			}

			public Builder addFile( Header headers, File file ) throws IOException
			{
				return addFile(
					headers,
					file.getName(),
					(int) Math.min( file.length(), Integer.MAX_VALUE ),
					new FileInputStream( file ) );
			}

			public Builder addFile( Header headers, String fileName, long length, InputStream stream ) throws IOException
			{
				if( fileName != null )
				{
					headers.add( CONTENT_DISPOSITION, "filename=\"" + fileName + "\"" );
				}

				return addInputStream( headers, new Stream.Input( stream, (int) Math.min( length, Integer.MAX_VALUE ) ) );
			}

			public Builder addBinaryData( String name, byte[] data, String mime )
			{
				return addInputStream(
					getParameterHeader( name, mime, true ),
					new Stream.Input( new ByteArrayInputStream( data ), data.length ) );
			}

			public Builder addImage( String name, Bitmap image )
			{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				image.compress( Bitmap.CompressFormat.PNG, 100, output );
				return addBinaryData( name, output.toByteArray(), "image/png" );
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

			public Multipart build()
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
					return new Multipart( new ByteArrayInputStream( new byte[] {} ), 0, contentType );
				}
			}
		}
	}
}