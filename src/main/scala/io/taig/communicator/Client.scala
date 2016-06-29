package io.taig.communicator

import okhttp3.OkHttpClient

object Client {
    type Builder = OkHttpClient.Builder

    object Builder {
        def apply() = new Builder()
    }

    def apply() = new Client
}