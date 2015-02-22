package io.taig.communicator

import java.io.InputStream
import java.util.zip.GZIPInputStream

import scala.io.Source

trait	Parser[T]
extends	Handler
{
	def parse( response: Response, source: InputStream ): T

	override def handle( response: Response, stream: InputStream ) = parse( response, stream )
}

object Parser
{
	object	String
	extends	Parser[String]
	{
		override def parse( response: Response, source: InputStream ) =
		{
			Source.fromInputStream( new GZIPInputStream( source ) ).getLines().mkString
		}
	}
}