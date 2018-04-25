package io.taig.communicator.builder

import io.taig.communicator.{OkHttpRequest, OkHttpRequestBody}
import io.taig.communicator.builder.RequestBuilder.Method
import io.taig.communicator.builder.RequestBuilder.Method.{
  PermitsRequestBody,
  RequiresRequestBody
}
import okhttp3.HttpUrl

case class RequestBuilder(
    url: HttpUrl,
    headers: Map[String, String] = Map.empty,
    method: Method = Method.GET,
    body: Option[BuilderBuilder[OkHttpRequestBody]] = None
) extends BuilderBuilder[OkHttpRequest] {
  def addHeader(key: String, value: String): RequestBuilder = {
    copy(headers = headers + (key → value))
  }

  def removeHeader(key: String): RequestBuilder = {
    copy(headers = headers - key)
  }

  def delete: RequestBuilder = method(Method.DELETE)

  def delete(body: BuilderBuilder[OkHttpRequestBody]): RequestBuilder = {
    method(Method.DELETE, body)
  }

  def get: RequestBuilder = method(Method.GET)

  def head: RequestBuilder = method(Method.HEAD)

  def patch(body: BuilderBuilder[OkHttpRequestBody]): RequestBuilder = {
    method(Method.PATCH, body)
  }

  def post(body: BuilderBuilder[OkHttpRequestBody]): RequestBuilder = {
    method(Method.POST, body)
  }

  def put(body: BuilderBuilder[OkHttpRequestBody]): RequestBuilder = {
    method(Method.PUT, body)
  }

  def method(value: Method with PermitsRequestBody): RequestBuilder = {
    copy(method = value, body = None)
  }

  def method(
      value: Method with RequiresRequestBody,
      body: BuilderBuilder[OkHttpRequestBody]
  ): RequestBuilder = {
    copy(method = value, body = Some(body))
  }

  override def build: OkHttpRequest = {
    val builder = new OkHttpRequest.Builder()
      .url(url)
      .method(method.name, body.map(_.build).orNull)

    headers.foreach {
      case (key, value) ⇒ builder.addHeader(key, value)
    }

    builder.build()
  }
}

object RequestBuilder {
  sealed abstract class Method(val name: String) {
    override def toString = name
  }

  object Method {
    sealed trait RequiresRequestBody
    sealed trait PermitsRequestBody

    case object DELETE
        extends Method("DELETE")
        with PermitsRequestBody
        with RequiresRequestBody
    case object GET extends Method("GET") with PermitsRequestBody
    case object HEAD extends Method("HEAD") with PermitsRequestBody
    case object PATCH extends Method("PATCH") with RequiresRequestBody
    case object POST extends Method("POST") with RequiresRequestBody
    case object PUT extends Method("PUT") with RequiresRequestBody

    case class WithRequestBody(override val name: String)
        extends Method(name)
        with RequiresRequestBody

    case class WithoutRequestBody(override val name: String)
        extends Method(name)
        with PermitsRequestBody

    case class WithOrWithoutRequestBody(override val name: String)
        extends Method(name)
        with RequiresRequestBody
        with PermitsRequestBody
  }
}
