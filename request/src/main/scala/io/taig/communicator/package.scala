package io.taig

package object communicator {
  type OkHttpRequest = okhttp3.Request
  type OkHttpRequestBuilder = okhttp3.Request.Builder

  type OkHttpResponse = okhttp3.Response

  type OkHttpRequestBody = okhttp3.RequestBody

  type OkHttpResponseBody = okhttp3.ResponseBody

  type OkHttpMultipartBody = okhttp3.MultipartBody
  type OkHttpMultipartBodyBuilder = okhttp3.MultipartBody.Builder

  type OkHttpPart = okhttp3.MultipartBody.Part

  type OkHttpWebSocket = okhttp3.WebSocket

  type OkHttpWebSocketListener = okhttp3.WebSocketListener
}
