//package io.taig.communicator.phoenix
//
//import io.circe.Json
//import io.circe.generic.auto._
//import io.circe.syntax._
//import io.taig.communicator._
//import io.taig.communicator.phoenix.message.Response.Payload
//import io.taig.communicator.phoenix.message.{ Request, Response }
//import io.taig.communicator.websocket.{ Close, WebSocketChannels }
//import io.taig.communicator.websocket.WebSocketChannels.Sender
//import monix.eval.Task
//import monix.execution.Scheduler
//import monix.reactive.OverflowStrategy
//
//import scala.concurrent.duration._
//import scala.language.postfixOps
//
//class Phoenix(
//        private[phoenix] val websocket: WebSocketChannels[Request, Response],
//        heartbeat:                      Option[Duration],
//        reconnect:                      Option[Duration]
//) {
//    private val iterator: Iterator[Ref] = {
//        Stream.iterate( 0L )( _ + 1 ).map( Ref( _ ) ).iterator
//    }
//
//    private[phoenix] def ref = synchronized( iterator.next() )
//
//    private[phoenix] def withRef[T]( f: Ref ⇒ T ): T = f( ref )
//
//    def join( topic: Topic, payload: Json = Json.Null ): Task[Channel] = withRef { ref ⇒
//        val send = Task {
//            logger.info( s"Requesting to join channel $topic" )
//            val request = Request( topic, Event.Join, payload, ref )
//            websocket.sender.send( request )
//        }
//
//        val receive = websocket.receiver.collect {
//            case Response( `topic`, _, Payload( "ok", _ ), `ref` ) ⇒
//                logger.info( s"Successfully joined channel $topic" )
//                new Channel( this, topic )
//        }.firstL
//
//        for {
//            _ ← send
//            receive ← receive
//        } yield receive
//    }
//
//    def close(): Unit = {
//        logger.debug( "Closing" )
//
//        websocket.close()
//    }
//}
//
//object Phoenix {
//    def apply(
//        request:   OkHttpRequest,
//        strategy:  OverflowStrategy.Synchronous[Json],
//        heartbeat: Option[Duration]                   = Some( 7 seconds ),
//        reconnect: Option[Duration]                   = Some( 5 seconds )
//    )(
//        implicit
//        c: Client,
//        s: Scheduler
//    ): Phoenix = {
//        val websocket = WebSocketChannels[Json]( request, strategy )
//
//        new Phoenix(
//            new WebSocketChannels[Request, Response] {
//                override val sender = new Sender[Request] {
//                    override def send( value: Request ) = {
//                        websocket.sender.send( value.asJson )
//                    }
//
//                    override def ping( value: Option[Request] ) = {
//                        websocket.sender.ping( value.map( _.asJson ) )
//                    }
//
//                    override def close( code: Int, reason: String ) = {
//                        websocket.sender.close( code, reason )
//                    }
//                }
//
//                override val receiver = {
//                    websocket.receiver.map( _.as[Response].valueOr( throw _ ) )
//                }
//
//                override def close() = {
//                    websocket.sender.close( Close.GoingAway, "Bye." )
//                }
//            },
//            heartbeat,
//            reconnect
//        )
//    }
//}