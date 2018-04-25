package io.taig.communicator.builder

import io.taig.communicator.OkHttpPart
import okhttp3.Headers
import okhttp3.MultipartBody.Part.{create, createFormData}

sealed trait PartBuilder extends BuilderBuilder[OkHttpPart]

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
    override def build: OkHttpPart = {
      createFormData(key, null, body.build)
    }
  }

  case class FormFile(key: String, fileName: String, body: RequestBodyBuilder)
      extends PartBuilder {
    override def build: OkHttpPart = {
      createFormData(key, fileName, body.build)
    }
  }
}
