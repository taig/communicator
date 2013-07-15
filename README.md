# Communicator

A simple Java networking library for the Android platform. It's based on `HttpURLConnection` as [recommended] [1] by
Google. *Communicator* aims to provide maximum flexibility with minimal verbosity and helps to accomplish common use
cases.

> **Please Note**  
> Communicator is currently in a very early development state. Therefore please excuse the lack of documentation and
> frequent API changes.

## Installation

[Download] [2] the latest release as a `*.jar` file and add it to your Android app's `libs/` folder. Android will
take care of adding the dependency to your classpath. You're already good to go now. If you want more from life than
simplicity: read [this] [3].

## Usage

There is a sample application available that covers the library's basic use cases. You can find the source code in the
`sample/` folder. In case you're wondering about the unusual project structure: the app is built with `sbt`. Just head
to the source folder and everything is back to normal. If you feel like running the sample app on your device but having
trouble building it then there's an `*.apk` in the download section waiting for you to give it a try.

### Request

`GET`, `POST`, `PUT`, `DELETE` or `HEAD` requests are called via `com.taig.communicator.method.Method`.

````java
import static com.taig.communicator.method.Method.*;
import com.taig.communicator.result.Text;
import com.taig.communicator.result.Image;

Response<String> source = GET<String>( Text.class, "http://www.android.com/" ).followRedirects( true ).request();
Response<Bitmap> logo = GET<Bitmap>( Image.class, "http://www.android.com/images/logo.png" ).request();
````

The first argument, `Text.class`, is a Parser that will be executed in order to process the connection's `InputStream`.
After successful request execution you will retrieve a `Response<T>` object that wraps the parsed payload as well as
the source's URL and the server's response headers.

### Custom Parser

A Parser is responsible for converting the connection's `InputStream` into something that makes sense for you.
If you take a look at `com.taig.communicator.result` you will find two predefined Parsers: `Text` for converting the
stream into a `String` and `Image` to get an `android.graphics.Bitmap`.

In order to create your own Parser (e.g. for processing HTML or JSON) all you have to do is creating a new class that
implements the `com.taig.communicator.result.Parser` interface.

````java
public interface Parser<T>
{
	public abstract T parse( URL url, InputStream stream ) throws IOException;
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
GET<String>( Text.class, "http://www.android.com/", new Event<String>()
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

In order to add Form data (or more generally spoken Key/Value pairs) to your request body you have to specify the `data`
argument in a `POST` or `PUT` request. More precisely *Communicator* demands you to supply your key/value pairs as
`Map<String, String>`.

````java
Map<String, String> params = new HashMap<String, String>();
params.put( "email", "my.taig@gmail.com" );
params.put( "pass", "As if!" );

POST<String>( Text.class, "https://facebook.com/login.php", Data.from( params ) ).run();
````

#### Binary Data (File Upload)

TODO

#### Cookies

Last but not least you can also add cookies to a request header. The common user/server HTTP workflow starts with a
`Set-Cookie` directive in a server's response header.

````java
// Transfer all cookies that were supplied in the preceding response.
List<HttpCookie> cookies = HEAD( "https://www.google.com" ).run().getCookies();
GET<String>( Text.class, "https://www.google.com" ).setCookies( cookies ).run();

// Do whatever the fuck you want.
GET<String>( Text.class, "https://www.google.com" ).addCookie( "session", "1234" ).run();
````

Furthermore *Communicator* comes with a `CookieStore` implementation that persists cookies via Android's
`SharedPreferences`. This is very useful if you have to store cookies beyond an app's lifecycle (e.g. a session cookie).

````java
CookieStore store = new CookieStore();
Response<Void> response = HEAD( "https://www.google.com" ).request();

// Persist retrieved cookies.
store.add( response );

// Send persisted cookies along with request.
GET<String>( Text.class, "https://www.google.com" ).setCookies( store ).run();
````

[1]: http://android-developers.blogspot.de/2011/09/androids-http-clients.html
[2]: https://github.com/Taig/Communicator/releases
[3]: http://tools.android.com/recent/dealingwithdependenciesinandroidprojects