package graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, Graph}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import akka.{Done, NotUsed}

import scala.concurrent.Future // Brings some nice operators into scope

object GraphBasics extends App {

  implicit private val actorSystem: ActorSystem = ActorSystem("GraphBasics")

  private val source: Source[Int, NotUsed] = Source(1 to 100)
  private val incrementer: Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 1)
  private val multiplier: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 10)
  private val sink: Sink[(Int, Int), Future[Done]] = Sink.foreach[(Int, Int)](item => println(s"[${Thread.currentThread().getName}] Output : $item"))

  /**
   * First Complex Graph
   *                                   ~> Incrementer Flow
   *    Source ~> BroadCast (Fan-out)                        ~> Zip (Fan-In)  ~> Sink
   *                                   ~> Multiplier Flow
   */

  private val graph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // Here Builder is Mutable Data Structure
    import GraphDSL.Implicits._ // Brings some nice operators into scope

    // Step 2: Add the necessary components of this graph
    val broadcast = builder.add(Broadcast[Int](2)) // Fan-Out : One Source to Multiple Components
    val zip = builder.add(Zip[Int, Int]) // Fan-In Operator

    // Step 3: Typing up the components
    source ~> broadcast // source Feeds to broadcast
    broadcast.out(0) ~> incrementer ~> zip.in0 // broadcast feeds to Incrementer and then incrementer to zip
    broadcast.out(1) ~> multiplier ~> zip.in1 // broadcast feeds to Multiplier and then multiplier to zip
    zip.out ~> sink // Zip feeds to sink

    // Step 4 : Return a Closed Shape
    ClosedShape // FREEZE the Builder's shape (stop mutating builder)
  }

  // Step 1 : Setting Up the fundamentals for the graph
  private val runnableGraph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(graph)

  // runnableGraph.run()

  /**
   * Exercise : Feed a source into 2 sinks at the same time
   *                                 ~> Sink1
   * Source ~> BroadCast (Fan-Out)
   *                                ~> Sink2
   */

  private val smallSource: Source[Int, NotUsed] = Source(1 to 20)
  private val sink1: Sink[Int, Future[Done]] = Sink.foreach[Int](item => println(s"[Sink1 => ${Thread.currentThread().getName}] Got Element : $item"))
  private val sink2: Sink[Int, Future[Done]] = Sink.foreach[Int](item => println(s"[Sink2 => ${Thread.currentThread().getName}] Got Element : $item"))

  private val fanOutGraph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Int](2))
    smallSource ~> broadcast
   /* broadcast.out(0) ~> sink1
    broadcast.out(1) ~> sink2*/

    broadcast ~> sink1
    broadcast ~> sink2

    ClosedShape
  }

  private val fanOut: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(fanOutGraph)

  // fanOut.run()

  /**
   * Exercise :
   *
   *   fast source  ->                         -> sink1
   *                      merge  -> balance
   *   slow source  ->                         -> sink2
   */

  private val balancedGraph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val merge = builder.add(Merge[Int](2))
    val balance = builder.add(Balance[Int](2))

    source ~> merge.in(0)
    smallSource ~> merge.in(1)

    merge ~> balance

    balance.out(0) ~> sink1
    balance.out(1) ~> sink2
    ClosedShape
  }

  RunnableGraph.fromGraph(balancedGraph).run()

  actorSystem.terminate().onComplete(terminated => println(terminated.foreach(println)))(actorSystem.dispatcher)
}