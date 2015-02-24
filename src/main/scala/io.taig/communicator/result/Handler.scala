package io.taig.communicator.result

import java.io.InputStream

import io.taig.communicator

trait Handler
{
	def handle( response: communicator.response.Plain, stream: InputStream ): Unit
}