package io.taig.communicator

import monix.eval.Task
import monix.execution.Cancelable
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
        val content = Parser[T].parse( response, response.wrapped.body )
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
     * use this method and should only do so with good reasons.
     *
     * @return Task with an untouched Response object
     */
    def unsafeToTask: Task[Response] = task
}

object Request {
    type Builder = okhttp3.Request.Builder

    object Builder {
        def apply(): Builder = new Builder()
    }

    implicit def requestToTask( request: Request ): Task[Response] = request.ignoreBody

    def apply( request: okhttp3.Request )( implicit c: Client ): Request = {
        val task = Task.create[Response] { ( scheduler, callback ) ⇒
            val call = c.newCall( request )

            scheduler.execute {
                new Runnable {
                    override def run() = {
                        try {
                            val response = call.execute()
                            callback.onSuccess( Response( response ) )
                        } catch {
                            case exception: Throwable ⇒ callback.onError( exception )
                        }
                    }
                }
            }

            Cancelable { () ⇒ call.cancel() }
        }

        new Request( task )
    }
}