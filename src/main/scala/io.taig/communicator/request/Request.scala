package io.taig.communicator.request

import java.io.IOException
import java.net.URL

import com.squareup.okhttp
import com.squareup.okhttp.{Call, OkHttpClient}
import io.taig.communicator

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext => Context, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

trait	Request[+R <: communicator.response.Plain, +E <: communicator.event.Event, +I <: communicator.interceptor.Interceptor[R, E]]
extends	Future[R]
{
	def client: OkHttpClient

	def request: okhttp.Request

	def executor: Context

	def interceptor: I

	protected var call: Call = null

	protected val events = mutable.ListBuffer.empty[( Try[communicator.response.Plain] => Any, Context )]

	protected val future =
	{
		val client = this.client.clone()
		client.networkInterceptors().add( interceptor )
		call = client.newCall( request )

		Future
		{
			try
			{
				interceptor.wrap( call.execute() )
			}
			catch
			{
				case error: IOException if call.isCanceled => throw new communicator.exception.io.Canceled( error )
			}
		}( executor )
	}

	// Execute stored events on underlying future complete
	future.onComplete( _ =>
	{
		events.foreach{ case ( event, executor ) => future.onComplete( event )( executor ) }
		events.clear()
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
				events.append( ( event.asInstanceOf[Try[communicator.response.Plain] => Any], executor ) )
			}
		}
	}

	def isCanceled = call.isCanceled

	def cancel() = call.cancel()

	def onSend( f: communicator.event.Progress.Send => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.send = Some( ( progress: communicator.event.Progress.Send ) =>
		{
			executor.execute( new Runnable
			{
				override def run() = f( progress )
			} )
		} )

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
}

object Request
{
	def apply(): okhttp.Request.Builder = new okhttp.Request.Builder()

	def apply( url: String ): okhttp.Request.Builder = apply().url( url )

	def apply( url: URL ): okhttp.Request.Builder = apply().url( url )

	/**
	 * @see [[io.taig.communicator.request.Plain]]
	 */
	def plain( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context ): Plain =
	{
		new Plain( client, request, executor )
	}

	/**
	 * @see [[io.taig.communicator.request.Plain]]
	 */
	def plain( request: okhttp.Request )( implicit client: OkHttpClient, executor: Context ): Plain =
	{
		plain( client, request )
	}

	/**
	 * @see [[io.taig.communicator.request.Handler]]
	 */
	def handle( client: OkHttpClient, request: okhttp.Request, handler: communicator.result.Handler )( implicit executor: Context ): Handler =
	{
		new Handler( client, request, handler, executor )
	}

	/**
	 * @see [[io.taig.communicator.request.Handler]]
	 */
	def handle( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context, handler: communicator.result.Handler ): Handler =
	{
		handle( client, request, handler )
	}

	/**
	 * @see [[io.taig.communicator.request.Handler]]
	 */
	def handle( request: okhttp.Request, handler: communicator.result.Handler )( implicit client: OkHttpClient, executor: Context ): Handler =
	{
		handle( client, request, handler )
	}

	/**
	 * @see [[io.taig.communicator.request.Handler]]
	 */
	def handle( request: okhttp.Request )( implicit client: OkHttpClient, handler: communicator.result.Handler, executor: Context ): Handler =
	{
		handle( client, request, handler )
	}

	/**
	 * @see [[io.taig.communicator.request.Parser]]
	 */
	def parse[T]( client: OkHttpClient, request: okhttp.Request, parser: communicator.result.Parser[T] )( implicit executor: Context ): Parser[T] =
	{
		new Parser[T]( client, request, parser, executor )
	}

	/**
	 * @see [[io.taig.communicator.request.Parser]]
	 */
	def parse[T]( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context, parser: communicator.result.Parser[T] ): Parser[T] =
	{
		parse( client, request, parser )
	}

	/**
	 * @see [[io.taig.communicator.request.Parser]]
	 */
	def parse[T]( request: okhttp.Request, parser: communicator.result.Parser[T] )( implicit client: OkHttpClient, executor: Context ): Parser[T] =
	{
		parse( client, request, parser )
	}

	/**
	 * @see [[io.taig.communicator.request.Parser]]
	 */
	def parse[T]( request: okhttp.Request )( implicit client: OkHttpClient, parser: communicator.result.Parser[T], executor: Context ): Parser[T] =
	{
		parse( client, request, parser )
	}
}