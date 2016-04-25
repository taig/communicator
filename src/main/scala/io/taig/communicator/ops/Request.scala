package io.taig.communicator.ops

import io.taig.communicator
import io.taig.communicator.Parser
import okhttp3.OkHttpClient

import scala.concurrent.ExecutionContext

trait Request {
    def request: okhttp3.Request

    def start[T: Parser]()( implicit client: OkHttpClient, executor: ExecutionContext ) = communicator.Request( request )
}