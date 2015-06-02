package io.taig.communicator.experimental

import java.io.IOException
import java.net.URL

import com.squareup.okhttp
import com.squareup.okhttp._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext => Context, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

trait	Request[T]
extends	Future[Response.Payload[T]]
{
	def client: OkHttpClient

	def request: okhttp.Request

	def parser: Parser[T]

	def executor: Context

	val interceptor = new Interceptor( request )

	protected var call: Call = null

	private val callbacks = mutable.Map[Context, mutable.Buffer[Try[_] => Any]]()

	protected val future: Future[Response.Payload[T]] =
	{
		val client = this.client.clone()
		client.networkInterceptors().add( interceptor )
		call = client.newCall( request )

		Future
		{
			try
			{
				val response = call.execute()
				val wrapped = new Response( response )
				wrapped.withPayload( parser.parse( wrapped, response.body().byteStream() ) )
			}
			catch
			{
				case error: IOException if call.isCanceled => throw new exception.io.Canceled( error )
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
				callbacks.foreach( _( value ) )
			}
		}

		callbacks.clear()
	} )( executor )

	def isCanceled = call.isCanceled

	def cancel() = call.cancel()

	def onSend( f: Progress.Send => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.onSend( ( progress: Progress.Send ) => executor.execute( f( progress ) ) )
		this
	}

	def onReceive( f: Progress.Receive => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.onReceive( ( progress: Progress.Receive ) => executor.execute( f( progress ) ) )
		this
	}

	def onFinish( f: ( Try[Response.Payload[T]] ) => Any )( implicit executor: Context ): this.type =
	{
		onComplete( f )
		this
	}

	def onSuccess( f: T => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Success( value ) => f( value.body )
		case _ =>
	}

	def onFailure( f: Throwable => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Failure( error ) => f( error )
		case _ =>
	}

	override def onComplete[U]( f: Try[Response.Payload[T]] => U )( implicit executor: Context ): Unit = callbacks.synchronized
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
	def prepare(): okhttp.Request.Builder = new okhttp.Request.Builder()

	def prepare( url: String ): okhttp.Request.Builder = prepare().url( url )

	def prepare( url: URL ): okhttp.Request.Builder = prepare().url( url )

	def apply[T: Parser]( request: okhttp.Request )( implicit client: OkHttpClient, executor: Context ): Request[T] =
	{
		new Implementation[T]( request, client, executor )
	}

	private case class	Implementation[T: Parser]( request: okhttp.Request, client: OkHttpClient, executor: Context )
	extends				Request[T]
	{
		override def parser = implicitly[Parser[T]]
	}
}