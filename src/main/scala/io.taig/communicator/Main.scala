package io.taig.communicator

import java.util.concurrent.Executors

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.result.Parser

import scala.util.{Failure, Success}

object	Main
extends	App
{
	implicit val ctx = scala.concurrent.ExecutionContext.global

	val single = scala.concurrent.ExecutionContext.fromExecutor( Executors.newSingleThreadExecutor() )

	val client = new OkHttpClient()

	val req = new okhttp.Request.Builder()
		.url( "http://blog.fefe.de" )
		.get()
		.build()

	Request.parse[String]( client, req, Parser.String )
		.onSend( println )( single )
		.onReceive( println )( single )
		.onComplete
		{
			case Success( response ) => println( 200 )
			case Failure( error ) => error.printStackTrace()
		}( single )
}