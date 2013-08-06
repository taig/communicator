package com.taig.communicator.data;

import java.net.HttpURLConnection;
import java.util.*;

import static com.taig.communicator.data.Header.Request.COOKIE;

/**
 * List of all HTTP-headers that may occur in a request- or a response-header.
 */
public class Header extends HashMap<String, Object[]> implements Appliable
{
	/**
	 * Used to specify directives that MUST be obeyed by all caching mechanisms along the request/response chain.
	 */
	public static final String CACHE_CONTROL = "Cache-Control";

	/**
	 * What type of connection the receiver would prefer.
	 */
	public static final String CONNECTION = "Connection";

	/**
	 * Implementation-specific headers that may have various effects anywhere along the request-response chain.
	 */
	public static final String PRAGMA = "Pragma";

	/**
	 * Informs the receiver of proxies through which the HTTP package was sent.
	 */
	public static final String VIA = "Via";

	public static final String CRLF = "\r\n";

	public Header()
	{
		super();
	}

	public Header( int capacity )
	{
		super( capacity );
	}

	public Header( int capacity, float loadFactor )
	{
		super( capacity, loadFactor );
	}

	public Header( Map<? extends String, ? extends Object[]> headers )
	{
		super( headers );
	}

	public Header( String key, Object... values )
	{
		super( 1 );
		put( key, values );
	}

	public Object[] put( String key, Object... values )
	{
		return super.put( key, values );
	}

	public void add( String key, Object... values )
	{
		Object[] current = put( key, (Object[]) values );

		if( current != null )
		{
			Object[] result = Arrays.copyOf( current, current.length + values.length );
			System.arraycopy( values, 0, result, current.length, values.length );
			put( key, result );
		}
	}

	@Override
	public void apply( HttpURLConnection connection )
	{
		for( Map.Entry<String, Object[]> header : entrySet() )
		{
			connection.setRequestProperty(
				header.getKey(),
				mkString( header.getValue(), header.getKey().equals( COOKIE ) ? "; " : "," ) );
		}
	}

	public String mkString( String delimiter )
	{
		StringBuilder builder = new StringBuilder();

		for( Map.Entry<String, Object[]> header : entrySet() )
		{
			builder.append( mkString( header.getKey(), header.getValue(), delimiter ) ).append( CRLF );
		}

		return builder.toString();
	}

	public static String mkString( String key, Object[] values, String delimiter )
	{
		return String.format( "%s: %s", key, mkString( values, delimiter ) );
	}

	public static String mkString( Object[] values, String delimiter )
	{
		StringBuilder builder = new StringBuilder();

		for( int i = 0; i < values.length; i++ )
		{
			builder.append( values[i] ).append( i + 1 < values.length ? delimiter : "" );
		}

		return builder.toString();
	}

	/**
	 * List of all HTTP-request-header fields.
	 */
	public static abstract class Request extends Header
	{
		/**
		 * Content-Types that are acceptable.
		 */
		public static final String ACCEPT = "Accept";

		/**
		 * Character sets that are acceptable.
		 */
		public static final String ACCEPT_CHARSET = "Accept-Charset";

		/**
		 * Acceptable encodings.
		 *
		 * @see <a href="http://en.wikipedia.org/wiki/HTTP_compression">HTTP compression</a>
		 */
		public static final String ACCEPT_ENCODING = "Accept-Encoding";

		/**
		 * Acceptable languages for response.
		 */
		public static final String ACCEPT_LANGUAGE = "Accept-Language";

		/**
		 * Acceptable version in time.
		 */
		public static final String ACCEPT_DATETIME = "Accept-Datetime";

		/**
		 * Authentication credentials for HTTP authentication.
		 */
		public static final String AUTHORIZATION = "Authorization";

		public static final String CONTENT_DISPOSITION = "Content-Disposition";

		/**
		 * The length of the request body in octets (8-bit bytes).
		 */
		public static final String CONTENT_LENGTH = "Content-Length";

		/**
		 * A Base64-encoded binary MD5 sum of the content of the request body.
		 */
		public static final String CONTENT_MD5 = "Content-MD5";

		public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

		/**
		 * The MIME type of the body of the request (used with POST and PUT requests).
		 */
		public static final String CONTENT_TYPE = "Content-Type";

		/**
		 * An HTTP cookie previously sent by the server.
		 */
		public static final String COOKIE = "Cookie";

		/**
		 * The date and time that the message was sent.
		 */
		public static final String DATE = "Date";

		/**
		 * Requests a web application to disable their tracking of a user. This is Mozilla's version of the X-Do-Not-Track
		 * header (since Firefox 4.0 Beta 11). Safari and IE9 also have support for this header.
		 */
		public static final String DNT = "DNT";

		/**
		 * Indicates that particular server behaviors are required by the client.
		 */
		public static final String EXPECT = "Expect";

		/**
		 * The email address of the user making the request.
		 */
		public static final String FROM = "From";

		/**
		 * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening. The
		 * port number may be omitted if the port is the standard port for the service requested. Mandatory since HTTP/1.1.
		 */
		public static final String HOST = "Host";

		/**
		 * Only perform the action if the client supplied entity matches the same entity on the server. This is mainly for
		 * methods like PUT to only update a resource if it has not been modified since the user last updated it.
		 */
		public static final String IF_MATCH = "If-Match";

		/**
		 * Allows a 304 Not Modified to be returned if content is unchanged.
		 */
		public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

		/**
		 * Allows a 304 Not Modified to be returned if content is unchanged.
		 *
		 * @see <a href="http://en.wikipedia.org/wiki/HTTP_ETag">HTTP ETag</a>
		 */
		public static final String IF_NONE_MATCH = "If-None-Match";

		/**
		 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire new entity.
		 */
		public static final String IF_RANGE = "If-Range";

		/**
		 * Only send the response if the entity has not been modified since a specific time..
		 */
		public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

		/**
		 * Limit the number of times the message can be forwarded through proxies or gateways.
		 */
		public static final String MAX_FORWARDS = "Max-Forwards";

		/**
		 * Authorization credentials for connecting to a proxy.
		 */
		public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

		/**
		 * Implemented as a misunderstanding of the HTTP specifications. Common because of mistakes in implementations of
		 * early HTTP versions. Has exactly the same functionality as standard Connection header.
		 */
		public static final String PROXY_CONNECTION = "Proxy-Connection";

		/**
		 * Request only part of an entity. Bytes are numbered from 0.
		 */
		public static final String RANGE = "Range";

		/**
		 * This is the address of the previous web page from which a link to the currently requested page was followed. (The
		 * word “referrer” is misspelled in the RFC as well as in most implementations).
		 */
		public static final String REFERER = "Referer";

		/**
		 * The transfer encodings the user agent is willing to accept: the same values as for the response header
		 * Transfer-Encoding can be used, plus the "trailers" value (related to the "<a href="http://en.wikipedia.org/wiki/Chunked_transfer_encoding
		 * ">chunked</a>" transfer method) to notify the server it expects to receive additional headers (the trailers) after
		 * the last, zero-sized, chunk.
		 */
		public static final String TE = "TE";

		/**
		 * Ask the server to upgrade to another protocol.
		 */
		public static final String UPGRADE = "Upgrade";

		/**
		 * The user agent string of the user agent.
		 */
		public static final String USER_AGENT = "User-Agent";

		/**
		 * A general warning about possible problems with the entity body.
		 */
		public static final String WARNING = "Warning";

		/**
		 * Allows easier parsing of the MakeModel/Firmware that is usually found in the User-Agent String of AT&T Devices.
		 */
		public static final String X_ATT_DEVICEID = "X-ATT-DeviceId";

		/**
		 * A de facto standard for identifying the originating IP address of a client connecting to a web server through an
		 * HTTP proxy or load balancer.
		 */
		public static final String X_FORWARDED_FOR = "X-Forwarded-For";

		/**
		 * Mainly used to identify Ajax requests. Most JavaScript frameworks send this header with value of XMLHttpRequest.
		 */
		public static final String X_REQUESTED_WITH = "X-Requested-With";

		/**
		 * Links to an XML file on the Internet with a full description and details about the device currently connecting. In
		 * the example to the right is an XML file for an AT&T Samsung Galaxy S2.
		 */
		public static final String X_WAP_PROFILE = "X-Wap-Profile";

		public static class ContentType
		{
			public static final Form FORM = new Form();

			public static final Multipart MULTIPART = new Multipart();

			protected String type;

			public ContentType( String type )
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

			public static class Form extends ContentType
			{
				public Form()
				{
					super( "application/x-www-form-urlencoded" );
				}
			}

			public static class Multipart extends ContentType
			{
				public static final String DIVIDER = "--";

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

				public String getSeparatingBoundary()
				{
					return DIVIDER + boundary + CRLF;
				}

				public String getTerminatingBoundary()
				{
					return DIVIDER + boundary + DIVIDER + CRLF;
				}

				@Override
				public String toString()
				{
					return super.toString() + "; boundary=" + boundary;
				}
			}
		}
	}

	/**
	 * List of all HTTP-response-header fields.
	 */
	public static abstract class Response extends Header
	{
		/**
		 * What partial content range types this server supports.
		 */
		public static final String ACCEPT_RANGES = "Accept-Ranges";

		/**
		 * Specifying which web sites can participate in <a href="http://en.wikipedia.org/wiki/Cross-origin_resource_sharing"
		 * >cross-origin resource sharing</a>.
		 */
		public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

		/**
		 * The age the object has been in a proxy cache in seconds.
		 */
		public static final String AGE = "Age";

		/**
		 * Valid actions for a specified resource. To be used for a 405 Method not allowed.
		 */
		public static final String ALLOW = "Allow";

		/**
		 * An opportunity to raise a "File Download" dialogue box for a known MIME type with binary format or suggest a
		 * filename for dynamic content. Quotes are necessary with special characters.
		 */
		public static final String CONTENT_DISPOSITION = "Content-Disposition";

		/**
		 * The type of encoding used on the data.
		 *
		 * @see <a href="http://en.wikipedia.org/wiki/HTTP_compression">HTTP compression</a>
		 */
		public static final String CONTENT_ENCODING = "Content-Encoding";

		/**
		 * The language the content is in.
		 */
		public static final String CONTENT_LANGUAGE = "Content-Language";

		/**
		 * The length of the response body in octets (8-bit bytes).
		 */
		public static final String CONTENT_LENGTH = "Content-Length";

		/**
		 * An alternate location for the returned data.
		 */
		public static final String CONTENT_LOCATION = "Content-Location";

		/**
		 * A Base64-encoded binary MD5 sum of the content of the response.
		 */
		public static final String CONTENT_MD5 = "Content-MD5";

		/**
		 * Where in a full body message this partial message belongs.
		 */
		public static final String CONTENT_RANGE = "Content-Range";

		/**
		 * The MIME type of this content.
		 */
		public static final String CONTENT_TYPE = "Content-Type";

		/**
		 * The date and time that the message was sent.
		 */
		public static final String DATE = "Date";

		/**
		 * An identifier for a specific version of a resource, often a <a href="http://en.wikipedia.org/wiki/Message_digest">message
		 * digest</a>.
		 */
		public static final String ETAG = "ETag";

		/**
		 * Gives the date/time after which the response is considered stale.
		 */
		public static final String EXPIRES = "Expires";

		/**
		 * Non-standard header used by Microsoft applications and load-balancers.
		 */
		public static final String FRONTEND_HTTPS = "Front-End-Https";

		/**
		 * The last modified date for the requested object, in <a href="http://tools.ietf.org/html/rfc2822">RFC 2822</a>
		 * format.
		 */
		public static final String LAST_MODIFIED = "Last-Modified";

		/**
		 * Used to express a typed relationship with another resource, where the relation type is defined by <a
		 * href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>.
		 */
		public static final String LINK = "Link";

		/**
		 * Used in redirection, or when a new resource has been created.
		 */
		public static final String LOCATION = "Location";

		/**
		 * This header is supposed to set <a href="http://en.wikipedia.org/wiki/P3P">P3P</a> policy, in the form of
		 * P3P:CP="your_compact_policy". However, P3P did not take off, most browsers have never fully implemented it, a lot
		 * of websites set this header with fake policy text, that was enough to fool browsers the existence of P3P policy and
		 * grant permissions for third party cookies.
		 *
		 * @see <a href="http://en.wikipedia.org/wiki/P3P">P3P</a>
		 */
		public static final String P3P = "P3P";

		/**
		 * Request authentication to access the proxy.
		 */
		public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

		/**
		 * Used in redirection, or when a new resource has been created. This refresh redirects after 5 seconds. This is a
		 * proprietary, non-standard header extension introduced by Netscape and supported by most web browsers.
		 */
		public static final String REFRESH = "Refresh";

		/**
		 * If an entity is temporarily unavailable, this instructs the client to try again after a specified period of time
		 * (seconds).
		 */
		public static final String RETRY_AFTER = "Retry-After";

		/**
		 * A name for the server.
		 */
		public static final String SERVER = "Server";

		/**
		 * An HTTP Cookie.
		 *
		 * @see <a href="http://en.wikipedia.org/wiki/HTTP_cookie">HTTP Cookie</a>
		 */
		public static final String SET_COOKIE = "Set-Cookie";

		/**
		 * A HSTS Policy informing the HTTP client how long to cache the HTTPS only policy and whether this applies to
		 * subdomains.
		 */
		public static final String SCRIPT_TRANSPORT_SECURITY = "Strict-Transport-Security";

		/**
		 * The Trailer general field value indicates that the given set of header fields is present in the trailer of a
		 * message encoded with <a href="http://en.wikipedia.org/wiki/Chunked_transfer_encoding">chunked transfer-coding</a>.
		 */
		public static final String TRAILER = "Trailer";

		/**
		 * The form of encoding used to safely transfer the entity to the user. <a href="http://www.iana.org/assignments/http-parameters">Currently
		 * defined methods</a> are: chunked, compress, deflate, gzip, identity.
		 */
		public static final String TRANSFER_ENCODING = "Transfer-Encoding";

		/**
		 * Tells downstream proxies how to match future request headers to decide whether the cached response can be used
		 * rather than requesting a fresh one from the origin server.
		 */
		public static final String VARY = "Vary";

		/**
		 * A general warning about possible problems with the entity body.
		 */
		public static final String WARNING = "Warning";

		/**
		 * Indicates the authentication scheme that should be used to access the requested entity.
		 */
		public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

		/**
		 * The only defined value, "nosniff", prevents Internet Explorer from MIME-sniffing a response away from the declared
		 * content-type. This also applies to Google Chrome, when downloading extensions.
		 */
		public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

		/**
		 * A de facto standard for identifying the originating protocol of an HTTP request, since a reverse proxy (load
		 * balancer) may communicate with a web server using HTTP even if the request to the reverse proxy is HTTPS
		 */
		public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

		/**
		 * <a href="http://en.wikipedia.org/wiki/Clickjacking">Clickjacking</a> protection: "deny" - no rendering within a
		 * frame, "sameorigin" - no rendering if origin mismatch.
		 */
		public static final String X_FRAME_OPTIONS = "X-Frame-Options";

		/**
		 * Specifies the technology (e.g. ASP.NET, PHP, JBoss) supporting the web application (version details are often in
		 * X-Runtime, X-Version, or X-AspNet-Version).
		 */
		public static final String X_POWERED_BY = "X-Powered-By";

		/**
		 * Recommends the preferred rendering engine (often a backward-compatibility mode) to use to display the content. Also
		 * used to activate <a href="http://en.wikipedia.org/wiki/Chrome_Frame">Chrome Frame</a> in Internet Explorer.
		 */
		public static final String X_UA_COMPATIBLE = "X-UA-Compatible";

		/**
		 * <a href="http://en.wikipedia.org/wiki/Cross-site_scripting">Cross-site scripting</a> (XSS) filter.
		 */
		public static final String X_XSS_PROTECTION = "X-XSS-Protection";
	}
}