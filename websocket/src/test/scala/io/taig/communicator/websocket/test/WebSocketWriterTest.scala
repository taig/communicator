//package io.taig.communicator.websocket.test
//
//import java.io.IOException
//
//import io.backchat.hookup.{ Connected, Disconnected, TextMessage }
//import io.taig.communicator.OkHttpRequest
//import io.taig.communicator.test.Suite
//import io.taig.communicator.websocket._
//import monix.eval.Task
//import monix.execution.Scheduler.Implicits.global
//
//import scala.language.postfixOps
//import scala.util.Try
//
//class WebSocketWriterTest
//        extends Suite
//        with SocketServer {
//    override def receive = {
//        case Connected           ⇒ send( "0" )
//        case TextMessage( text ) ⇒ send( text )
//        case Disconnected( _ )   ⇒ //
//    }
//
//    it should "connect to a websocket" in {
//        WebSocketWriter[String]( request ).map { writer ⇒
//            writer.close( Close.GoingAway, None )
//            writer shouldBe a[WebSocketWriter[_]]
//        }.runAsync
//    }
//
//    it should "allow to send a payload" in {
//        WebSocketWriter[String]( request ).map { writer ⇒
//            writer.send( "foobar" )
//            writer.close( Close.GoingAway, None )
//            writer shouldBe a[WebSocketWriter[_]]
//        }.runAsync
//    }
//
//    it should "allow to send a ping" in {
//        WebSocketWriter[String]( request ).map { writer ⇒
//            writer.ping()
//            writer.ping( Some( "foobar" ) )
//            writer.close( Close.GoingAway, None )
//            writer shouldBe a[WebSocketWriter[_]]
//        }.runAsync
//    }
//
//    it should "have no effect when the socket is closed multiple times" in {
//        WebSocketWriter[String]( request ).map { writer ⇒
//            writer.close( Close.GoingAway, None )
//            writer.close( Close.GoingAway, None )
//            writer.close( Close.GoingAway, None )
//            writer shouldBe a[WebSocketWriter[_]]
//        }.runAsync
//    }
//
//    it should "fail to write to a closed websocket" in {
//        WebSocketWriter[String]( request ).map { writer ⇒
//            writer.send( "foo" )
//            writer.close( Close.GoingAway, Some( "Bye." ) )
//            Try( writer.send( "bar" ) ).failed.get shouldBe a[IllegalStateException]
//        }.runAsync
//    }
//
//    it should "fail with an unknown url" in {
//        WebSocketWriter[String] {
//            new OkHttpRequest.Builder()
//                .url( "ws://externalhost/ws" )
//                .build()
//        }.runAsync.failed.map {
//            _ shouldBe an[IOException]
//        }
//    }
//
//    it should "work synchronously with a buffered connection" in {
//        val writer = BufferedWebSocketWriter[String]()
//        writer.send( "foobar" )
//        writer.close( Close.Normal, None )
//        writer shouldBe a[BufferedWebSocketWriter[_]]
//    }
//
//    it should "buffer messages during an unexpected disconnect" in {
//        Task.create[List[String]] { ( scheduler, callback ) ⇒
//            val writer = BufferedWebSocketWriter[String]()
//
//            val messages = collection.mutable.ListBuffer[String]()
//
//            val listener = new WebSocketListener[String] {
//                override def onMessage( message: String ) = {
//                    messages += message
//                }
//
//                override def onPong( payload: Option[String] ) = {}
//
//                override def onClose( code: Int, reason: Option[String] ) = {
//                    callback.onSuccess( messages.toList )
//                }
//
//                override def onFailure( exception: IOException, response: Option[String] ) = {}
//            }
//
//            writer.send( "foo" )
//
//            WebSocket[String]( request )( listener ).runAsync( scheduler ).map {
//                case ( socket, _ ) ⇒
//                    writer.connect( socket )
//                    writer.disconnect()
//                    writer.send( "bar" )
//                    writer.connect( socket )
//                    writer.close( Close.Normal, Some( "Bye." ) )
//            }
//
//        }.runAsync.map {
//            _ should contain theSameElementsAs ( "0" :: "foo" :: "bar" :: Nil )
//        }
//    }
//}