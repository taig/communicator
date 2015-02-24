package io.taig.communicator.result.parser

import java.io.InputStream

import _root_.io.taig.communicator
import io.taig.communicator.result.Parser

object	String
extends	Parser[String]
{
	override def parse( response: communicator.response.Plain, stream: InputStream ) = IteratorString.parse( response, stream ).mkString
}