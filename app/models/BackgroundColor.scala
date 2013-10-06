package models.bgcolor

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

case object Join
case object Quit

case class Connected(enumerator:Enumerator[JsValue])
case object UpdateBackground
case object ResetMax

object BackgroundColor {

  implicit val timeout = Timeout(1 second)

  lazy val default = Akka.system.actorOf(Props[BackgroundColor])

  def join: scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
    (default ? Join).map {
      case Connected(enumerator) =>
        val iteratee = Iteratee.foreach[JsValue] { event =>
          (event \ "action").as[String] match {
            case "reset" => default ! ResetMax
          }
          default ! UpdateBackground
        }.map { _ =>
          default ! Quit
        }

        default ! UpdateBackground
        (iteratee,enumerator)
    }
  }
}

class BackgroundColor extends Actor {
  var count = 0
  var max = 0
  val (eventEnumerator, eventChannel) = Concurrent.broadcast[JsValue]

  def receive = {
    case Join => {
      count += 1
      if(count > max)
        max = count
      sender ! Connected(eventEnumerator)
      self ! UpdateBackground
    }

    case UpdateBackground =>
      notifyAll("update", count, max)

    case ResetMax =>
      max = count
      self ! UpdateBackground

    case Quit => {
      count -= 1
      self ! UpdateBackground
    }
  }

  def notifyAll(kind: String, count: Int, max: Int) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "count" -> JsNumber(count),
        "max" -> JsNumber(max)
      )
    )
    eventChannel.push(msg)
  }
}
