package io.taig.communicator.builder

import java.io.File

import io.taig.communicator.OkHttpRequestBody
import io.taig.communicator.builder.RequestBodyBuilder.Content
import okhttp3.MediaType
import okhttp3.RequestBody.create

case class RequestBodyBuilder(
    contentType: MediaType,
    content: Content
) extends BuilderBuilder[OkHttpRequestBody] {
  override def build: OkHttpRequestBody = content match {
    case Content.Text(value) ⇒ create(contentType, value)
    case Content.Bytes(value) ⇒ create(contentType, value)
    case Content.Reference(file) ⇒ create(contentType, file)
  }
}

object RequestBodyBuilder {
  sealed trait Content

  object Content {
    case class Text(value: String) extends Content
    case class Bytes(value: Array[Byte]) extends Content
    case class Reference(file: File) extends Content
  }
}
