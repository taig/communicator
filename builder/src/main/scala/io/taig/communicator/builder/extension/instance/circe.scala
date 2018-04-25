package io.taig.communicator.builder.extension.instance

import java.io.File

import cats.syntax.either._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.taig.communicator.OkHttpRequestBody
import io.taig.communicator.builder._
import okhttp3.{Headers, HttpUrl, MediaType}

import scala.collection.JavaConverters._

trait circe {
  implicit val decoderFile: Decoder[File] = Decoder[String].map(new File(_))

  implicit val encoderFile: Encoder[File] =
    Encoder[String].contramap(_.getAbsolutePath)

  implicit val decoderHeaders: Decoder[Headers] =
    for {
      data <- Decoder[Map[String, List[String]]]
      headers = data.foldLeft(new Headers.Builder) {
        case (builder, (key, values)) ⇒
          values.foldLeft(builder)(_.add(key, _))
      }
    } yield headers.build()

  implicit val encoderHeaders: Encoder[Headers] =
    Encoder[Map[String, List[String]]]
      .contramap(_.toMultimap.asScala.toMap.mapValues(_.asScala.toList))

  implicit val decoderHttpUrl: Decoder[HttpUrl] =
    Decoder[String].map(HttpUrl.parse)

  implicit val encoderHttpUrl: Encoder[HttpUrl] =
    Encoder[String].contramap(_.toString)

  implicit val decoderMediaType: Decoder[MediaType] =
    Decoder[String].map(MediaType.parse)

  implicit val encoderMediaType: Encoder[MediaType] =
    Encoder[String].contramap(_.toString)

  implicit val decoderBuilderOkHttpRequestBody
    : Decoder[Builder[OkHttpRequestBody]] =
    Decoder.instance[Builder[OkHttpRequestBody]] { cursor ⇒
      cursor.get[Int]("type").flatMap {
        case 0 ⇒ cursor.get[RequestBodyBuilder]("body")
        case 1 ⇒ cursor.get[MultipartBodyBuilder]("body")
        case tpe ⇒ Left(DecodingFailure(s"Unknown type $tpe", cursor.history))
      }
    }

  implicit val encoderBuilderOkHttpRequestBody
    : Encoder[Builder[OkHttpRequestBody]] =
    Encoder.instance[Builder[OkHttpRequestBody]] {
      case request: RequestBodyBuilder ⇒
        Json.obj(
          "type" → 0.asJson,
          "body" → request.asJson
        )
      case multipart: MultipartBodyBuilder ⇒
        Json.obj(
          "type" → 1.asJson,
          "body" → multipart.asJson
        )
    }
}

object circe extends circe
