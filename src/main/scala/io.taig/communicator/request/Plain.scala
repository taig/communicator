package io.taig.communicator.request

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.{Request, Response}

import scala.concurrent.{ExecutionContext => Context}

class	Plain( val client: OkHttpClient, val wrapped: okhttp.Request, val executor: Context )
extends	Request[Response]
{
	override def response( wrapped: okhttp.Response ) = new Response( wrapped )
}