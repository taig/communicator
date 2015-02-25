package io.taig.communicator.internal.response

import com.squareup.okhttp
import io.taig.communicator.internal

class	Handled private[communicator]( wrapped: okhttp.Response, body: internal.body.Response, handler: internal.result.Handler )
extends	Plain( wrapped )
{
	handler.handle( this, body.source().inputStream() )
}

object Handled
{
	def unapply( response: Handled ) = Plain.unapply( response )
}