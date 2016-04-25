package io.taig

import scala.concurrent.Future
import scala.language.implicitConversions

package object communicator {
    implicit private[communicator] def `Function0 -> Unit -> Runnable`( f: ⇒ Unit ): Runnable = new Runnable {
        override def run() = f
    }

    implicit class RichBuilder( builder: okhttp3.Request.Builder )
            extends ops.Request {
        override val request = builder.build()
    }

    implicit class RichRequest( val request: okhttp3.Request )
        extends ops.Request

    implicit class RichFuture[T]( val future: Future[T] )
        extends ops.Future[T]
}