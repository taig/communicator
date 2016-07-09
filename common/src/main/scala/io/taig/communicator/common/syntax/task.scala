package io.taig.communicator.common.syntax

import io.taig.communicator.common.operation
import monix.eval.Task

import scala.language.implicitConversions

trait task {
    implicit def commonTaskSyntax[A]( task: Task[A] ): operation.task[A] = {
        new operation.task[A]( task )
    }
}

object task extends task