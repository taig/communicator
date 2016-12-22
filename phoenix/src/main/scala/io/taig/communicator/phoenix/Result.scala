package io.taig.communicator.phoenix

import io.taig.communicator.phoenix.message.Response

sealed trait Result extends Product with Serializable
sealed trait Error extends Result

object Result {
    case class Success( response: Response ) extends Result
    case class Failure( response: Response ) extends Error
    case object None extends Error
}