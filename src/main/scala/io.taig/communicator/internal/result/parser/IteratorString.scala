package io.taig.communicator.internal.result.parser

import java.io.InputStream
import java.nio.charset.Charset

import com.squareup.okhttp.MediaType
import io.taig.communicator.internal
import io.taig.communicator.internal.result.Parser

import scala.io.Source

object	IteratorString
extends	Parser[Iterator[String]]
{
	override def parse( response: internal.response.Plain, stream: InputStream ) =
	{
		val charset = Option( response.headers.get( "Content-Type" ) )
			.map( MediaType.parse )
			.map( _.charset() )
			.flatMap( Option.apply )
			.getOrElse( Charset.forName( "UTF-8" ) )

		Source.fromInputStream( stream, charset.displayName() ).getLines()
	}
}