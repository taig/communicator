package io.taig.communicator.internal.request

import java.io.IOException
import java.net.URL

import com.squareup.okhttp
import com.squareup.okhttp.{Call, OkHttpClient}
import io.taig.communicator.internal
import io.taig.communicator.internal._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext => Context, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

trait	Request[+R <: internal.response.Plain, +E <: internal.event.Event, +I <: internal.interceptor.Interceptor[R, E]]
extends	Future[R]
{
	def client: OkHttpClient

	def request: okhttp.Request

	def executor: Context

	def interceptor: I

	protected var call: Call = null

	private val callbacks = mutable.Map[Context, mutable.Buffer[Try[_] => Any]]()

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
				case error: IOException if call.isCanceled => throw new internal.exception.io.Canceled( error )
			}
		}( executor )
	}

	future.onComplete( _ => callbacks.synchronized
	{
		callbacks.foreach
		{
			case ( executor, callbacks ) =>
			{
				val value = this.value.get
				executor.prepare()
				callbacks.foreach( callback => executor.execute( callback( value ): Unit ) )
			}
		}

		callbacks.clear()
	} )( executor )

	def isCanceled = call.isCanceled

	def cancel() = call.cancel()

	def onSend( f: internal.event.Progress.Send => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.send = Some(
			( progress: internal.event.Progress.Send ) => executor.execute( f( progress ) )
		)

		this
	}

	def onFinish( f: ( Try[R] ) => Any )( implicit executor: Context ): this.type =
	{
		onComplete( f )
		this
	}

	def onSuccess( f: R => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Success( value ) => f( value )
		case _ =>
	}

	def onFailure( f: Throwable => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Failure( error ) => f( error )
		case _ =>
	}

	override def onComplete[U]( f: Try[R] => U )( implicit executor: Context ): Unit = callbacks.synchronized
	{
		if( isCompleted )
		{
			executor.prepare().execute( f( value.get ): Unit )
		}
		else
		{
			callbacks
				.getOrElseUpdate( executor, mutable.ListBuffer[Try[_] => Any]() )
				.append( f.asInstanceOf[Try[_] => Any] )
		}
	}

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
	 * @see [[io.taig.communicator.internal.request.Plain]]
	 */
	def plain( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context ): internal.request.Plain =
	{
		new internal.request.Plain( client, request, executor )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Plain]]
	 */
	def plain( request: okhttp.Request )( implicit client: OkHttpClient, executor: Context ): internal.request.Plain =
	{
		plain( client, request )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Handler]]
	 */
	def handle( client: OkHttpClient, request: okhttp.Request, handler: internal.result.Handler )( implicit executor: Context ): internal.request.Handler =
	{
		new internal.request.Handler( client, request, handler, executor )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Handler]]
	 */
	def handle( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context, handler: internal.result.Handler ): internal.request.Handler =
	{
		handle( client, request, handler )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Handler]]
	 */
	def handle( request: okhttp.Request, handler: internal.result.Handler )( implicit client: OkHttpClient, executor: Context ): internal.request.Handler =
	{
		handle( client, request, handler )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Handler]]
	 */
	def handle( request: okhttp.Request )( implicit client: OkHttpClient, handler: internal.result.Handler, executor: Context ): internal.request.Handler =
	{
		handle( client, request, handler )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Parser]]
	 */
	def parse[T]( client: OkHttpClient, request: okhttp.Request, parser: internal.result.Parser[T] )( implicit executor: Context ): internal.request.Parser[T] =
	{
		new internal.request.Parser[T]( client, request, parser, executor )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Parser]]
	 */
	def parse[T]( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context, parser: internal.result.Parser[T] ): internal.request.Parser[T] =
	{
		parse( client, request, parser )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Parser]]
	 */
	def parse[T]( request: okhttp.Request, parser: internal.result.Parser[T] )( implicit client: OkHttpClient, executor: Context ): internal.request.Parser[T] =
	{
		parse( client, request, parser )
	}

	/**
	 * @see [[io.taig.communicator.internal.request.Parser]]
	 */
	def parse[T]( request: okhttp.Request )( implicit client: OkHttpClient, parser: internal.result.Parser[T], executor: Context ): internal.request.Parser[T] =
	{
		parse( client, request, parser )
	}
}