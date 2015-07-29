package io.taig.communicator.test

import java.io.{IOException, InputStream}
import java.util.concurrent.TimeUnit.SECONDS

import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody}
import io.taig.communicator._
import org.mockserver.client.server.MockServerClient
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.time._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

class	Request
extends	FlatSpec
with	Matchers
with	BeforeAndAfterAll
{
	implicit val client = new OkHttpClient()

	implicit val patience = ScalaFutures.PatienceConfig( Span( 3, Seconds ), Span( 250, Milliseconds ) )

	val fixture = new
	{
		val server = startClientAndServer( 8888 )

		def client = new MockServerClient( "127.0.0.1", 8888 )

		def request = Request.prepare( "http://127.0.0.1:8888" )
	}

	override protected def afterAll() = fixture.server.stop()

	"A Request" should "support GET requests" in
	{
		fixture.client
			.when( request().withMethod( "GET" ), Times.once() )
			.respond( response().withStatusCode( 200 ) )

		whenReady( fixture.request.get().start[Unit]() )( _.code shouldBe 200 )
	}

	it should "support POST requests" in
	{
		fixture.client
			.when( request().withMethod( "POST" ), Times.exactly( 2 ) )
			.respond( response().withStatusCode( 200 ) )

		val body = fixture.request.post( RequestBody.create( MediaType.parse( "text/plain" ), "taig" ) ).start[Unit]()
		val empty = fixture.request.post( RequestBody.create( null, "" ) ).start[Unit]()

		whenReady( body )( _.code shouldBe 200 )
		whenReady( empty )( _.code shouldBe 200 )
	}

	it should "parse strings" in
	{
		fixture.client
			.when( request().withMethod( "GET" ), Times.once() )
			.respond( response().withBody( "test" ) )

		val string = fixture.request.start[String]()

		whenReady( string )( _.body shouldBe "test" )
	}

	it should "indicate cancellation with a proper exception" in
	{
		fixture.client
			.when( request().withMethod( "GET" ), Times.once() )
			.respond( response().withStatusCode( 200 ).withDelay( SECONDS, 1 ) )

		val toBeCanceled = fixture.request.start[String]()
		toBeCanceled.cancel()

		whenReady( toBeCanceled.failed )( _ shouldBe an [exception.io.Canceled] )
	}

	it should "fail if the parser throws an Exception" in
	{
		fixture.client
			.when( request().withMethod( "GET" ), Times.once() )
			.respond( response().withStatusCode( 200 ).withBody( "test" ) )

		implicit val parser = new Parser[String]
		{
			override def parse( response: Response, stream: InputStream ) = throw new IOException()
		}

		val failing = fixture.request.start[String]()

		whenReady( failing.failed )( _ shouldBe an [IOException] )
	}
}