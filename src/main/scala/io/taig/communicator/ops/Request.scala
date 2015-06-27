package io.taig.communicator.ops

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator

import scala.concurrent.ExecutionContext

trait Request
{
	def request: okhttp.Request

	def start()( implicit client: OkHttpClient, executor: ExecutionContext ) = communicator.Request( request )
}