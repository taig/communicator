package io.taig.communicator.builder

import io.taig.communicator.OkHttpPart
import okhttp3.Headers
import okhttp3.MultipartBody.Part.{ create, createFormData }

sealed trait Part extends Builder[OkHttpPart]

object Part {
    case class Body( body: RequestBody, headers: Option[Headers] ) extends Part {
        override def build: OkHttpPart = create( headers.orNull, body.build )
    }

    case class FormKeyValue( key: String, value: String ) extends Part {
        override def build: OkHttpPart = createFormData( key, value )
    }

    case class FormBody( key: String, body: RequestBody ) extends Part {
        override def build: OkHttpPart = {
            createFormData( key, null, body.build )
        }
    }

    case class FormFile( key: String, fileName: String, body: RequestBody ) extends Part {
        override def build: OkHttpPart = {
            createFormData( key, fileName, body.build )
        }
    }
}