package io.taig.communicator.experimental

import java.io.InputStream
import java.nio.charset.Charset

import com.squareup.okhttp.MediaType

import scala.io.Source

trait Parser[T]
{
	def parse( response: Response, stream: InputStream ): T
}

object Parser
{
	implicit val nothing = new Parser[Nothing]
	{
		override def parse( response: Response, stream: InputStream ) = null.asInstanceOf[Nothing]
	}

	implicit val iteratorString = new Parser[Iterator[String]]
	{
		override def parse( response: Response, stream: InputStream ) =
		{
			val charset = Option( response.headers.get( "Content-Type" ) )
				.map( MediaType.parse )
				.map( _.charset() )
				.flatMap( Option.apply )
				.getOrElse( Charset.forName( "UTF-8" ) )

			Source.fromInputStream( stream, charset.displayName() ).getLines()
		}
	}

	implicit val string = new Parser[String]
	{
		override def parse( response: Response, stream: InputStream ) =
		{
			iteratorString.parse( response, stream ).mkString
		}
	}
}