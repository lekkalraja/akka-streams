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