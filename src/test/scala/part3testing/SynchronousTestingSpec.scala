package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override protected def afterAll(): Unit = system.terminate()

  import SynchronousTestingSpec._
  "A counter" should {
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc // counter has already received the message
      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc)
      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      // let the counter operate on the calling thread!
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()
      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) // probe has already received the message 0
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read
  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {

      case Inc => count += 1
      case Read => sender ! count
    }
  }
}
