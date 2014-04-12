package jobs

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.GeoSquare
import TweetManager._
import models._

class GeoPartitionner(keywords: List[String], square: GeoSquare, row: Int, col: Int) extends Actor {
  /*Total Number of tweets*/
  var total = 0L
  /*Map holding the results*/
  var results: Map[GeoSquare, Long] = Map()
  /*List of all the queries to send*/
  val queries = TweetQuery(keywords, square, row, col).subqueries
  /*List of listeners*/
  val listeners: List[ActorRef] = queries.map(x => ActorSystem().actorOf(Props(new Counter(x.area, self))))

  def receive = {  
   case Winner => 
    println("winner is: "+results.maxBy(_._2))  
   case Collect => 
    listeners.foreach(_ ! ReportCount)
   case Report(id, count) =>
    total += count
    results += (id -> count)
   case TotalTweets => 
    sender ! total
  }
}
