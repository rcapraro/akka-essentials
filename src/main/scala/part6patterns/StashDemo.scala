package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /*
    Resource Actor:
      - open => it can receive read/write requests to the resource
      - otherwise => it can postpone all read/write requests until the state is open

    Resource Actor is close
      - Open => switch to the open state
      - Read, write messages are POSTPONED

    Resource Actor is open
      - read, Write are handled
      - Close => switch to the closed state

    [Open, read, Read, Write]
      - switch to the open state
      - read the data
      - read the data again
      - write the data

    [Read, Open, Write]
      - stash Read
        Stash: [Read]
     - open => switch state to the open state
        Mailbox: [Read, Write]
     - Read and Write are handled

   */

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // step #1 - mix-in the stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        context.become(open)
        // step #3 - unstashAll when you switch the message handler
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        // step #2 - stash what you can't handle
        stash()
    }

    def open: Receive = {
      case Read =>
        // do some actual computation
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"I am writing $data")
        innerData = data
      case Close =>
        log.info("Closing Resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read // stashed
  resourceActor ! Open // switch to open; I have read ""
  resourceActor ! Open // stashed
  resourceActor ! Write("I love stash") // stashed; "I am writing I love Stash"
  resourceActor ! Close // switch to closed; switch to open
  resourceActor ! Read // I have read I love stash

}
