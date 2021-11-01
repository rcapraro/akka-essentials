package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {

  // Actors can create other actors
  object Parent {
    case class CreateChild(name: String)

    case class TellChild(message: String)
  }

  class Parent extends Actor {

    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        // create a new actor right HERE
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) =>
        childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  import Parent._

  var system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("Hey Kid!")

  // Actor hierarchies
  // parent -> child  -> grandChild
  //        -> child2 -> etc...

  /*
  Guardian actors (top level):
    - /system = system guardian (log, etc)
    - /user = user-level guardian (created by us: system.actorOf)
    - / = root guardian, parent of /system and /user
   */

  /*
  Actor selection
   */
  val childSelection = system.actorSelection("user/parent/child")
  childSelection ! "I found you!"

  /*
  !!!DANGER!!!
  NEVER PASS MUTABLE ACTOR STATE, OR THE `THIS` REFERENCE, TO CHILD ACTORS!!!
   */
  object NaiveBankAccount {
    case class Deposit(amount: Int)

    case class Withdraw(amont: Int)

    case object InitializeAccount
  }

  class NaiveBankAccount extends Actor {

    import CreditCard._
    import NaiveBankAccount._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // DANGER!!!
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int): Unit = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }

    def withdraw(funds: Int): Unit = amount -= {
      println(s"${self.path} withdrawing $funds on top from $amount")
      funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // DANGER!!! - should contain an ActorRef instead!

    case object CheckStatus
  }

  class CreditCard extends Actor {

    import CreditCard._

    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachedTo(account))
    }

    def attachedTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} you message has been processed. ")
        // benign
        account.withdraw(1) // because I can: never do that, always use messages!
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)
  Thread.sleep(500)
  val creditCardSelection = system.actorSelection("/user/account/card")
  creditCardSelection ! CheckStatus

}
