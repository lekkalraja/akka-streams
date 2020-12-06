package basicks

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object FirstPrinciples extends App {

  implicit private val actorSystem: ActorSystem = ActorSystem("FirstPrinciples")

  /**
   * Basic Flow : Source -> IncFlow -> Sink
   */
  private val source: Source[Int, NotUsed] = Source(1 to 10)
  private val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](element => println(s"[${Thread.currentThread().getName}] Basic Sink : $element"))
  private val incFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 1)
  private val graph: RunnableGraph[NotUsed] = source.via(incFlow).to(sink)
  graph.run()

  private val sourceWithFlow: Source[Int, NotUsed] = source.via(incFlow)
  private val flowWithSink: Sink[Int, NotUsed] = incFlow.to(sink)

  // Different ways to construct Streams(Graph)
  sourceWithFlow.to(sink).run()
  source.to(flowWithSink).run()
  source.via(incFlow).to(sink).run()

  //nulls are NOT allowed => Throws : java.lang.NullPointerException: Element must not be null, rule 2.13
  /* private val illegalSource: Source[Null, NotUsed] = Source.single(null)
   illegalSource.to(Sink.foreach(println)).run()*/

  // use Options instead
  Source.single(Option(null)).to(Sink.foreach(println)).run() // It will print None

  // Various Kinds of Sources
     // Finite Sources
  private val singleElementSource: Source[String, NotUsed] = Source.single("Single Element")
  private val iterableSource: Source[Int, NotUsed] = Source(List(1, 2, 3)) // Source.apply(List(1,2,3))
  private val emptySource: Source[Nothing, NotUsed] = Source.empty

  // private val infiniteSource: Source[Int, NotUsed] = Source(Stream.from(1))
  private val infiniteSource: Source[Int, NotUsed] = Source(LazyList.from(1))

  import scala.concurrent.ExecutionContext.Implicits.global
  private val sourceFromFuture: Source[Int, NotUsed] = Source.future(Future.apply(42))

  // Various Kinds of Sinks
  private val theMostBoringSink: Sink[Any, Future[Done]] = Sink.ignore // A Sink that will consume the stream and discard the elements
  private val foreachSink: Sink[Any, Future[Done]] = Sink.foreach(println)
  private val headSink: Sink[Int, Future[Int]] = Sink.head[Int]
  private val foldSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)((left, right) => left + right)

  // Various Kinds of Flows --> Usually mapped to collection operators but doesn't have flatMap
  private val mapFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(element => element + 2)
  private val takeFlow: Flow[Int, Int, NotUsed] = Flow[Int].take(5)

  // Syntactic Sugars

  Source(1 to 10).map(element => element + 1).runForeach(element => println(s"[${Thread.currentThread().getName}] Syntactic Sugar Graph : $element"))

  // ~~ Source(1 to 10).via(Flow[Int](element => element + 1).to(Sink.forEach(println).run()))

  /**
   * Exercise : Create a stream that takes the name of persons, then will the first 2 names with length > 5 characters
   */

  private val iteratorSource: Source[String, NotUsed] = Source.fromIterator(() => List("Raja", "Achilleas", "Radha", "Helen", "Hector").iterator)
  private val lengthFilter: Flow[String, String, NotUsed] = Flow[String].filter(name => name.length > 5)
  private val take2ElementsFlow: Flow[String, String, NotUsed] = Flow[String].take(2)
  private val namesSink: Sink[String, Future[Done]] = Sink.foreach[String](validName => println(s"1. [${Thread.currentThread().getName}] Top 2 Valid Name : $validName"))
  iteratorSource.via(lengthFilter).via(take2ElementsFlow).to(namesSink).run()

  println("======================================================")

  Source(List("Raja", "Achilleas", "Radha", "Helen", "Hector"))
    .filter(name => name.length > 5)
    .take(2)
    .runForeach(validName => println(s"2. [${Thread.currentThread().getName}] Top 2 Valid Name : $validName"))
  println("======================================================")

  actorSystem.terminate()
}