package io.taig.communicator

import java.io.IOException

import monix.eval.Task
import monix.execution.Cancelable
import okhttp3.{ Call, Callback }
import scala.language.implicitConversions

final class Request private ( task: Task[Response] ) {
    /**
     * Transform the Request's InputStream to an instance of T
     *
     * An implicit Parser[T] has to be in scope.
     *
     * @tparam T
     * @return Task that parses the response body
     */
    def parse[T: Parser]: Task[Response.With[T]] = task.map { response ⇒
        val content = Parser[T].parse( response, response.wrapped.body().byteStream() )
        response.withBody( content )
    }

    /**
     * Ignore the server response (and close the InputStream right away)
     *
     * Calling methods of monix.Task on a Request instance (e.g. Request.map) will implicitly call this method to
     * convert the Request to a Task (and therefore close the response InputStream).
     *
     * @return Task that ignores the response body
     */
    def ignoreBody: Task[Response] = task.map { response ⇒
        response.wrapped.close()
        response
    }

    /**
     * Get the raw Task instance
     *
     * When calling this method it is necessary to handle and close the InputStream manually. You are discouraged to
     * use this method and should only do so with damn good reasons.
     *
     * @return Task with an untouched Response object
     */
    def unsafeToTask: Task[Response] = task
}

object Request {
    type Builder = okhttp3.Request.Builder

    object Builder {
        def apply() = new Builder()
    }

    implicit def requestToTask( request: Request ): Task[Response] = request.ignoreBody

    def apply( request: okhttp3.Request )( implicit c: Client ): Request = {
        val task = Task.create[Response] { ( _, taskCallback ) ⇒
            val call = c.newCall( request )

            val requestCallback = new Callback {
                override def onResponse( call: Call, response: okhttp3.Response ) = {
                    try {
                        taskCallback.onSuccess( Response( response ) )
                    } catch {
                        case exception: Throwable ⇒ taskCallback.onError( exception )
                    }
                }

                override def onFailure( call: Call, exception: IOException ) = {
                    taskCallback.onError( exception )
                }
            }

            call.enqueue( requestCallback )

            Cancelable { () ⇒
                requestCallback.onFailure( call, new IOException( "Canceled" ) )
                call.cancel()
            }
        }

        new Request( task )
    }
}