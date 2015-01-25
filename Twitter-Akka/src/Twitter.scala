import java.awt.{BasicStroke, Color}
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import _root_.probability_monad.Distribution._
import akka.actor._
import akka.routing.{RoundRobinGroup, RoundRobinPool, SmallestMailboxPool}
import com.typesafe.config.ConfigFactory
import org.jfree.chart.{ChartFrame, ChartFactory}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.{XYSeriesCollection, XYSeries}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Random

case class Tweet(tweet: String, timestamp: String)
case class STweet(tweet: String, timestamp: String, sender: ActorRef )
case object SendTweet
case class TweetsStat(tweetsRec: Int, tweetsSen: Int, nTweetsProcessed: Int)
case object ResetCounter
case object DisplayStat
case object TweetMore
case object UserConnected
case class createClients(nClients: Int)
case object SendServerInfo
case class ClientIDsRange(start: Int, end: Int )
case class SendClientIDs(clientSz: Int)

object Database {
  var userCount = 0
  val mapID = mutable.HashMap[String, Int]()
  val followersMap = new ArrayBuffer[Set[Int]]()
  val actorsMap = mutable.HashMap[String, String]()
  //val messageQBuff = new ConcurrentHashMap[Int, mutable.Queue[(String, String)]]()
}

object Twitter {

  def paretoD(nUsers : Int) = {
    Database.followersMap += Set(0)
    val size = nUsers
    val xm = if (size <= 10000) 1 else if(size <= 100000) 2 else if(size <= 400000) 3 else 3*size/400000
    val pdf = pareto(math.log10(5)/math.log10(4), xm )
    val samples = pdf.given(_ < size).sample(size).sortWith(_ > _)
    var totalSize = 0
    1 to size map {user =>
      val followers = if (samples(user - 1) - samples(user - 1).toInt < 0.45)
        samples(user - 1).floor.toInt
      else samples(user - 1).ceil.toInt
      totalSize += followers
      var follSet = Stream.fill(followers)(Random.nextInt(size) + 1).toSet
      while(follSet.size < followers) follSet += Random.nextInt(size) + 1

      Database.followersMap += follSet
    }

    println("Total followers size : " + totalSize)
  }

  def plotDistribution() = {
    val map = Database.followersMap.clone()
    val series = new XYSeries("User vs # of followers" )
    val data = for(i <- 1 until map.length) series.add(i, map(i).size)
    val dataSet = new XYSeriesCollection()
    dataSet.addSeries(series)
    plotGraph(dataSet)

  }

  def plotGraph(dataSet: XYSeriesCollection) = {
    val sc = ChartFactory.createXYLineChart("User vs # of followers, using Pareto distribution", "UserX", "# of followers", dataSet,
      PlotOrientation.VERTICAL, true, true, false)
    val plot = sc.getXYPlot
   val renderer = new XYLineAndShapeRenderer()
    plot.setOutlinePaint(Color.BLACK)
    plot.setOutlineStroke(new BasicStroke(2.0f))
    plot.setBackgroundPaint(Color.WHITE)
    plot.setRangeGridlinesVisible(true)
    plot.setRangeGridlinePaint(Color.BLACK)
    plot.setDomainGridlinesVisible(true)
    plot.setDomainGridlinePaint(Color.BLACK)
    renderer.setSeriesShapesVisible(0, false)
    plot.setRenderer(renderer)
    val frame = new ChartFrame("First", sc)
    frame.pack()
    frame.setVisible(true)
    frame.setForeground(Color.WHITE)
  }

  def SimpleTwitterStat(nUsers: Int) = {
    val sampleSize = 400000
    var strBatchSz = 1
    var endBatchSz = (10 * nUsers) / 100
    // 10% user have less than 3 followers
    Database.followersMap += Set(0)
    var scaledValue = ((3 * nUsers) / sampleSize).toInt
    var follCount = if (scaledValue >= 1) scaledValue else 1
    (strBatchSz to endBatchSz) map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      //arr.foreach(x => println(x))
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((20 * nUsers) / 100).toInt
    scaledValue = ((9 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 3
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      //val set = Set(arr : _*)
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((30 * nUsers) / 100).toInt
    scaledValue = ((19 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 9
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((40 * nUsers) / 100).toInt
    scaledValue = ((36 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 19
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((50 * nUsers) / 100).toInt
    scaledValue = ((61 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 36
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((60 * nUsers) / 100).toInt
    scaledValue = ((99 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 61
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((70 * nUsers) / 100).toInt
    scaledValue = ((154 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 98
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((80 * nUsers) / 100).toInt
    scaledValue = ((246 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 154
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((90 * nUsers) / 100).toInt
    scaledValue = ((458 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 246
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((95 * nUsers) / 100).toInt
    scaledValue = ((819 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 458
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((96 * nUsers) / 100).toInt
    scaledValue = ((978 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 819
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((97 * nUsers) / 100).toInt
    scaledValue = ((1211 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 978
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((98 * nUsers) / 100).toInt
    scaledValue = ((1675 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 1211
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((99 * nUsers) / 100).toInt
    scaledValue = ((2991 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 1675
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    strBatchSz = endBatchSz + 1
    endBatchSz = ((99.9 * nUsers) / 100).toInt
    scaledValue = ((24964 * nUsers) / sampleSize).toInt
    follCount = if (scaledValue >= 1) scaledValue else 2991
    strBatchSz to endBatchSz map { user =>
      val arr = Array.fill(follCount)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    val nFoll = (0.5 * nUsers).toInt
    (endBatchSz + 1 to nUsers) map { user =>
      val arr = Array.fill(nFoll)(Random.nextInt(nUsers) + 1).toSet
      Database.followersMap += arr
    }

    println("Total followers size : " +  Database.followersMap.size)

  }

  def printInfo() = {
    println("Size of followers map : " + Database.followersMap.length)
    for(idx <- 1 until  Database.followersMap.length) {
      print(idx + " -> ")
      Database.followersMap(idx).foreach(ele => print(ele + " "))
      println()
    }
  }

  def main(args: Array[String]) {
    //Start Server
    if(args(0) equalsIgnoreCase "server"){
      val nServers = args(1).toInt
      val nUsers = args(2).toInt
      val displayInterval = args(3).toInt
      val ServerIP = args(4)
      val dist = args(5)
      val myConfig = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=%s".format(ServerIP))
      val dfltConfig = ConfigFactory.load("Server")
      val combined = myConfig.withFallback(dfltConfig)
      val serverConfig = ConfigFactory.load(combined)
      //println(serverConfig.getString("akka.remote.hostname"))
      val usersPerServer = (nUsers.toDouble/nServers).ceil.toInt

      1 to nUsers map { user =>
        Database.mapID += ("User" + user -> user)
        //Database.messageQBuff += (user -> mutable.Queue[(String, String)]())
      }
      //SimpleTwitterStat(nUsers)
      if(dist equalsIgnoreCase("P"))
        paretoD(nUsers)
      else
        SimpleTwitterStat(nUsers)
      //printInfo()
      //plotDistribution()
      val serverSystem = ActorSystem("TwitterServer", serverConfig)
      //val serverIP = ConfigFactory.load("Server").getString("akka.remote.netty.tcp.hostname")
      val counterActor = serverSystem.actorOf(Props(new counter(displayInterval, nServers)), "counter")

      val clRng = mutable.ArrayBuffer[Tuple2[Int, Int]]()
      var start = 1
      var end = usersPerServer
      for(i <- 1 to nServers){ val s = start; val e = end ; clRng += Tuple2(s,e) ;
        start = end + 1
        end = end + usersPerServer
        if(end > nUsers)
          end = nUsers
      }
      clRng.foreach{ x => print(x._1) ; print(" " + x._2); println() }
      1 to clRng.length map {i => val tup = clRng(i - 1)
        serverSystem.actorOf(Props(new Server(displayInterval, counterActor, nServers, nUsers, tup._1, tup._2)), "server" + i)
      }

      var ith = 0
      val list =
        List.fill(nServers) {
          ith +=1
          "/user/server" + ith
        }

      val router = serverSystem.actorOf(RoundRobinGroup(list).props(), "router")
      //val routerActor = serverSystem.actorOf(RoundRobinPool(nServers).props(Props(new Server(displayInterval, counterActor))), "router")
      //val routerActor = serverSystem.actorOf(SmallestMailboxPool(nServers).props(Props(new Server(displayInterval, counterActor))), "router")
      //val router = serverSystem.actorOf(FromConfig.props(Props(new Server(displayInterval, counterActor))), "router")
    }

    // Start Client
    else if (args(0) equalsIgnoreCase "client") {
      val serverIP = args(1)
      val serverPort = args(2)
      val nClients = args(3).toInt
      val tweetInterval = if(args.length == 5) args(4).toInt else 1000

      val serverLoc = "akka.tcp://TwitterServer@%s:%s/user/router".format(serverIP, serverPort)
      println(serverLoc)
      val counterLoc = "akka.tcp://TwitterServer@%s:%s/user/counter".format(serverIP,serverPort)
      val ClientIP = args(5)
      val myConfig = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=%s".format(ClientIP))
      val dfltConfig = ConfigFactory.load("User")
      val combined = myConfig.withFallback(dfltConfig)
      val userConfig = ConfigFactory.load(combined)
      val clientSystem = ActorSystem("TwitterClient", userConfig )
      val clientHelper = clientSystem.actorOf(Props(new ClientHelper(counterLoc, serverLoc, nClients, tweetInterval)), "helper")
    }

    else{
      println("Please input the following arguments : ")
      println("To start the server : " + "Server" + " <No. of Server Actors> <Total Client> <Stats display interval in ms>")
      println("To start the client : " + "Client" + " <ServerIP> <Server Port> <No of users> -optional <Next Tweet Interval in ms>")
    }
  }
}

class counter(duration: Int, nServers: Int) extends Actor {
  var totalTweetsProcessed = 0
  var serverCount = 0
  var currClientSize = 0

/*  override def preStart() = {
    //context.system.scheduler.schedule(0 second,duration milliseconds, self, DisplayStat)
  }*/

  def receive = {
    case SendClientIDs(clientSz) =>
      // Store the references of new actors that will be created by the client helper actor
      val parent = sender().path.parent

      (currClientSize + 1 to currClientSize + clientSz) map { x =>
        val userID = "User" + x
        val actorPath = parent + "/" + userID
        Database.actorsMap += (userID -> actorPath)
      }
      // Send the requested info to client helper
      sender ! ClientIDsRange(currClientSize + 1, currClientSize + clientSz)
      currClientSize += clientSz

    case TweetsStat(tweetsRec, tweetsSent, nTweetsProcessed) =>
      //println("Received from " + sender)
      totalTweetsProcessed += nTweetsProcessed
      serverCount += 1
      if (serverCount == nServers) {
        //self ! DisplayStat
        //println("Total tweets received : " + tweetsRec)
        //println("Total tweets sent : " + tweetsSent)
        println("Total tweets processed : " + totalTweetsProcessed)
        serverCount = 0
        totalTweetsProcessed = 0
      }

    case DisplayStat =>
      println("Number of tweets processed by server is : " + totalTweetsProcessed)
      serverCount = 0
      totalTweetsProcessed = 0

    case _ => //println("")
  }
}

class Server(displayInterval: Int,
             counter: ActorRef,
             nServers : Int,
             nUsers : Int,
             val iDStart : Int,
             val iDEnd : Int)
  extends Actor {
  var nTweetsRec = 0
  var nTweetsSent = 0
  val myNo = self.path.name.drop(6).toInt
  val usersPerServer = (nUsers.toDouble/nServers).ceil.toInt
  // Server Data
  val msgQ = mutable.Map[Int, mutable.Queue[(String, String)]]()
  iDStart to iDEnd map {id => msgQ += (id -> mutable.Queue[(String, String)]()) }

  implicit def reset() = {
    counter ! TweetsStat(nTweetsRec, nTweetsSent, nTweetsRec + nTweetsSent)
    nTweetsRec = 0
    nTweetsSent = 0
    //println("Server : " + self + " reset the tweet counter")
  }

  override def preStart() = {
    import context.dispatcher
    context.system.scheduler.schedule(0 second, displayInterval milliseconds, self, ResetCounter)
    //context.system.scheduler.schedule(0 second, displayInterval milliseconds)(reset)
  }

  def storeTweet(userID: String, tweet: String, timestamp : String): Unit = {
    val qID = userID.drop(4).toInt  // Database.mapID(userID)
    if (msgQ(qID).size > 100) {
      msgQ(qID).dequeue()
    }
    msgQ(qID).enqueue(Tuple2(tweet, timestamp))
  }

  def receive = {
    case Tweet(tweet, timestamp) =>
      //Check to which actor the tweet belongs
      val sID = (sender.path.name.drop(4).toDouble / usersPerServer).ceil.toInt
      //println("Server ID is : " + sID + " and myNo is " + myNo)
      if(sID != myNo)
        context.actorSelection(self.path.parent + "/server" + sID) ! STweet(tweet, timestamp, sender)
      else{
        // Store the tweet in the database
        nTweetsRec += 1
        val userID = sender().path.name
        storeTweet(userID, tweet, timestamp)

        //Send this tweet to all the followers and the user
       val userMapID = Database.mapID(userID)
        sender ! Tweet(tweet, new Timestamp(System.currentTimeMillis()).toString)
        nTweetsSent += 1
        for (user <- Database.followersMap(userMapID)) {
         if (Database.actorsMap.contains("User" + user)) {
            context.actorSelection(Database.actorsMap("User" + user)) !
              Tweet(tweet, new Timestamp(System.currentTimeMillis()).toString)
            nTweetsSent += 1
          }
        }
       }
    case STweet(tweet, timestamp, sender) =>
      nTweetsRec += 1
      val userID = sender.path.name
      //println("myNo " + myNo + " Sender no " + userID)
      storeTweet(userID, tweet, timestamp)

      //Send this tweet to all the followers and the user
      val userMapID = Database.mapID(userID)
      sender ! Tweet(tweet, new Timestamp(System.currentTimeMillis()).toString)
      nTweetsSent += 1
      //Database.followersMap(userMapID).foreach(_ ! Tweet(tweet))
      for (user <- Database.followersMap(userMapID)) {
        if (Database.actorsMap.contains("User" + user)) {
          context.actorSelection(Database.actorsMap("User" + user)) !
            Tweet(tweet, new Timestamp(System.currentTimeMillis()).toString)
          nTweetsSent += 1
        }
      }

    case ResetCounter =>
      //println("Tweet Processed by : " + self.path.name)
      //println("Tweets Received : " + nTweetsRec)
      //println("Tweets Sent : " + nTweetsSent)
      //val total = nTweetsRec + nTweetsSent
      //println("Tweets Total : " + total )
      counter ! TweetsStat(nTweetsRec, nTweetsSent, nTweetsRec + nTweetsSent)
      nTweetsRec = 0
      nTweetsSent = 0

    case _ => println("Some ? message rec by " + self.path.name)
  }
}

class ClientHelper(counterPath: String ,serverPath: String, clientSz: Int, tweetInterval : Int) extends Actor {

  //var serverPath: String = _
  var counterRef : ActorRef = _
  val nClients = clientSz
  //println(self + " started!")
  var schd : akka.actor.Cancellable = _
  override def preStart() = {
    sendIdentifyRequest // to counter actor
  }
  def sendIdentifyRequest() = {
    context.actorSelection(counterPath) ! Identify(counterPath)
    import context.dispatcher
    schd = context.system.scheduler.scheduleOnce(2 second, self, ReceiveTimeout)
  }

  def receive = {
    case ActorIdentity(`counterPath`, Some(actor)) =>
      if(!schd.isCancelled) schd.cancel()
      counterRef = actor
      println("---------------Connection with Server established------------------")
      println("Reference of server : " + counterRef)
      println("Requesting client ID's from server")
      counterRef ! SendClientIDs(nClients)

    case ActorIdentity(`counterPath`, None) =>
      println("Server is not yet available to : " + self )

    case ReceiveTimeout =>
      println("Timeout")
      sendIdentifyRequest

    case ClientIDsRange(start, end) =>
      // Create Clients
      start to end map { x =>
        //@todo change the tweet time interval
        context.system.actorOf(Props(new Client(serverPath, tweetInterval )), "User" + x)
      }

    case _ => println("Something wrong with client helper!!")
  }

}

class Client(serverPath: String, tweetInterval: Int) extends Actor {
  val server = serverPath
  val nextTweetIn = tweetInterval
  val tweetQ = mutable.Queue[Tuple2[String, String]]()
  //val pQ = mutable.PriorityQueue[Tuple2[String, String]]()(Ordering.by(_._2.toInt))

  override def preStart() = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(0 millisecond, self, SendTweet)
    // @todo Get some tweet data from server after some time
  }

  def receive = {
    case SendTweet =>
      val time = new Timestamp(System.currentTimeMillis()).toString
      val tStr = "Hello twitter"
      context.actorSelection(server) ! Tweet(tStr, time)
      // can be changed
      import context.dispatcher
      context.system.scheduler.scheduleOnce(nextTweetIn milliseconds, self, SendTweet)

    case TweetMore =>
    // @todo

    case Tweet(tweet, timestamp) => // Got a tweet from server
      if(tweetQ.length > 100) tweetQ.dequeue()
      tweetQ.enqueue((tweet, timestamp))

    case _ => println("I am not supposed to receive this")
  }
}
