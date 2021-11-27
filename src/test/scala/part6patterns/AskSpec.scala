package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  import AskSpec._

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "An piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  def authenticatorTestSuite(props: Props): Unit = {

    import AuthManager._

    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("richard", "rtjvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("richard", "rtjvm")
      authManager ! Authenticate("richard", "iloveakka")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("richard", "rtjvm")
      authManager ! Authenticate("richard", "rtjvm")
      expectMsg(AuthSuccess)
    }
  }
}

object AskSpec {

  // this code is somewhere else in you app
  case class Read(key: String)

  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"trying to read the value at the key $key")
        sender ! kv.get(key) //Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // user authenticator actor
  case class RegisterUser(username: String, password: String)

  case class Authenticate(username: String, password: String)

  case class AuthFailure(message: String)

  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }

  class AuthManager extends Actor with ActorLogging {

    import AuthManager._

    // 'Ask' logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDB: ActorRef = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String): Unit = {
      val originalSender = sender
      val future = authDB ? Read(username)
      future.onComplete {
        // most important: never call methods on the actor instance or access mutable state in inComplete
        // avoid closing over the actor instance or mutable state
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipedAuthManager extends AuthManager {

    import AuthManager._

    override def handleAuthentication(username: String, password: String): Unit = {
      val future = authDB ? Read(username) //Future[Any]
      val passwordFuture = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // will be completed with the response I will send back

      // when a future complete, send the response to the actor ref in the arg list
      responseFuture.pipeTo(sender)
    }
  }
}
