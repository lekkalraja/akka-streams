package basics

import akka.actor.{Actor, ActorSystem}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

/**
 * ORDERING GUARANTEES NO MATTER WHAT
 */
object OperatorFusionAndAsyncBoundaries extends App {

  implicit private val actorSystem: ActorSystem = ActorSystem("OperatorFusionAndAsyncBoundaries")

  private val simpleSource: Source[Int, NotUsed] = Source(1 to 15)

  private val adder: Flow[Int, Int, NotUsed] = Flow[Int].map { element =>
    println(s"[${Thread.currentThread().getName}] Adder for Element : $element")
    element + 1
  }
  private val multiplier: Flow[Int, Int, NotUsed] = Flow[Int].map{ element =>
    println(s"[${Thread.currentThread().getName}] Multiplier for Element : $element")
    element * 10
  }
  private val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](element => println(s"[${Thread.currentThread().getName}] Printing Element : $element"))

  // This runs on the SAME ACTOR => This is called as operator/component FUSION
  simpleSource
    .via(adder)
    .via(multiplier)
    .to(sink)
    // .run()

  // Internally when we call run() => It will define an actor and instantiate that actor and will keep send messages i.e.
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case x : Int =>
        // Flows
        println(s"[${Thread.currentThread().getName}] Adder for Element : $x")
        val x2 = x + 1
        println(s"[${Thread.currentThread().getName}] Adder for Element : $x2")
        val y = x2 * 10
        // Sink
        println(s"[${Thread.currentThread().getName}] Printing Element : $y")

    }
  }
  // private val simpleActor: ActorRef = actorSystem.actorOf(Props[SimpleActor])
  // (1 to 15).foreach(simpleActor ! _)

  private val complexAdder: Flow[Int, Int, NotUsed] = Flow[Int].map { element =>
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] Complex Adder for Element : $element")
    element + 1
  }
  private val complexMultiplier: Flow[Int, Int, NotUsed] = Flow[Int].map{ element =>
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] Complex Multiplier for Element : $element")
    element * 10
  }

  // It will take ~ 2 seconds to process each element in the stream because the entire flow will run on same Actor ~ Async Boundary
  simpleSource
    .via(complexAdder)
    .via(complexMultiplier)
    .to(sink)
   // .run()

  // To make better productivity make use of Async Boundaries i.e. make to run each component/operator in separate Actor

  simpleSource
    .via(complexAdder).async // From Here Runs on one actor => [OperatorFusionAndAsyncBoundaries-akka.actor.default-dispatcher-8] Complex Adder for Element : 1
    .via(complexMultiplier).async // From Here Runs on another Actor => [OperatorFusionAndAsyncBoundaries-akka.actor.default-dispatcher-5] Complex Multiplier for Element : 2
    .to(sink) // From Here Runs on Another Actor => [OperatorFusionAndAsyncBoundaries-akka.actor.default-dispatcher-6] Printing Element : 20
    .run()

  Thread.sleep(20000)
  actorSystem.terminate()
}