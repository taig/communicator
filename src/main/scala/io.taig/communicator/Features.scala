package io.taig.communicator

import com.squareup.okhttp.Call

import scala.collection.mutable
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait	Features[+R <: Response]
extends	Future[R]
{
	def wrapped: Future[R]

	def callbacks: mutable.Map[ExecutionContext, mutable.Buffer[Try[_] => Any]]

	def call: Call

	def interceptor: Interceptor

	def isCanceled = call.isCanceled

	def cancel() = call.cancel()

	def onSend( f: Progress.Send => Unit )( implicit executor: ExecutionContext ): this.type =
	{
		interceptor.onSend( ( progress: Progress.Send ) => executor.execute( f( progress ) ) )
		this
	}

	def onReceive( f: Progress.Receive => Unit )( implicit executor: ExecutionContext ): this.type =
	{
		interceptor.onReceive( ( progress: Progress.Receive ) => executor.execute( f( progress ) ) )
		this
	}

	def onFinish( f: ( Try[R] ) => Any )( implicit executor: ExecutionContext ): this.type =
	{
		onComplete( f )
		this
	}

	def onSuccess( f: R => Unit )( implicit executor: ExecutionContext ): this.type = onFinish
	{
		case Success( value ) => f( value )
		case _ =>
	}

	def onFailure( f: Throwable => Unit )( implicit executor: ExecutionContext ): this.type = onFinish
	{
		case Failure( error ) => f( error )
		case _ =>
	}

	override def onComplete[U]( f: Try[R] => U )( implicit executor: ExecutionContext ): Unit = callbacks.synchronized
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

	override def isCompleted = wrapped.isCompleted

	override def value = wrapped.value

	@throws[Exception]
	override def result( atMost: Duration )( implicit permit: CanAwait ) = wrapped.result( atMost )

	@throws[InterruptedException]
	@throws[TimeoutException]
	override def ready( atMost: Duration )( implicit permit: CanAwait ) =
	{
		wrapped.ready( atMost )
		this
	}
}