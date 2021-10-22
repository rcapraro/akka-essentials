package part1recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {

  // 1 - OOP encapsulation is only valid in the SINGLE THREAD MODEL

  class BankAccount(@volatile private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int): Unit = this.amount -= money

    def deposit(money: Int): Unit = this.amount += money

    def getAmount: Int = amount
  }

  /*
  val account = new BankAccount(2000)
  for(_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }
  for(_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }

  println(account.getAmount) // not always 2000 !
  */


  // OOP is broken

  // synchronization! Locks (synchronized) to the rescue
  // deadlock, livelock, etc

  // 2 - delegating something to a thread is a PAIN.

  // you have a running thread and you want to pass a runnable to that thread.
  var task: Runnable = null
  val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task == null) {
        runningThread.synchronized {
          println("[background] waiting for a task...")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("[background] I have a task")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable): Unit = {
    if (task == null) task = r
    runningThread.synchronized {
      runningThread.notify()
    }
  }

  /*
  runningThread.start()
  Thread.sleep(500)
  delegateToBackgroundThread(() => println(42))
  Thread.sleep(100)
  delegateToBackgroundThread(() => println("this should run in the background"))
  */

  // 3 - tracing and dealing with errors in a multithreading env is a PITA.

  // 1M in between 10 thread

  import scala.concurrent.ExecutionContext.Implicits.global
  val futures = (0 to 9)
    .map(i => 100000 * i until 100000 * (i + 1)) // 0 - 99999, 100000 - 199999, 200000 - 299999, etc
    .map(range => Future {
      if (range.contains(546745)) throw new RuntimeException("Invalid number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)( _ + _) // Future will the sum of all the number
  sumFuture.onComplete(println)
}
