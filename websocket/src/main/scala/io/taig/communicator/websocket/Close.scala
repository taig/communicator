package io.taig.communicator.websocket

object Close {
    val Normal = 1000

    val GoingAway = 1001

    val ProtocolError = 1002

    val Unsupported = 1003

    val UnsupportedData = 1007
}