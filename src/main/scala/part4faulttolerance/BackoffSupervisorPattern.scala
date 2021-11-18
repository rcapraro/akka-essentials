package part4faulttolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import java.io.File
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {

    var dataSource: Source = null

    override def preStart(): Unit =
      log.info("Persistent actor starting")

    override def postStop(): Unit =
      log.info("Persistent actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent actor restarted")

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testFiles/important_data.txt"))
        log.info("I've just read some IMPORTANT data" + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackoffSupervisorDemo")
  // val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
  // simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(BackoffOpts.onFailure(
    Props[FileBasedPersistentActor],
    "simpleBackoffActor",
    3 seconds,
    30 seconds,
    0.2
  ))

  // val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  // simpleBackoffSupervisor ! ReadFile
  /*
  simpleSupervisor:
  - creates a child called simpleBackoffActor (props of type FileBasedPersistentActor)
  - supervision strategy is the default one (restarting on everything)
    - first attemtpt after 3 seconds
    - next attempt is 2x the previous attempts: 3s, 6s, 12s, 24s
   */

  val stopSupervisorProps = BackoffSupervisor.props(BackoffOpts.onStop(
    Props[FileBasedPersistentActor],
    "stopBackoffActor",
    3 seconds,
    30 seconds,
    0.2
  ).withSupervisorStrategy
    (
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

  // val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  // stopSupervisor ! ReadFile

  class EagerFileBasePersistentActor extends FileBasedPersistentActor {
    override def preStart(): Unit =
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testFiles/important_data.txt"))
  }

  val eagerActor = system.actorOf(Props[EagerFileBasePersistentActor])
  // in case of ActorInitializationException, the default strategy is Stop

  val repeatedSupervisorProps = BackoffSupervisor.props(BackoffOpts.onStop(
    Props[EagerFileBasePersistentActor],
    "eagerActor",
    1 second,
    30 seconds,
    0.1
  ))

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

  /*
  eagerSupervisor
    - child eagerActor
      - will die on start with ActorInitializationException
      - trigger the supervision strategy in eagerSupervisor => Stop eagerActor
    - backoff will kick in after 1 second, 2s, 4s, 8s, 16s
   */


}
