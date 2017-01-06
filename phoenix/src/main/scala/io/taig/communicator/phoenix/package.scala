package io.taig.communicator

import io.taig.phoenix.models
import org.slf4j.LoggerFactory

package object phoenix {
    private[phoenix] val logger = LoggerFactory.getLogger( "phoenix" )

    type Event = models.Event
    val Event = models.Event

    type Inbound = models.Inbound
    val Inbound = models.Inbound

    type Push = models.Push
    val Push = models.Push

    type Ref = models.Ref
    val Ref = models.Ref

    type Request = models.Request
    val Request = models.Request

    type Response = models.Response
    val Response = models.Response

    type Topic = models.Topic
    val Topic = models.Topic
}