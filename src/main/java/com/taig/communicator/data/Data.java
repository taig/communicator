package com.taig.communicator.data;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import com.taig.communicator.io.Countable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static com.taig.communicator.data.Header.Request.ContentType;
import static com.taig.communicator.data.Header.CONTENT_ENCODING;

/**
 * An {@link Countable.Stream.Input InputStream} that servers as an HTTP-request-body.
 *
 * @param <C> The Stream's {@link ContentType}.
 */
public abstract class Data<C extends Header.Request.ContentType> extends Countable.Stream.Input implements Appliable
{
	private C contentType;

	/**
	 * Construct a {@link Data} object based on a {@link ByteArrayInputStream}.
	 *
	 * @param stream      The underlying ByteArrayInputStream.
	 * @param contentType The Data's {@link ContentType}
	 */
	public Data( ByteArrayInputStream stream, C contentType )
	{
		super( stream );
		this.contentType = contentType;
	}

	/**
	 * Construct a {@link Data} object based on an arbitrary {@link InputStream}.
	 *
	 * @param stream      The underlying InputStream.
	 * @param length      The InputStream's content length in bytes or <code>-1</code> if unknown.
	 * @param contentType The Data's {@link ContentType}
	 */
	public Data( InputStream stream, long length, C contentType )
	{
		super( stream, length );
		this.contentType = contentType;
	}

	/**
	 * Retrieve the {@link Data Data's} {@link ContentType}.
	 *
	 * @return The Data's ContentType.
	 */
	public C getContentType()
	{
		return contentType;
	}

	/**
	 * Set the {@link HttpURLConnection HttpURLConnection's} <code>Content-Type</code> and <code>Content-Length</code>
	 * headers.
	 *
	 * @param connection The HttpURLConnection that will be configured.
	 */
	@Override
	public void apply( HttpURLConnection connection )
	{
		if( connection.getRequestProperty( Header.Request.CONTENT_TYPE ) == null )
		{
			connection.setRequestProperty( Header.Request.CONTENT_TYPE, contentType.toString() );
		}

		if( connection.getRequestProperty( Header.Request.CONTENT_LENGTH ) == null )
		{
			if( getLength() > 0 )
			{
				connection.setRequestProperty( Header.Request.CONTENT_LENGTH, String.valueOf( getLength() ) );

				if( getLength() <= Integer.MAX_VALUE )
				{
					connection.setFixedLengthStreamingMode( (int) getLength() );
				}
				else
				{
					connection.setChunkedStreamingMode( 0 );
				}
			}
			else
			{
				connection.setRequestProperty( Header.Request.CONTENT_LENGTH, "0" );
			}
		}
	}

	/**
	 * A {@link Data} representation that serves as <code>application/x-www-form-urlencoded</code> formatted
	 * transmission.
	 */
	public static class Form extends Data<ContentType.Form>
	{
		private String charset;

		/**
		 * Construct a {@link Form} object based on an {@link InputStream}.
		 * <p/>
		 * When using this constructor you are responsible to provide an adequately formatted data source via the
		 * InputStream by yourself.
		 *
		 * @param stream  The underlying InputStream.
		 * @param length  The InputStream's content length in bytes or <code>-1</code> if unknown.
		 * @param charset The InputStream's content encoding. May be <code>null</code>.
		 */
		public Form( InputStream stream, long length, String charset )
		{
			super( stream, length, ContentType.FORM );
			this.charset = charset;
		}

		/**
		 * Construct a {@link Form} object based on {@link Parameter Parameters}.
		 *
		 * @param parameters The Parameters to transmit.
		 * @param charset    The Parameter's encoding. May be <code>null</code>.
		 */
		public Form( Parameter parameters, String charset )
		{
			super( new ByteArrayInputStream( parameters.mkString( charset ).getBytes() ), ContentType.FORM );
			this.charset = charset;
		}

		/**
		 * Retrieve the {@link Form Form's} encoding.
		 *
		 * @return The Form's encoding.
		 */
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

	/**
	 * A {@link Data} representation that serves as <code>multipart/form-data</code> formatted transmission.
	 */
	public static class Multipart extends Data<ContentType.Multipart>
	{
		/**
		 * Construct a {@link Multipart} object.
		 * <p/>
		 * When using this constructor you are responsible to provide an adequately formatted data source via the
		 * InputStream by yourself. To bypass obstacle you should use the {@link Builder}.
		 *
		 * @param stream      The underlying InputStream.
		 * @param length      The InputStream's content length in bytes or <code>-1</code> if unknown.
		 * @param contentType The Multipart's {@link ContentType.Multipart ContentType}.
		 * @see Builder
		 */
		public Multipart( InputStream stream, long length, ContentType.Multipart contentType )
		{
			super( stream, length, contentType );
		}

		/**
		 * A class to generate a properly formatted {@link Multipart} object that allows to chain multiple data
		 * resources, such as {@link Parameter Parameters} and {@link File Files}.
		 */
		public static class Builder
		{
			private ContentType.Multipart contentType;

			private LinkedList<Stream.Input> streams = new LinkedList<Stream.Input>();

			/**
			 * Construct a {@link Builder} with a default {@link ContentType.Multipart}.
			 */
			public Builder()
			{
				this( new ContentType.Multipart() );
			}

			/**
			 * Construct a {@link Builder} with a custom {@link ContentType.Multipart}.
			 *
			 * @param contentType The Builder's ContentType.
			 */
			public Builder( ContentType.Multipart contentType )
			{
				this.contentType = contentType;
			}

			/**
			 * Create a {@link Header} object with a predefined <code>Content-Disposition</code> value.
			 *
			 * @param name The value for the name attribute (e.g. <code>Content-Disposition: form-data;
			 *             name="my_name"</code>).
			 * @return A Header object with a predefined <code>Content-Disposition</code> value.
			 */
			public static Header getParameterHeader( String name )
			{
				Header headers = new Header();
				headers.put( Header.Request.CONTENT_DISPOSITION, "form-data", "name=\"" + name + "\"" );
				return headers;
			}

			/**
			 * Create a {@link Header} object with predefined values for <code>Content-Disposition</code> and
			 * <code>Content-Type</code> (text/plain).
			 *
			 * @param name    The value for the name attribute (e.g. <code>Content-Disposition: form-data;
			 *                name="my_name"</code>).
			 * @param charset The value for the charset attribute (e.g. <code>Content-Type: text/plain;
			 *                charset=UTF-8</code>). May be <code>null</code>.
			 * @return A Header object with predefined <code>Content-Disposition</code> and <code>Content-Type</code>
			 * values.
			 */
			public static Header getParameterHeader( String name, String charset )
			{
				Header headers = getParameterHeader( name );
				headers.put( Header.Request.CONTENT_TYPE, "text/plain" );

				if( charset != null )
				{
					headers.add( Header.Request.CONTENT_TYPE, "charset=" + charset );
				}

				return headers;
			}

			/**
			 * Create a {@link Header} object with predefined values for <code>Content-Disposition</code>,
			 * <code>Content-Type</code> and <code>Content-Transfer-Encoding</code>.
			 *
			 * @param name   The value for the name attribute (e.g. <code>Content-Disposition: form-data;
			 *               name="my_name"</code>).
			 * @param mime   The value for the mime attribute (e.g. <code>Content-Type: text/plain</code>). May be
			 *               <code>null</code>.
			 * @param binary Whether or not to specify the <code>Content-Transfer-Encoding: binary</code> header.
			 * @return A Header object with predefined <code>Content-Disposition</code>, <code>Content-Type</code> and
			 * <code>Content-Transfer-Encoding</code> values.
			 */
			public static Header getParameterHeader( String name, String mime, boolean binary )
			{
				Header headers = getParameterHeader( name );

				if( mime != null )
				{
					headers.put( Header.Request.CONTENT_TYPE, mime );
				}

				if( binary )
				{
					headers.put( Header.Request.CONTENT_TRANSFER_ENCODING, "binary" );
				}

				return headers;
			}

			/**
			 * Add a {@link Parameter} to the {@link Builder}.
			 *
			 * @param parameters The Parameter to add.
			 * @return <code>this</code>
			 */
			public Builder addParameter( Parameter parameters )
			{
				return addParameter( parameters, null );
			}

			/**
			 * Add a {@link Parameter} to the {@link Builder}.
			 *
			 * @param parameters The Parameter to add.
			 * @param charset    The charset used for the Parameter's encoding.
			 * @return <code>this</code>
			 */
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

			/**
			 * Add a binary {@link File} to the {@link Builder}.
			 * <p/>
			 * This method guesses the File's mime type via {@link URLConnection#guessContentTypeFromName(String)}.
			 *
			 * @param name The field name (not the File's name).
			 * @param file The File.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addBinaryFile( String name, File file ) throws IOException
			{
				return addBinaryFile( name, file, URLConnection.guessContentTypeFromName( file.getName() ) );
			}

			/**
			 * Add a binary {@link File} to the {@link Builder}.
			 *
			 * @param name The field name (not the File's name).
			 * @param file The File.
			 * @param mime The File's mime type. May be <code>null</code>.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addBinaryFile( String name, File file, String mime ) throws IOException
			{
				return addFile( getParameterHeader( name, mime, true ), file );
			}

			/**
			 * Add a {@link AssetFileDescriptor} for a binary file to the {@link Builder}.
			 *
			 * @param name     The field name (not the file's name).
			 * @param fileName The file's name.
			 * @param file     The AssetFileDescriptor.
			 * @param mime     The file's mime type. May be <code>null</code>.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addBinaryFile( String name, String fileName, AssetFileDescriptor file, String mime ) throws IOException
			{
				return addFile( getParameterHeader( name, mime, true ), fileName, file.getLength(), file.createInputStream() );
			}

			/**
			 * Add an {@link InputStream} as a binary file to the {@link Builder}.
			 *
			 * @param name     The field name (not the file's name).
			 * @param fileName The file's name.
			 * @param length   The file's content length or <code>-1</code> if unknown.
			 * @param stream   The InputStream.
			 * @param mime     The file's mime type. May be <code>null</code>.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addBinaryFile( String name, String fileName, long length, InputStream stream, String mime ) throws IOException
			{
				return addFile( getParameterHeader( name, mime, true ), fileName, length, stream );
			}

			/**
			 * Add a text {@link File} to the {@link Builder}.
			 *
			 * @param name    The field name (not the File's name).
			 * @param file    The File.
			 * @param charset The File's charset. May be <code>null</code>.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addTextFile( String name, File file, String charset ) throws IOException
			{
				return addFile( getParameterHeader( name, charset ), file );
			}

			/**
			 * Add a {@link AssetFileDescriptor} for a text file to the {@link Builder}.
			 *
			 * @param name     The field name (not the file's name).
			 * @param fileName The file's name.
			 * @param file     The AssetFileDescriptor.
			 * @param charset  The file's charset. May be <code>null</code>.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addTextFile( String name, String fileName, AssetFileDescriptor file, String charset ) throws IOException
			{
				return addFile( getParameterHeader( name, charset ), fileName, file.getLength(), file.createInputStream() );
			}

			/**
			 * Add an {@link InputStream} as a text file to the {@link Builder}.
			 *
			 * @param name     The field name (not the file's name).
			 * @param fileName The file's name.
			 * @param length   The file's content length or <code>-1</code> if unknown.
			 * @param stream   The InputStream.
			 * @param charset  The file's charset. May be <code>null</code>.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addTextFile( String name, String fileName, long length, InputStream stream, String charset ) throws IOException
			{
				return addFile( getParameterHeader( name, charset ), fileName, length, stream );
			}

			/**
			 * Add an arbitrary {@link File} to the {@link Builder}.
			 * <p/>
			 * This method requires you to specify the {@link Header} by yourself. To automatically generate suitable
			 * Headers you're highly encouraged to use the more specialized methods such as {@link
			 * #addBinaryFile(String, java.io.File)} or {@link #addTextFile(String, java.io.File, String)}.
			 *
			 * @param headers The Headers that describe the File.
			 * @param file    The file.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addFile( Header headers, File file ) throws IOException
			{
				return addFile( headers, file.getName(), file.length(), new FileInputStream( file ) );
			}

			/**
			 * Add an arbitrary {@link InputStream} as file to the {@link Builder}.
			 * <p/>
			 * This method requires you to specify the {@link Header} by yourself. To automatically generate suitable
			 * Headers you're highly encouraged to use the more specialized methods such as {@link
			 * #addBinaryFile(String, String, long, java.io.InputStream, String)} or {@link #addTextFile(String,
			 * String,
			 * long, java.io.InputStream, String)}.
			 *
			 * @param headers  The Headers that describe the file.
			 * @param fileName The file's name.
			 * @param length   The file's content length or <code>-1</code> if unknown.
			 * @param stream   The InputStream.
			 * @return <code>this</code>
			 * @throws IOException
			 */
			public Builder addFile( Header headers, String fileName, long length, InputStream stream ) throws IOException
			{
				if( fileName != null )
				{
					headers.add( Header.Request.CONTENT_DISPOSITION, "filename=\"" + fileName + "\"" );
				}

				return addInputStream( headers, new Stream.Input( stream, length ) );
			}

			/**
			 * Add a byte array to the {@link Builder}.
			 *
			 * @param name The field name.
			 * @param data The binary data.
			 * @param mime The data's mime type. May be <code>null</code>.
			 * @return <code>this</code>
			 */
			public Builder addBinaryData( String name, byte[] data, String mime )
			{
				return addInputStream(
					getParameterHeader( name, mime, true ),
					new Stream.Input( new ByteArrayInputStream( data ), data.length ) );
			}

			/**
			 * Add a {@link Bitmap} to the {@link Builder}.
			 *
			 * @param name  The field name (not the Bitmap's name).
			 * @param image The Bitmap.
			 * @return <code>this</code>
			 */
			public Builder addImage( String name, Bitmap image )
			{
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				image.compress( Bitmap.CompressFormat.PNG, 100, output );
				return addBinaryData( name, output.toByteArray(), "image/png" );
			}

			/**
			 * Add an arbitrary {@link InputStream} to the {@link Builder}.
			 * <p/>
			 * This method requires you to specify the {@link Header} by yourself.
			 *
			 * @param headers The Headers that describe the InputStream.
			 * @param stream  The InputStream.
			 * @return <code>this</code>
			 */
			public Builder addInputStream( Header headers, Stream.Input stream )
			{
				String prefix = contentType.getSeparatingBoundary() + headers.mkString( "; " ) + Header.CRLF;
				String suffix = Header.CRLF;

				long length = stream.getLength();

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

			/**
			 * Transform the currently added {@link Parameter Parameters}, {@link File Files} and {@link InputStream
			 * InputStreams} into a valid {@link Multipart} object.
			 *
			 * @return A valid Multipart object that may server as a HTTP request body.
			 */
			public Multipart build()
			{
				if( !streams.isEmpty() )
				{
					String suffix = contentType.getTerminatingBoundary();
					InputStream stream = new ByteArrayInputStream( suffix.getBytes() );
					long length = suffix.length();

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