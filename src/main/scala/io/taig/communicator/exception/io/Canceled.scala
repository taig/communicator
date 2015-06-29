package io.taig.communicator.exception.io

import java.io.IOException

class	Canceled( message: String, cause: Throwable )
extends	IOException( message, cause )
{
	def this( message: String ) = this( message, null )

	def this( cause: Throwable ) = this( null, cause )

	def this() = this( null, null )
}