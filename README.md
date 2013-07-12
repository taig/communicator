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

[1]: http://android-developers.blogspot.de/2011/09/androids-http-clients.html
[2]: https://github.com/Taig/Communicator/releases
[3]: http://tools.android.com/recent/dealingwithdependenciesinandroidprojects

Usage
-----

### Request

`GET`, `POST`, `PUT`, `DELETE` or `HEAD` requests are called from `package com.taig.communicator.method.Method`.

````java
import static com.taig.communicator.method.*;
import static com.taig.communicator.result.Text;
import static com.taig.communicator.result.Image;

Response<String> source = GET<String>( Text.class, "http://www.android.com/" ).run();
Response<Bitmap> logo = GET<Bitmap>( Image.class, "http://www.android.com/images/logo.png" ).run();
````

The first argument, `Text.class`, is a result parser that will be executed after successful data retrieval on the
connection's thread. To supply your own result parser create a new subclass of `com.taig.communicator.result.Result`.
Creating a custom result parser could be necessary if you're planning to parse some HTML or JSON results.

> **Please Note**  
> Latest Android SDK versions prevent you from performing HTTP requests on the main thread because it will block your
> UI and therefore lead to a bad user experience. Place your requests within an `AsyncTask` and you're ready to request
> internets.

... more to come.
