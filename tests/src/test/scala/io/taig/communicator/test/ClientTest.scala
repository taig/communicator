package io.taig.communicator.test

import io.taig.communicator.Client
import okhttp3.OkHttpClient

class ClientTest extends Suite {
    it should "apply the OkHttpClient constructor" in {
        Client() shouldBe an[OkHttpClient]
    }

    it should "apply the OkHttpClient.Builder constructor" in {
        Client.Builder() shouldBe an[OkHttpClient.Builder]
    }
}