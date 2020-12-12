# akka-streams

* `Akka Streams` is an implementation of Reactive Streams (http://www.reactive-streams.org/)
* Reactive Streams is a Specification of asynchronous stream processing with non-blocking pressure
* Reactive Streams API Consists of the following components that are required to by It's implementations
  - Publisher --> Emits Unbounded elements (asynchronously)
  - Subscriber --> Receives elements
  - Subscription
  - Processor --> Transforms elements along the way
  - async
  - back-pressure
## Akka-Streams (Implementation of Reactive Streams)
  - Source ~ Publisher : Emits elements asynchronously, may or may not terminate
  - Sink ~  Subscriber : Receives elements, terminates only when the publisher terminates
  - Flow ~ Processor : Transforms Elements 
  * Build Streams by connection components (Operators)
  - Directions 
    - upstream -> towards the source - - - - -> Source
    - Downstream -> towards the sink <- - - - -
  * `Graph` : Upstream --> Source --> Flow --> Flow --> Sink
  
#### Materializing Value's
* Getting a meaningful value out of a running Stream
* Components are static until they run 
```
 val graph = source.via(flow).to(sink)
 val result = graph.run() ==> result -> Materialized value
```
* A graph is a `blueprint` for a stream
* Running a graph allocates the right resources
  * Creating actor's
  * Allocation thread pools, sockets, connections, etc... everything is transparent 

* Running a graph is `Materializing`
* Materializing a graph = materializing all components in that graph
* Each component produces a materialized value when run
* the graph produces a `single` materialized value (by default leftmost component value `Keep.left`)
    * can change by using `xxxMat(Keep.xxx())`,  Keep contains #left, #right, #both (left, right), #none
    * our job to choose which one to pick
    
* A component can materialize multiple times
    * you can reuse the same component in different graphs
    * different runs = different materialization's!
* A materialized value can be `ANYTHING`
    * NotUsed -> combination of scala's Unit & Java's Void
    * Future[Any]
    * etc...
    
### Operator Fusion And Async Boundaries

* Akka Stream Components fused by default => The entire graph will run on the same actor
* To make sure each component to run on a separate actor introduce Async boundaries

```
  private val simpleSource: Source[Int, NotUsed] = Source(1 to 15)
  private val adder: Flow[Int, Int, NotUsed] = Flow[Int].map { element =>
    println(s"[${Thread.currentThread().getName}] Adder for Element : $element")
    element + 1
  }
  private val multiplier: Flow[Int, Int, NotUsed] = Flow[Int].map{ element =>
    println(s"[${Thread.currentThread().getName}] Adder for Element : $element")
    element * 10
  }
  private val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](element => println(s"[${Thread.currentThread().getName}] Printing Element : $element"))

  // This runs on the SAME ACTOR => This is called as operator/component FUSION
  private val used: NotUsed = simpleSource
    .via(adder)
    .via(multiplier)
    .to(sink)
    .run()
```

* The above stream will turn into below model when we materialize(call runXXX()) the static graph(stream)

```
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
  private val simpleActor: ActorRef = actorSystem.actorOf(Props[SimpleActor])
  (1 to 15).foreach(simpleActor ! _)
``` 

#### Async boundaries
```
source.async
      .via(Flow[Int].map(x => x + 1)).async
      .via(Flow[Int].map(x => x * 1)).async
      .to(Sink.foreach(println))
```
    * Components run on different actors
    * better throughput
* Best when : Individual operations are expensive
* Avoid when : Operations are comparable with a message pass
* Irrespective of Async or not, streams `always guarantees Ordering`

```
 simpleSource
    .via(complexAdder).async // From Here Runs on one actor => [OperatorFusionAndAsyncBoundaries-akka.actor.default-dispatcher-8] Complex Adder for Element : 1
    .via(complexMultiplier).async // From Here Runs on another Actor => [OperatorFusionAndAsyncBoundaries-akka.actor.default-dispatcher-5] Complex Multiplier for Element : 2
    .to(sink) // From Here Runs on Another Actor => [OperatorFusionAndAsyncBoundaries-akka.actor.default-dispatcher-6] Printing Element : 20
    .run()
```
* An async boundary contains
    * Everything from the previous boundary (if any)
    * Everything between the previous boundary and current boundary
* Communication based on actor messages

### BackPressure
* One of the fundamental features of `Reactive Streams`
* Elements flow as response to demand from consumers
* Fast Consumers : all is well
* Slow Consumers: Problem
    * consumer will send a signal to producer to slow down using Backpressure Protocol
* Akka Streams can slow down fast producers
* Backpressure protocol is transparent