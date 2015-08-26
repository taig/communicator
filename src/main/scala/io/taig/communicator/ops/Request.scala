package io.taig.communicator.ops

import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator
import io.taig.communicator.Parser

import scala.concurrent.ExecutionContext

trait Request {
    def request: okhttp.Request

    def start[T: Parser]()( implicit client: OkHttpClient, executor: ExecutionContext ) = communicator.Request( request )
}