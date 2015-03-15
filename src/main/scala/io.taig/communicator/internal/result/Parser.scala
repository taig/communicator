package io.taig.communicator.internal.result

import java.io.InputStream

import io.taig.communicator.internal

trait Parser[T]
{
	def parse( response: internal.response.Plain, stream: InputStream ): T
}

object Parser
{
	implicit def iteratorString: Parser[Iterator[String]] = parser.IteratorString

	implicit def string: Parser[String] = parser.String
}