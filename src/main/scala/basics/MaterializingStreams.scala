package basics

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit private val actorStream: ActorSystem = ActorSystem("MaterializingStreams")

  private val sourceMaterializedValue: Source[Int, NotUsed] = Source(List(1, 2, 3, 4, 5, 6, 7, 8))
  private val flowMaterializedValue: Flow[Int, Int, NotUsed] = Flow[Int].map(element => element * element)
  private val reducedMaterializedValue: Flow[Int, Int, NotUsed] = Flow[Int].reduce((left, right) => left + right)
  private val sinkMaterializedValue: Sink[Int, Future[Done]] = Sink.foreach[Int](sum => println(s"Total Sum of Stream : $sum"))
  private val graphMaterializedValue: RunnableGraph[NotUsed] = sourceMaterializedValue.via(flowMaterializedValue).via(reducedMaterializedValue).to(sinkMaterializedValue)
  // By-Default the final materialized value will be "LEFT MOST (Keep.left)"
  private val finalMaterializedValue: NotUsed = graphMaterializedValue.run()

  // Can Pick Wanted Materialized Value by choosing xxxMat()
  private val chosenMaterializedValue: RunnableGraph[Future[Done]] = sourceMaterializedValue
    .viaMat(flowMaterializedValue)((sourceMV, flowMV) => flowMV)
    .viaMat(reducedMaterializedValue)((flowMv, reducedMV) => reducedMV)
    .toMat(sinkMaterializedValue)((reducedMV, sinkMV) => sinkMV)
  private val chosenFinalValue: Future[Done] = chosenMaterializedValue.run()
  chosenFinalValue.onComplete {
    case Success(value) => println(s"[Chosen Materialized Value] Successfully executed stream with status : $value")
    case Failure(ex) => println(s"[Chosen Materialized Value] Failed to process the stream with : ${ex.getMessage}")
  }(ExecutionContext.global)

  // ~~ Equivalent to above statements
  private val chosenFinalValueWithKeep: Future[Done] = sourceMaterializedValue
    .viaMat(flowMaterializedValue)(Keep.right)
    .viaMat(reducedMaterializedValue)(Keep.right)
    .toMat(sinkMaterializedValue)(Keep.right)
    .run()
  chosenFinalValueWithKeep.onComplete {
    case Success(value) => println(s"[Chosen Materialized Value With Keep] Successfully executed stream with status : $value")
    case Failure(ex) => println(s"[Chosen Materialized Value With Keep]  Failed to process the stream with : ${ex.getMessage}")
  }(ExecutionContext.global)

  private val sourceMVByDefault: NotUsed = Source(List(1, 2, 3, 4, 5)).to(Sink.reduce[Int]((a, b) => a + b)).run()
  private val reduceVale: Future[Int] = Source(List(1, 2, 3, 4, 5)).toMat(Sink.reduce[Int]((a, b) => a + b))(Keep.right).run()
  reduceVale.onComplete{
    case Success(value) => println(s"[Reduced Value] Successfully executed stream with status : $value")
    case Failure(ex) => println(s"[Reduced Value]  Failed to process the stream with : ${ex.getMessage}")
  }(actorStream.dispatcher)

  private val source: Source[Int, NotUsed] = Source(1 to 10)
  private val sink: Sink[Int, Future[Int]] = Sink.reduce[Int]((a, b) => a + b)
  private val eventualInt: Future[Int] = source.runWith(sink)
  eventualInt.onComplete{
    case Success(value) => println(s"[Reduced With Sink Value] Successfully executed stream with status : $value")
    case Failure(ex) => println(s"[Reduced With Sink Value]  Failed to process the stream with : ${ex.getMessage}")
  }(actorStream.dispatcher)

  // Syntactic sugars
  Source(1 to 10)
    .toMat(Sink.reduce[Int]( _ + _))(Keep.right)
    .run()
    .onComplete{
      case Success(value) => println(s"[Reduced With Sugar-1] Successfully executed stream with status : $value")
      case Failure(ex) => println(s"[Reduced With Sugar-1]  Failed to process the stream with : ${ex.getMessage}")
    }(actorStream.dispatcher)

  Source(1 to 10)
    .runWith(Sink.reduce[Int]( _ + _)) // ~~ toMat(sink)(Keep.right).run()
    .onComplete{
      case Success(value) => println(s"[Reduced With Sugar-2] Successfully executed stream with status : $value")
      case Failure(ex) => println(s"[Reduced With Sugar-2]  Failed to process the stream with : ${ex.getMessage}")
    }(actorStream.dispatcher)

  Source(1 to 10)
    .runReduce( _ + _) // ~~ same as Above  i.e. runWith(Sink.reduce(f))
    .onComplete{
      case Success(value) => println(s"[Reduced With Sugar-3] Successfully executed stream with status : $value")
      case Failure(ex) => println(s"[Reduced With Sugar-3]  Failed to process the stream with : ${ex.getMessage}")
    }(actorStream.dispatcher)

  // backwards
  Sink.foreach[String](println).runWith(Source.single("Backward Streaming"))

  // Both Ways
  Flow[Int].map( element => element * 2).runWith(Source(List(1,2,3)), Sink.foreach[Int](element => println(s"Both Ways Streaming : $element")))

  /**
   * Exercise :
   *  Return the last element out of a source with differnt ways
   */
  private val namesSource: Source[String, NotUsed] = Source(List("first-1", "middle-1", "last-1"))
  private val lastElementSink: Sink[String, Future[String]] = Sink.last[String]
  private val lastElementGraph: RunnableGraph[Future[String]] = namesSource.toMat(lastElementSink)(Keep.right)
  private val lastElement: Future[String] = lastElementGraph.run()
  lastElement.onComplete{
    case Success(element) => println(s"[Exercise-1-1] Stream Processed successfully and found last element is : $element")
    case Failure(exception) => println(s"[Exercise-1-1] Stream Failed to pick last element with details $exception")
  }(actorStream.dispatcher)

  Source(List("first-2", "middle-2", "last-2"))
    .toMat(Sink.last[String])(Keep.right)
    .run()
    .onComplete{
      case Success(element) => println(s"[Exercise-1-2] Stream Processed successfully and found last element is : $element")
      case Failure(exception) => println(s"[Exercise-1-2] Stream Failed to pick last element with details $exception")
    }(actorStream.dispatcher)

  Source(List("first-3", "middle-3", "last-3"))
    .runWith(Sink.last[String])
    .onComplete{
      case Success(element) => println(s"[Exercise-1-3] Stream Processed successfully and found last element is : $element")
      case Failure(exception) => println(s"[Exercise-1-3] Stream Failed to pick last element with details $exception")
    }(actorStream.dispatcher)

  Source(List("first-4", "middle-4", "last-4"))
    .runReduce[String]( (_, right) => right)
    .onComplete{
      case Success(element) => println(s"[Exercise-1-4] Stream Processed successfully and found last element is : $element")
      case Failure(exception) => println(s"[Exercise-1-4] Stream Failed to pick last element with details $exception")
    }(actorStream.dispatcher)

  /**
   * Exercise :
   *  Compute the total word count out of a stream of sentences
   */

  private val linesSource: Source[String, NotUsed] = Source(List("Akka Is a tool Kit", "Contains Actor's, Streams, Persistence, HTTP, etc..."))

  linesSource
    .map(line => line.split(" ").length)
    .runReduce( (left, right) => left + right)
    .onComplete {
      case Success(total) => println(s"[Exercise-2-1] Total Word count of stream of lines is : $total")
      case Failure(exception) => println(s"[Exercise-2-1] Failed to Process the stream : ${exception.getMessage}")
    }(actorStream.dispatcher)

  linesSource
    .fold(0)( (currentCount, newLine) => currentCount + newLine.split(" ").length)
    .runWith(Sink.head[Int])
    .onComplete {
      case Success(total) => println(s"[Exercise-2-2] Total Word count of stream of lines is : $total")
      case Failure(exception) => println(s"[Exercise-2-2] Failed to Process the stream : ${exception.getMessage}")
    }(actorStream.dispatcher)

  linesSource
    .runFold(0)( (currentCount, newLine) => currentCount + newLine.split(" ").length)
    .onComplete {
      case Success(total) => println(s"[Exercise-2-3] Total Word count of stream of lines is : $total")
      case Failure(exception) => println(s"[Exercise-2-3] Failed to Process the stream : ${exception.getMessage}")
    }(actorStream.dispatcher)

  linesSource
    .toMat(Sink.fold[Int, String](0)( (currentCount, newLine) => currentCount + newLine.split(" ").length))(Keep.right)
    .run()
    .onComplete {
      case Success(total) => println(s"[Exercise-2-4] Total Word count of stream of lines is : $total")
      case Failure(exception) => println(s"[Exercise-2-4] Failed to Process the stream : ${exception.getMessage}")
    }(actorStream.dispatcher)

  linesSource
    .runWith(Sink.fold[Int, String](0)( (currentCount, newLine) => currentCount + newLine.split(" ").length))
    .onComplete {
      case Success(total) => println(s"[Exercise-2-4] Total Word count of stream of lines is : $total")
      case Failure(exception) => println(s"[Exercise-2-4] Failed to Process the stream : ${exception.getMessage}")
    }(actorStream.dispatcher)

  actorStream.terminate()
}