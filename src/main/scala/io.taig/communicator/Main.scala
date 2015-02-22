package io.taig.communicator

import java.util.concurrent.Executors

import _root_.io.taig.communicator.result.Parser
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient

import scala.util.{Failure, Success}

object Main extends App
{
	implicit val ctx = scala.concurrent.ExecutionContext.global

	val single = scala.concurrent.ExecutionContext.fromExecutor( Executors.newSingleThreadExecutor() )

	val client = new OkHttpClient()

	val req = new okhttp.Request.Builder()
		.url( "http://stackoverflow.com" )
		.header( "Accept-Encoding", "gzip" )
		.get()
		.build()

	Request.parse[String]( client, req, Parser.String )
		.onSend( println )( single )
		.onReceive( println )( single )
		.onSuccess( reponse => println( "Yeah" ) )( single )
		.onSuccess( reponse => println( "Yeah" ) )( single )
		.onFinish
		{
			case Success( response ) => println( 200 )
			case Failure( error ) => error.printStackTrace()
		}( single )
}