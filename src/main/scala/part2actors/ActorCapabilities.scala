package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {

    override def receive: Receive = {
      case "Hi!" => sender() ! "Hello, there!"
      case message: String => println(s"[${self.path}] I have received $message")  //self=context.self
      case number: Int => println(s"[${self.path}] I have received a NUMBER $number")
      case SpecialMessage(contents) => println(s"[${self.path}] I have received something SPECIAL: $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // i keep the original sender of the WPM
    }
  }

  val actorSystem = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  // 1 - messages can be of any type
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE
  // in practice, use case classes and case objects
  simpleActor ! 42

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information about their context and about themselves (context, context.self, etc)
  // context.self == self ~ this

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and i am proud of it")

  // 3 - actor can REPLY to messages
  val alice = actorSystem.actorOf(Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi!" // reply to "me", but "sender" is null ? => dead letters


  // 5 - forwarding messages (sending message with the original sender)
  // R -> A -> B
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob)

}
