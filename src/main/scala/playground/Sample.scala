package playground

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.ExecutionContext

object Sample extends App {

  implicit private val akkaStreams: ActorSystem = ActorSystem("AkkaStreams")

  Source.single("Hello, Akka Streams!").to(Sink.foreach(println)).run()
  akkaStreams.terminate().onComplete(println)(ExecutionContext.global)
}