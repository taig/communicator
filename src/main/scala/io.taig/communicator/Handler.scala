package io.taig.communicator

import java.io.InputStream

trait Handler
{
	def handle( response: Response, stream: InputStream ): Unit
}