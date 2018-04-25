package io.taig.communicator.builder

import java.util.UUID

import cats.data.NonEmptyList
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.taig.communicator.builder.extension.instance.circe._
import io.taig.communicator.{OkHttpMultipartBody, OkHttpMultipartBodyBuilder}
import okhttp3.MediaType
import okhttp3.MultipartBody.FORM

case class MultipartBodyBuilder(
    parts: NonEmptyList[PartBuilder],
    contentType: MediaType = FORM,
    boundary: String = UUID.randomUUID.toString
) extends Builder[OkHttpMultipartBody] {
  override def build: OkHttpMultipartBody = {
    val builder = new OkHttpMultipartBodyBuilder(boundary)
      .setType(contentType)

    parts.toList.foreach { part ⇒
      builder.addPart(part.build)
    }

    builder.build()
  }
}

object MultipartBodyBuilder {
  implicit val decoder: Decoder[MultipartBodyBuilder] = deriveDecoder

  implicit val encoder: Encoder[MultipartBodyBuilder] = deriveEncoder
}
