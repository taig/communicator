package io.taig.communicator

import okhttp3.OkHttpClient

object Client {
    type Builder = OkHttpClient.Builder

    def apply() = new Client
}