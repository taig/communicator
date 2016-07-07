package io.taig

import okhttp3.OkHttpClient

import scala.language.implicitConversions

package object communicator {
    implicit private[communicator] def function0ToRunnable( f: () ⇒ Unit ): Runnable = {
        new Runnable {
            override def run() = f()
        }
    }

    type Client = OkHttpClient

    object Client {
        type Builder = OkHttpClient.Builder

        object Builder {
            def apply(): Builder = new Builder()
        }

        def apply(): Client = new Client
    }
}