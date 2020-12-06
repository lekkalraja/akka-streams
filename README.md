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
  