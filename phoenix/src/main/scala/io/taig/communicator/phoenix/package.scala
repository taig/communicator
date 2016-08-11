package io.taig.communicator

import io.circe.Json
import okhttp3.RequestBody
import okhttp3.ws.WebSocket.TEXT

package object phoenix {
    def request( json: Json ) = RequestBody.create( TEXT, json.noSpaces )
}