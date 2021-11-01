package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /*
  1 - Inline configuration
   */
  val configString =
    """
      | akka {
      |   loglevel = "INFO"
      | }
  """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "A message to remember"

  /*
  2 - Configuration file
  */
  // akka automatically at src/main/resources/application.conf
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"

  /*
  3 - Separate configuration in the same file
  */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "Remember me, I'm special"

  /*
  4 - Separate configuration in another file
  */
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"Separate config file - log level ${separateConfig.getString("akka.loglevel")}")

  /*
  5 - Different file formats: JSON, properties
  */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"JSON config file - log level ${jsonConfig.getString("akka.loglevel")}")
  val propsConfig = ConfigFactory.load("props/propsConfig.json")
  println(s"Properties config file - log level ${propsConfig.getString("akka.loglevel")}")



}
