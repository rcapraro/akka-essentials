package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // part 1 - actor systems
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part 2 - create actors
  // word count actor
  class WordCountActor extends Actor {
    //internal data
    var totalWords = 0
    // behavior
    override def receive: Receive = { // Receive is an alias on PartialFunction[Any, Unit]
      case message: String =>
        println(s"[word counter] i have received: $message")
        totalWords += message.split(" ").length
      case message => println(s"[word counter] I cannot understand ${message.toString}")
    }
  }

  // part 3 - instantiate our actor
  val wordCountActor = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // part 4 - communicate!
  wordCountActor ! "I am learning Akka and it's pretty damn cool!" // ! = "tell"
  anotherWordCounter ! "A different message"
  // asynchronous

  // best practice: props in companion object
  object Person {
    def props(name: String): Props = Props(new Person(name))
  }

  // actor with constructor arguments
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  val bob = actorSystem.actorOf(Person.props("Bob"), "bobActor")
  bob ! "hi"

}
