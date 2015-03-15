package io.taig.communicator.internal.result.parser

import java.io.InputStream

import io.taig.communicator.internal
import io.taig.communicator.internal.result.Parser

object	String
extends	Parser[String]
{
	override def parse( response: internal.response.Plain, stream: InputStream ) =
	{
		IteratorString.parse( response, stream ).mkString
	}
}