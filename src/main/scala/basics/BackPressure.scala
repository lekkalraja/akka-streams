package basics

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object BackPressure extends App {

  implicit private val actorSystem: ActorSystem = ActorSystem("BackPressure")

  private val fastSource: Source[Int, NotUsed] = Source(1 to 1000)
  private val slowerSink: Sink[Int, Future[Done]] = Sink.foreach[Int] { element =>
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] Consumed Element : $element")
  }

  // fastSource.to(slowerSink).run() // No BackPressure

  //  fastSource.async.to(slowerSink).run() // With BackPressure

  private val incrementFlow: Flow[Int, Int, NotUsed] = Flow[Int].map { element =>
    println(s"[${Thread.currentThread().getName}] Increment Element : $element")
    element + 1
  } // Default Buffer size : 16

  /**
   * Reactions to BackPressure (In Order) :
   * 1. Try to slow down if possible
   * 2. Buffer elements until there's more demand (Default 16)
   * 3. Drop down elements from the buffer if it overflows (By Using OverFlowStrategies)
   * 4. Tear Down/ Kill the whole stream (failure)
   */
  fastSource.async
    .via(incrementFlow).async
    .to(slowerSink)
  //  .run()

  /**
   * Overflow Strategies :
   *  - dropHead = Oldest
   *  - dropTail = Newest
   *  - dropNew = exact element to be dropped = keeps the buffer
   *  - drop = drop's the entire buffer
   *  - backpressure signal
   *  - fail
   */

  /* [ERROR] [12/12/2020 17:34:55.814] [BackPressure-akka.actor.default-dispatcher-6] [Buffer(akka://BackPressure)]
     Failing because buffer is full and overflowStrategy is: [Fail] in stream [class akka.stream.impl.fusing.Buffer$$anon$26] */
  private val failedIncrementFlow: Flow[Int, Int, NotUsed] = incrementFlow.buffer(10, OverflowStrategy.fail)
  fastSource.async
    .via(failedIncrementFlow).async
    .to(slowerSink)
    //.run()

  /**
   * 1 - 16 Nobody is Back Pressured
   * 17-26 Flow will buffer, flow will start dropping at the next element
   * 26-1000 Flow Will always drop the oldest element
   * 991 - 10000 => 992 - 1001 => Sink
   */
  private val dropHeadIncrementalFlow: Flow[Int, Int, NotUsed] = incrementFlow.buffer(10, OverflowStrategy.dropHead)
  fastSource.async
    .via(dropHeadIncrementalFlow).async
    .to(slowerSink)
    //.run()

  private val dropTailIncrementFlow: Flow[Int, Int, NotUsed] = incrementFlow.buffer(10, OverflowStrategy.dropTail)
  fastSource.async
    .via(dropTailIncrementFlow).async
    .to(slowerSink)
    //.run()

  private val dropNewIncrementFlow: Flow[Int, Int, NotUsed] = incrementFlow.buffer(10, OverflowStrategy.dropNew)
  fastSource.async
    .via(dropNewIncrementFlow).async
    .to(slowerSink)
    //.run()

  private val dropBufferIncrementFlow: Flow[Int, Int, NotUsed] = incrementFlow.buffer(10, OverflowStrategy.dropBuffer)
  fastSource.async
    .via(dropBufferIncrementFlow).async
    .to(slowerSink)
    //.run()

  private val backPressuredIncrementFlow: Flow[Int, Int, NotUsed] = incrementFlow.buffer(10, OverflowStrategy.backpressure)
  fastSource.async
    .via(backPressuredIncrementFlow).async
    .to(slowerSink)
    //.run()

  Thread.sleep(100000)
  actorSystem.terminate()
}