package io.taig.communicator.result

import java.io.InputStream

import io.taig.communicator.Response

import scala.io.Source

trait Parser[T]
{
	def parse( response: Response, stream: InputStream ): T
}

object Parser
{
	object	String
	extends	Parser[String]
	{
		override def parse( response: Response, stream: InputStream ) =
		{
			Source
				.fromInputStream( stream )
				.getLines()
				.mkString
		}
	}
}