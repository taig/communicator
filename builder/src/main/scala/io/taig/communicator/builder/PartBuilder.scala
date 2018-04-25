package io.taig.communicator.builder

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.taig.communicator.builder.extension.instance.circe._
import io.taig.communicator.OkHttpPart
import okhttp3.Headers
import okhttp3.MultipartBody.Part.{create, createFormData}

sealed trait PartBuilder extends Builder[OkHttpPart]

object PartBuilder {
  case class Body(body: RequestBodyBuilder, headers: Option[Headers])
      extends PartBuilder {
    override def build: OkHttpPart = create(headers.orNull, body.build)
  }

  case class FormKeyValue(key: String, value: String) extends PartBuilder {
    override def build: OkHttpPart = createFormData(key, value)
  }

  case class FormBody(key: String, body: RequestBodyBuilder)
      extends PartBuilder {
    override def build: OkHttpPart = createFormData(key, null, body.build)
  }

  case class FormFile(key: String, fileName: String, body: RequestBodyBuilder)
      extends PartBuilder {
    override def build: OkHttpPart = createFormData(key, fileName, body.build)
  }

  implicit val decoder: Decoder[PartBuilder] = deriveDecoder

  implicit val encoder: Encoder[PartBuilder] = deriveEncoder
}
