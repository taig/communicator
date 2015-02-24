package io.taig.communicator.result.parser

import java.io.InputStream
import java.nio.charset.Charset

import _root_.io.taig.communicator
import com.squareup.okhttp.MediaType
import io.taig.communicator.result.Parser

import scala.io.Source

object	IteratorString
extends	Parser[Iterator[String]]
{
	override def parse( response: communicator.response.Plain, stream: InputStream ) =
	{
		val charset = Option( response.headers.get( "Content-Type" ) )
			.map( MediaType.parse )
			.map( _.charset() )
			.flatMap( Option.apply )
			.getOrElse( Charset.forName("UTF-8") )

		Source.fromInputStream( stream, charset.displayName() ).getLines()
	}
}