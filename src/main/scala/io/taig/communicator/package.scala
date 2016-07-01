package io.taig

import okhttp3.OkHttpClient

package object communicator {
    type Client = OkHttpClient

    object Client {
        type Builder = OkHttpClient.Builder

        object Builder {
            def apply(): Builder = new Builder()
        }

        def apply(): Client = new Client
    }
}