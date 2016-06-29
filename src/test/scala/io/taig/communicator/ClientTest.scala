package io.taig.communicator

import okhttp3.OkHttpClient

class ClientTest extends Suite {
    it should "apply the OkHttpClient constructor" in {
        Client() shouldBe an[OkHttpClient]
    }

    it should "apply the OkHttpClient.Builder constructor" in {
        Client.Builder() shouldBe an[OkHttpClient.Builder]
    }
}