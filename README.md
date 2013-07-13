Communicator
============

A simple Java networking library for the Android platform. It's based on `HttpURLConnection` as [recommended] [1] by
Google. *Communicator* aims to provide maximum flexibility with minimal verbosity and helps to accomplish common use cases.

> **Please Note**  
> Communicator is currently in a very early development state. Therefore please excuse the lack of documentation and
> frequent API changes.

Installation
------------

[Download] [2] the latest release as a `*.jar` file and add it to your Android app's `libs/` folder. Android will
take care of adding the dependency to your classpath. You're already good to go now. If you want more from life than
simplicity: [read] [3] this.

Usage
-----

There is a sample application available that covers the library's basic use cases. You can find the source code in the
`sample/` folder. In case you're wondering about the unusual project structure: the app is built with `sbt`. Just head to
the source folder and everything is back to normal. If you feel like running the sample app on your device but having
trouble building it then there's an `*.apk` in the download section waiting for you to give it a try.

### Request

`GET`, `POST`, `PUT`, `DELETE` or `HEAD` requests are called from `package com.taig.communicator.method.Method`.

````java
import static com.taig.communicator.method.*;
import static com.taig.communicator.result.Text;
import static com.taig.communicator.result.Image;

Response<String> source = GET<String>( Text.class, "http://www.android.com/" ).request();
Response<Bitmap> logo = GET<Bitmap>( Image.class, "http://www.android.com/images/logo.png" ).request();
````

The first argument, `Text.class`, is a result parser that will be executed after successful data retrieval on the
connection's thread. After successful request execution you will retrieve a `Response<T>` object that wraps the received
payload as well as the source URL and the server's response headers.

> **Please Note**  
> Latest Android SDK versions prevent you from performing HTTP requests on the main thread because it will block your
> UI and therefore lead to a bad user experience. Place your requests within an `AsyncTask` and you're ready to request
> internets.

### Custom Result Parser

A Result Parser it responsible for converting the connection's `InputStream` into something that makes sense for you.
If you take a look at `com.taig.communicator.result` you will find two predefined Result Parsers: `Text` to convert the
stream into a `String` and `Image` to get an `android.graphics.Bitmap`.

Hereafter is a simple example of a Result Parser that parses HTML and retrieves a specific DOM element. To get started
we need an additional library that helps us at parsing the connection's `InputStream`. [Jsoup] [4] is a reliable library
for such a task. Download the `*.jar` and add it to your app's `libs/` folder.

Create a new Java class `Headline`:

````java
package my.app;

import org.jsoup.Jsoup;

import java.io.InputStream;
import java.net.URL;

public class Headline extends Result<String>
{
        @Override
	public String process( URL url, InputStream stream ) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
````

Now, in the method body, we need to handle the `InputStream`. `Jsoup.parse()` provides a very convenient way of doing this.

````java
Jsoup.parse( stream, "UTF-8", url.toString() );
throw new UnsupportedOperationException();
````

Finally we can make use of Jsoup's parsing API.

````java
return Jsoup.parse( stream, "UTF-8", url.toString() ).select( "h1" ).first().text();
````

> **Please Note**  
> You don't have to take care of closing the `InputStream` here. This is done by the library right after you're done
> processing the stream.

### Events

... more to come.

[1]: http://android-developers.blogspot.de/2011/09/androids-http-clients.html
[2]: https://github.com/Taig/Communicator/releases
[3]: http://tools.android.com/recent/dealingwithdependenciesinandroidprojects
[4]: http://jsoup.org
