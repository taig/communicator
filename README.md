# Communicator

A simple Java networking library for the Android platform. It's based on `HttpURLConnection` as [recommended] [1] by
Google. *Communicator* aims to provide maximum flexibility with minimal verbosity and helps to accomplish common use
cases.

This library requires at least API level 10 (Android 2.3.3), that is [more than 96%] [2] of the currently active
devices.

## Installation

Add `http://taig.github.io/Toolbelt/release` to your content resolvers, and add the dependency:

> Package: com.taig.android

> Name: communicator

> Version: 1.1.2

In sbt, for instance:
```
resolvers += Resolver.url( "Communicator", url( "http://taig.github.io/Communicator/release" ) )( ivyStylePatterns )
libraryDependencies += "com.taig.android" % "communicator" % "1.1.2"
```

### Request

`GET`, `POST`, `PUT`, `DELETE` or `HEAD` requests are called via `com.taig.communicator.method.Method`.

````java
import static com.taig.communicator.method.Method.*;
import com.taig.communicator.result.Parser;

Response.Payload<String> source = GET( Parser.TEXT, new URL( "http://www.android.com/" ) ).followRedirects( true ).request();
Response.Payload<Bitmap> logo = GET( Parser.IMAGE, new URL( "http://www.android.com/images/logo.png" ) ).request();
````

The first argument, `Parser.TEXT`, is a Parser that will be executed in order to process the connection's `InputStream`.
After successful request execution you will retrieve a `Response<T>` object that wraps the parsed payload as well as
the source's URL and the server's response headers.

### Custom Parser

A Parser is responsible for converting the connection's `InputStream` into something that makes sense for you.
If you take a look at `com.taig.communicator.result` you will find three predefined Parsers: `Text` for converting the
stream into a `String`, `Image` to get an `android.graphics.Bitmap` and `Ignore` in case you don't care about the server
response.

In order to create your own Parser (e.g. for processing HTML or JSON) all you have to do is creating a new class that
implements the `com.taig.communicator.result.Parser` interface.

````java
public interface Parser<T>
{
	public T parse( URL url, InputStream stream ) throws IOException;
}
````

> **Please Note**  
> You don't have to take care of closing the `InputStream` in the `parser()` method. This is done by the library right
> after you're done processing the stream. The additional `url` argument may come in handy if you're trying to convert
> a HTML document's relative links to absolute URLs.

### Events

Events provide a very simple way of interacting with your application during request execution. Every HTTP method in
`com.taig.communicator.Method` takes an additional parameter `Event<T>`.

````java
GET<String>( Parser.TEXT, new URL( "http://www.android.com/" ), new Event<String>()
{
	@Override
	protected void onReceive( int progress )
	{
		Log.d( "MyAppTag", "Resource load status: " + progress + "%" );
	}
} ).run();
````

You can override a variety of event methods whose body will always be **executed on the main thread**. So you're safe
to interact with you app's user interface (e.g. updating a `ProgressBar`) without wrapping your code in a
`runOnUiThread` call. On the other hand you should avoid doing any heavy lifting consequently.

> **Please Note**  
> You can fire a `Request` with either `run()` or `request()`. The latter method returns a `Response<T>` but will in
> exchange not trigger the `onSuccess()`, `onFailure()` and `onFinish()` events! `run()` however does not have a return
> value but triggers all available events.

### Sending Payload

#### Key/Value Data (Form Submit)

In order to add form data (or more generally spoken key/value pairs) to the request body you have to specify the `data`
argument in a `POST` or `PUT` request. More precisely *Communicator* demands you to supply your key/value pairs as
`com.taig.communicator.data.Parameter`, a subclass of `Map<String, String>`.

````java
Parameter params = new Parameter();
params.put( "email", "my.taig@gmail.com" );
params.put( "pass", "As if!" );

POST( Parser.TEXT, new URL( "https://facebook.com/login.php" ), params ).run();
````

The supplied data will then be properly encoded for transmission and the request headers `Content-Length` and
`Content-Type` will be set accordingly.

#### Binary Data (File Upload)

Since a `multipart/fom-data` request tends to have way more complex headers than a simple `form-url-encoded` request,
*Communicator* provides a Builder to allow a simple header build up.

````java
Data data = new Data.Multipart.Builder()
	.addParameter( params )
	.addTextFile( "cv", new File( "/my_cv.txt" ), "utf-8" )
	.build();

POST( Parser.TEXT, new URL( "http://some.webservice.com" ), data ).run();
````

#### Cookies

Last but not least you can also add cookies to a request header. The common user/server HTTP workflow starts with a
`Set-Cookie` directive in a server's response header.

````java
Response<Void> response = HEAD( new URL( "https://www.google.com" ) ).request();

GET( Parser.TEXT, new URL( "https://www.google.com" ) )
    .addCookie( new HttpCookie( "remember_me", "true" )
    .addHeader(
        COOKIE,
        new HttpCookie( "js", "true" ),
        new HttpCookie( "flash", "false" ) )
    .run();
````

Furthermore *Communicator* comes with a `CookieStore` implementation that persists cookies via Android's
`SharedPreferences`. This is very useful if you have to store cookies beyond an app's lifecycle (e.g. a session cookie).

````java
CookieStore store = new PersistedCookieStore( MyActivity.this );
Response response = HEAD( new URL( "https://www.google.com" ) ).request();

for( HttpCookie cookie : response.getCookies() )                // Persist retrieved cookies.
{
	store.add( reponse.getURL().toURI(), cookie );
}

GET( Parser.TEXT, new URL( "https://www.google.com" ) )         // Send persisted cookies that are associated with
    .putCookie( store )                                         // "google.com" along with the request.
    .run();
````

### Communicator (Asynchronous Request Execution)

In the `com.taig.communicator.concurrent` package there is an implementation of Java's `Executor`, called
`Communicator`. This class servers to manage multiple, concurrent requests and is also able to manage cookies during
HTTP interactions. Constructing a `Communicator` object requires you to specify the maximum amount of concurrent
connections. It will then go ahead and spawn the same amount of Threads in order to process your following requests.

````java
Communicator communicator = new Communicator( 2 );
communicator.execute( GET<String>( Text.class, new URL( "http://www.example.org" ) ) );
communicator.execute( GET<String>( Text.class, new URL( "http://www.example.com" ) ) );
communicator.execute( GET<String>( Text.class, new URL( "http://www.example.net" ) ) );
````

> **Please Note**  
> As defined in Java's `Executor` interface the method `execute( Runnable )` is not limited to `Request` objects, but
> you should think twice before submitting anything else.

`Communicator` keeps track of the request queue very accurately. If you need to perform an urgent request you are able
to declare it's priority via the `execute( Runnable runnable, boolean skipQueue )` method.

````java
communicator.execute( GET<String>( Parser.TEXT, new URL( "http://www.example.xxx" ) ), true );
````

By default `Communicator` drops all cookies as its policy says `CookiePolicy.ACCEPT_NONE`. You can changes this behavior
with the `accept( CookieStore, CookiePolicy )` method.

````java
communicator.accept( new PersistedCookieStore(), CookiePolicy.ACCEPT_ALL );
communicator.accept( new PersistedCookieStore(), new CookiePolicy( "session", "sess", "SESSIONID" ) );
````

Creating a `Communicator` with its several Threads can become a memory intensive task. I advise you to do it only once
during your app's lifecycle: Add a static reference from your application context and use it from all of your activities.

Instead of using `Communicator` it is of course possible to feed any arbitrary `Executor` with `Request` objects (since
they are `Runnables`). But I highly recommend you to use this implementation because it provides a variety of
interruption methods to stop or cancel active requests. This is something you should keep in mind when facing Android's
activity lifecycles: make sure to call `cancel()` or `stop()` when an activity is being destroyed and you won't need
the requested content any more. Also don't forget to shut down the executor when the application is being destroyed
(`close()` or `closeNow()`) to tear down the threads properly.

> **Please Note**  
> Keep in mind that the concurrent processing of multiple resources (especially images) leads to a very high memory
> consumption. If you're facing `OutOfMemoryExceptions` during your requests you should consider to reduce the amount of
> simultaneous connections or to improve your Parser (e.g. forward the data immediately to the cache directory).

[1]: http://android-developers.blogspot.de/2011/09/androids-http-clients.html
[2]: http://developer.android.com/about/dashboards/index.html#Platform
[3]: https://github.com/Taig/Communicator/releases
[4]: http://tools.android.com/recent/dealingwithdependenciesinandroidprojects