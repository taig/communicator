package io.taig.communicator.internal.result

import java.io.InputStream

import io.taig.communicator.internal

trait Handler
{
	def handle( response: internal.response.Plain, stream: InputStream ): Unit
}