package jobs

import akka.actor.Actor
import akka.actor.ActorRef

import models._

class Counter(pos: GeoSquare, listener: ActorRef) extends Actor {
  var count = 0L

  def increase(n: Int) = {
    count += n
   // listener ! (id, n)
  }

  def get() = count
  /*TODO change this so that report is a default Object*/
  def receive = {
    case ts: Seq[_] => increase(ts.size)
    case ReportCount =>
      listener ! Report(pos, count)
    case _ => 
      increase(1)
  }
}
