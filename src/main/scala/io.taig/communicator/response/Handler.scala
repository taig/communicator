package io.taig.communicator.response

import java.io.InputStream

import _root_.io.taig.communicator.Response

trait Handler
{
	def handle( response: Response, stream: InputStream ): Unit
}