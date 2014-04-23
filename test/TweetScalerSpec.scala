import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import jobs._
import models.TweetQuery
import models.GeoSquare
import models.Tweet
import play.api.libs.json.Json
import akka.actor.Actor
import models._
import jobs.TweetManager._
import akka.actor.ActorRef
import play.api.libs.json.JsNumber
import utils.AkkaImplicits._

@RunWith(classOf[JUnitRunner])
class TweetScalerSpec extends Specification {

  class Reporter(kws : List[String]) extends Actor {
    var generalCount = 0
    var duplicateCount = 0
    var kwsCount = kws map (kw => (kw -> 0)) toMap
    var ids: List[Long] = Nil

    def receive = {
      case kw: String =>
        println("General count: " + generalCount + ", duplicates: " + duplicateCount)
        println("Count for " + kw + ":" + kwsCount(kw))

      case (id: Long, kw: String) =>
        kwsCount += (kw -> (kwsCount(kw) + 1))
        generalCount += 1
        if (ids.contains(id)) {
          duplicateCount += 1
          println("duplicate found, nb of duplicates: " + duplicateCount + "/" + generalCount)
        }
        ids :+= id
    }
  }

  class Listener(reporter: ActorRef) extends Actor {
    var count = 0
    def receive = {
      case Tweet(value, origin) =>
        println("\t" + (value \ "text"))
      	reporter ! (((value \ "id").as[JsNumber].value.toLong), origin.kwsInSearchFormat)
        count += 1
        if (count % 50 == 0) reporter ! origin.kwsInSearchFormat
    }
  }
  
  "Tweet Manager" should {
    "start a lot of queries, check for duplicates, and never stop" in new WithApplication  {
      /* Requests are all over the US */
      val queries = List(
          TweetQuery("Federer" :: Nil, GeoSquare(-8.9, 44.02, 14.22, 52.7), 10, 10),
          TweetQuery("Wawrinka" :: Nil, GeoSquare(-8.9, 44.02, 14.22, 52.7), 10, 10),
          TweetQuery("Monte Carlo" :: "Monte-Carlo" :: Nil, GeoSquare(-8.9, 44.02, 14.22, 52.7), 10, 10))
      val reporter = toRef(Props(new Reporter(queries map (_.kwsInSearchFormat))))
      
      val subqueries = queries flatMap (_.subqueries) map (qu => (qu, toRef(Props(new Listener(reporter)))))
      TweetManagerRef ! AddQueries(subqueries)
      TweetManagerRef ! Start
      
      while(true) Thread.sleep(40000000)

    }
  }
}