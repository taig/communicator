package io.taig

import scala.language.implicitConversions

package object communicator {
    implicit private[communicator] def function0ToRunnable( f: () ⇒ Unit ): Runnable = {
        new Runnable {
            override def run() = f()
        }
    }

    type OkHttpRequest = okhttp3.Request

    object OkHttpRequest {
        type Builder = okhttp3.Request.Builder
    }
}