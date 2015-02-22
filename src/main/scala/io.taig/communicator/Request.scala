package io.taig.communicator

import java.io.IOException

import _root_.io.taig.communicator.request.Content
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import _root_.io.taig.communicator.body.{Receive, Send}

import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext => Context, Future, TimeoutException}
import scala.util.{Success, Failure, Try}
import scala.collection.mutable

trait	Request[+R <: Response]
extends	Future[R]
with	Cancelable
{
	def response( wrapped: okhttp.Response ): R

	def client: OkHttpClient

	def wrapped: okhttp.Request

	def executor: Context

	protected val events = mutable.Queue.empty[( Try[Response] => Any, Context )]

	protected val listener = new Listener

	protected lazy val send = new Send( wrapped.body(), listener.send )

	protected lazy val call = client.newCall
	{
		if( wrapped.body() == null )
		{
			wrapped
		}
		else
		{
			wrapped
				.newBuilder()
				.method( wrapped.method(), send )
				.build()
		}
	}

	protected val future = Future
	{
		try
		{
			response( call.execute() )
		}
		catch
		{
			case error: IOException if error.getMessage == "Canceled" => cancel( -1 )
		}
	}( executor )

	future.onComplete( _ =>
	{
		events.dequeueAll( _ => true ).foreach
		{
			case ( event, executor ) => future.onComplete( event )( executor )
		}
	} )( executor )

	/**
	 * Enqueue an onComplete event
	 * 
	 * The events cannot be passed to onComplete right away while the Future is still running because this will
	 * lead to a wrong execution order.
	 * 
	 * @param event Event to execute when the Future is complete
	 */
	protected def enqueue( event: Try[R] => Any )( implicit executor: Context ): Unit =
	{
		if( isCompleted )
		{
			future.onComplete( event )
		}
		else
		{
			events.synchronized
			{
				events.enqueue( ( event.asInstanceOf[Try[Response] => Any], executor ) )
			}
		}
	}

	override def isCanceled = call.isCanceled

	override def cancel() =
	{
		call.cancel()

		if( wrapped.body() != null )
		{
			send.cancel()
		}
	}

	def onSend( f: Progress.Send => Unit )( implicit executor: Context ): this.type =
	{
		listener.send = ( progress: Progress.Send ) => executor.execute( f( progress ) )
		this
	}

	override def onFailure[U]( pf: PartialFunction[Throwable, U] )( implicit executor: Context ) =
	{
		onFailure( ( error: Throwable ) => pf.apply( error ): Unit )
	}

	def onFailure( f: Throwable => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Failure( error ) => f( error )
		case _ =>
	}

	override def onSuccess[U]( pf: PartialFunction[R, U] )( implicit executor: Context ) =
	{
		onSuccess( ( response: R ) => pf.apply( response ): Unit )
	}

	def onSuccess( f: R => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Success( response ) => f( response )
		case _ =>
	}

	def onFinish( f: ( Try[R] ) => Any )( implicit executor: Context ): this.type =
	{
		enqueue( f )
		this
	}

	override def onComplete[U]( f: ( Try[R] ) => U )( implicit executor: Context ) = onFinish( f )

	override def isCompleted = future.isCompleted

	override def value = future.value

	@throws[Exception]
	override def result( atMost: Duration )( implicit permit: CanAwait ) = future.result( atMost )

	@throws[InterruptedException]
	@throws[TimeoutException]
	override def ready( atMost: Duration )( implicit permit: CanAwait ) =
	{
		future.ready( atMost )
		this
	}

	protected class Listener
	{
		var send: Progress.Send => Unit = null
	}
}

object Request
{
	def apply[T]( request: okhttp.Request )( implicit client: OkHttpClient, parser: Parser[T], executor: Context ): Content[T] =
	{
		new Content[T]( client, request, parser, executor )
	}

//	def apply[T]( client: OkHttpClient, request: okhttp.Request )( implicit parser: Parser[T], executor: Context ): Content[T] =
//	{
//		new Content[T]( client, request, parser, executor )
//	}
}