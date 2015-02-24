package io.taig.communicator.result

import java.io.InputStream

import io.taig.communicator

trait Parser[T]
{
	def parse( response: communicator.response.Plain, stream: InputStream ): T
}

object Parser
{
	implicit def iteratorString: Parser[Iterator[String]] = parser.IteratorString

	implicit def string: Parser[String] = parser.String
}