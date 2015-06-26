package io.taig.communicator.test

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody}
import io.taig.communicator._
import org.mockserver.client.server.MockServerClient
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Delay
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

class	Request
extends	FlatSpec
with	Matchers
with	BeforeAndAfterAll
{
	implicit val client = new OkHttpClient()

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

		whenReady( fixture.request.get().start() )( _.code shouldBe 200 )
	}

	it should "support POST requests" in
	{
		fixture.client
			.when( request().withMethod( "POST" ), Times.exactly( 2 ) )
			.respond( response().withStatusCode( 200 ) )

		val body = fixture.request.post( RequestBody.create( MediaType.parse( "text/plain" ), "taig" ) ).start()
		val empty = fixture.request.post( RequestBody.create( null, "" ) ).start()

		whenReady( body )( _.code shouldBe 200 )
		whenReady( empty )( _.code shouldBe 200 )
	}

	it should "parse strings" in
	{
		fixture.client
			.when( request().withMethod( "GET" ), Times.once() )
			.respond( response().withBody( "test" ) )

		val string = fixture.request.start().parse[String]()

		whenReady( string )( _.body shouldBe "test" )
	}

	it should "indicate cancellation with a proper exception" in
	{
		fixture.client
			.when( request().withMethod( "GET" ), Times.once() )
			.respond( response().withStatusCode( 200 ).withDelay( new Delay( SECONDS, 1 ) ) )

		val toBeCanceled = fixture.request.start()
		toBeCanceled.cancel()

		whenReady( toBeCanceled.failed, Timeout( Span( 1, Seconds ) ) )( _ shouldBe an [exception.io.Canceled] )
	}
}