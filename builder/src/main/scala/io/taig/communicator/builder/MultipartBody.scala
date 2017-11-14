package io.taig.communicator.builder

import java.util.UUID

import cats.data.NonEmptyList
import io.taig.communicator.OkHttpMultipartBody
import okhttp3.MediaType
import okhttp3.MultipartBody.FORM

case class MultipartBody(
    parts: NonEmptyList[Part],
    contentType: MediaType = FORM,
    boundary: String = UUID.randomUUID.toString
) extends Builder[OkHttpMultipartBody] {
  override def build: OkHttpMultipartBody = {
    val builder = new OkHttpMultipartBody.Builder(boundary)
      .setType(contentType)

    parts.toList.foreach { part ⇒
      builder.addPart(part.build)
    }

    builder.build()
  }
}
