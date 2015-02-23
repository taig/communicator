package io.taig.communicator

import java.util.concurrent.Executors

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.result.Parser

import scala.concurrent.Future
import scala.util.{Failure, Success}

object	Main
extends	App
{
	implicit val ctx = scala.concurrent.ExecutionContext.global

	val single = scala.concurrent.ExecutionContext.fromExecutor( Executors.newSingleThreadExecutor() )

	val client = new OkHttpClient()

	val req = new okhttp.Request.Builder()
		.url( "http://www.textfiles.com/drugs/2cb.txt" )
		.get()
		.build()

	val x = Request.parse[String]( client, req, Parser.String )
		.onSend( println )( single )
		.onReceive( println )( single )
		.onFinish
		{
			case Success( response ) => println( 200 )
			case Failure( error ) => error.printStackTrace()
		}( single )

	Future
	{
		Thread.sleep( 1000 )
		println( "Le Cancel:" )
		x.cancel()
		client.cancel( req )
	}( ctx )
}