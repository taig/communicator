package io.taig.communicator.test

import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody}
import io.taig.communicator._
import org.mockserver.client.server.MockServerClient
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

class	Test
extends	FlatSpec
with	Matchers
with	BeforeAndAfterAll
{
	implicit val client = new OkHttpClient()

	val fixture = new
	{
		val server = startClientAndServer( 8888 )

		def client = new MockServerClient( "127.0.0.1", 8888 )

		def request = new Builder().url( "http://127.0.0.1:8888" )
	}

	override protected def afterAll() = fixture.server.stop()

	"A Request" should "support GET requests" in
	{
		fixture.client
			.when( request().withMethod( "GET" ) )
			.respond( response().withStatusCode( 200 ) )

		whenReady( fixture.request.get().plain() )( _.code shouldBe 200 )
	}

	it should "support POST requests" in
	{
		fixture.client
			.when( request().withMethod( "POST" ) )
			.respond( response().withStatusCode( 200 ) )

		val body = fixture.request.post( RequestBody.create( MediaType.parse( "text/plain" ), "taig" ) ).plain()
		val empty = fixture.request.post( null ).plain()

		whenReady( body )( _.code shouldBe 200 )
		whenReady( empty )( _.code shouldBe 200 )
	}
}