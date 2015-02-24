package io.taig.communicator.response

import com.squareup.okhttp
import io.taig.communicator
import io.taig.communicator.result.Handler

class	Handled private[communicator]( wrapped: okhttp.Response, body: communicator.body.Response, handler: Handler )
extends	Plain( wrapped )
{
	handler.handle( this, body.source().inputStream() )
}

object Handled
{
	def unapply( response: Handled ) = Plain.unapply( response )
}