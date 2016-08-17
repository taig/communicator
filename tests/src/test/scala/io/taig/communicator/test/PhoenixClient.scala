//package io.taig.communicator
//
//import io.taig.communicator.phoenix.Phoenix
//import monix.execution.Scheduler.Implicits.global
//import monix.reactive.OverflowStrategy
//import org.scalatest.BeforeAndAfterEach
//
//trait PhoenixClient
//        extends SocketServer
//        with BeforeAndAfterEach { this: org.scalatest.Suite ⇒
//    var phoenix: Phoenix = null
//
//    override def beforeEach() = {
//        super.beforeEach()
//
//        phoenix = Phoenix( request, OverflowStrategy.Unbounded )
//    }
//
//    override def afterEach() = {
//        super.afterEach()
//
//        phoenix.close()
//    }
//}