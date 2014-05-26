package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import org.scalajs.dom.extensions._
import org.scalajs.dom
import example.roll.Roll
import scala.scalajs.js.annotation.JSExport

sealed trait Touch
object Touch{
  case class Down(p: cp.Vect) extends Touch
  case class Move(p: cp.Vect) extends Touch
  case class Up(p: cp.Vect) extends Touch
}
class GameHolder(canvas: dom.HTMLCanvasElement){

  def bounds = new cp.Vect(canvas.width, canvas.height)

  private[this] val keys = mutable.Set.empty[Int]

  var levels = List(
    "Demo.svg",
    "Descent.svg",
    "Bounce.svg",
    "Climb.svg",
    "BarrelWalk.svg"
  )
  updateCanvas()
  def advanceGame() = levels = levels.tail

  val game: Calc[Game] = Calc{
    Roll(
      levels.head,
      () => bounds,
      () => {
        advanceGame()
        game.recalc()
      },
      () => game.recalc()
    )
  }

  var touches = mutable.Buffer.empty[Touch]

  def event(e: dom.Event): Unit = (e, e.`type`.toString) match{
    case (e: dom.KeyboardEvent, "keydown") =>  keys.add(e.keyCode.toInt)
    case (e: dom.KeyboardEvent, "keyup") =>  keys.remove(e.keyCode.toInt)
    case (e: PointerEvent, "pointerdown") => touches += Touch.Down((e.clientX, e.clientY))
    case (e: PointerEvent, "pointermove") => touches += Touch.Move((e.clientX, e.clientY))
    case (e: PointerEvent, "pointerup" | "pointerout" | "pointerleave") => touches += Touch.Up((e.clientX, e.clientY))
    case _ => println("Unknown event " + e.`type`)
  }

  var active = false

  val ctx = canvas.getContext("2d").cast[dom.CanvasRenderingContext2D]

  def updateCanvas() = {
    if (canvas.width != dom.innerWidth) canvas.width = dom.innerWidth
    if (canvas.height != dom.innerHeight) canvas.height = dom.innerHeight
  }
  def update() = {
    updateCanvas()
    val x = touches.toList
    touches.clear()
    game().update(keys.toSet, x)
    game().draw(ctx)
  }
}

abstract class Game{
  var result: Option[String] = None
  def update(keys: Set[Int], touches: Seq[Touch]): Unit
  def draw(ctx: dom.CanvasRenderingContext2D): Unit
}

@JSExport
object ScalaJSExample {
  @JSExport
  def main(): Unit = {
    val canvas =
      dom.document
         .getElementById("canvas")
         .cast[dom.HTMLCanvasElement]

    val ribbonGame = Calc(new GameHolder(canvas))

    Seq("keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave").foreach{s =>
      dom.document.body.addEventListener(s, (e: dom.Event) => ribbonGame().event(e))
    }

    dom.setInterval(() => ribbonGame().update(), 15)
  }
}
