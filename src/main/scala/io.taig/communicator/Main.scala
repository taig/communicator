package io.taig.communicator

import java.util.concurrent.Executors

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.result.Parser

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object	Main
extends	App
{
	implicit val ctx = ExecutionContext.global

	val single = ExecutionContext.fromExecutor( Executors.newSingleThreadExecutor() )

	val client = new OkHttpClient()

	val req = new okhttp.Request.Builder()
		.url( "http://www.textfiles.com/drugs/2cb.txt" )
		.get()
		.tag( "asdf" )
		.build()

	val x = Request.parse[String]( client, req, Parser.String )
		.onSend( println )( single )
		.onReceive( println )( single )
		.onFinish
		{
			case Success( response ) => println( 200 )
			case Failure( error ) =>
			{
				println( "Something went wrong!" )
				error.printStackTrace()
			}
		}( single )

	println( "Prepare Cancel!" )
//	x.client.cancel( "asdf" )
	Thread.sleep( 500 )
	println( "Le Cancel:" )
	x.cancel()
}