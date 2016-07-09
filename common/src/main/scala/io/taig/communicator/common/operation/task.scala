package io.taig.communicator.common.operation

import monix.eval.Task

final class task[A]( task: Task[A] ) {
    def filter( f: A ⇒ Boolean ): Task[A] = {
        task.map { a ⇒
            if ( f( a ) ) a else throw new NoSuchElementException( "Task.filter predicate is not satisfied" )
        }
    }

    def withFilter( f: A ⇒ Boolean ): Task[A] = filter( f )
}