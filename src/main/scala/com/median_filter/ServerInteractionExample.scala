package com.median_filter

import java.io.File

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class LinearMedianFilterActor ( var windowWidth: Int, var windowHeight: Int ) extends Actor {
  val log: LoggingAdapter = Logging(context.system, this)
  log.info("Starting")

  def medianFilter( img:BufferedImage ) : BufferedImage = {
    val outputPixelValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR)
    val window = Array.fill(windowWidth * windowHeight)(0)
    val edgex = windowWidth / 2
    val edgey = windowHeight / 2

    for ( x <- edgex until img.getWidth() - edgex ) {
      for ( y <- edgey until img.getHeight() - edgey ) {
        var i = 0
        for ( fx <- 0 until windowWidth ) {
          for ( fy <- 0 until windowHeight ) {
            window(i) = img.getRGB(x + fx - edgex, y + fy - edgey)
            i += 1
          }
        }
        window.sortInPlace()
        outputPixelValue.setRGB(x, y, window(windowWidth * windowHeight / 2))
      }
    }
    outputPixelValue
  }
//input for train test image
  def receive: Receive = {
    case image:BufferedImage =>
      log.info("Received image")
      val t0 = System.nanoTime()
      val newImg = this.medianFilter(image)
      val t1 = System.nanoTime()
      log.info("Done! took "+ (t1 - t0) / 1000000000.0 +"s")

      sender ! newImg
  }
}

class ConcurrentMedianFilterActor ( var windowWidth1: Int, var windowHeight1: Int ) extends LinearMedianFilterActor ( windowWidth1, windowHeight1 ) {
  override def medianFilter(img: BufferedImage): BufferedImage = {
    val outputPixelValue = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR)
    val edgex = windowWidth / 2
    val edgey = windowHeight / 2

    //Create a buffer of futures to handle concurrency
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    val futures = new ListBuffer[Future[Any]]

    for ( x <- edgex until img.getWidth() - edgex ) {

      val otherFuture = Future {

        for (y <- edgey until img.getHeight() - edgey) {

          val newFuture = Future {
            val window = Array.fill(windowWidth * windowHeight)(0)
            var i = 0
            for (fx <- 0 until windowWidth) {
              for (fy <- 0 until windowHeight) {
                window(i) = img.getRGB(x + fx - edgex, y + fy - edgey)
                i += 1
              }
            }
            window.sortInPlace()
            outputPixelValue.setRGB(x, y, window(windowWidth * windowHeight / 2))
          }

          futures += newFuture

        }

      }
      futures += otherFuture
    }
    outputPixelValue
  }
}

class Server extends Actor {
  val log: LoggingAdapter = Logging(context.system, this)
  log.info("Starting")
  var height, width = 3
  var linearActor: ActorRef = context.actorOf(Props(new LinearMedianFilterActor(width, height)),"Linear_Median_Filter_Actor")
  var concurrentActor: ActorRef = context.actorOf(Props(new ConcurrentMedianFilterActor(width, height)),"Concurrent_Median_Filter_Actor")
  def receive: Receive = {
    case path:File =>
      val image = ImageIO.read(path)
      //Initialize both actors
      log.info("Received image")
      implicit val timeout: Timeout = Timeout(100 seconds)
      val linearFuture: Future[Any] = ask(linearActor, image)
      val concurrentFuture: Future[Any] = ask(concurrentActor, image)
      val linearResult: BufferedImage = Await.result(linearFuture, 100 second).asInstanceOf[BufferedImage]
      val concurrentResult: BufferedImage = Await.result(concurrentFuture, 100 second).asInstanceOf[BufferedImage]

      // Save images
      val linearIMG_path = new File(path.getParent + "/linearIMG.jpg")
      val concurrentIMG_path = new File(path.getParent + "/concurrentIMG.jpg")
      ImageIO.write(linearResult, "jpg", linearIMG_path)
      ImageIO.write(concurrentResult, "jpg", concurrentIMG_path)
      log.info("Saved Linear Image as: "+linearIMG_path)
      log.info("Saved Concurrent Image as: "+concurrentIMG_path)
    case _ => log.warning("Didnt receive an image...")
  }
}

object Main extends App {
  val system = ActorSystem("Server_Example")
  var server = system.actorOf(Props[Server],"Server")
  // open charlie chaplin image
  var path = new File("./src/main/imgs/test.jpg")
  println(path.getAbsolutePath)
  if (args.length != 0) {
    var path = new File(args(0))
  }
  server ! path

}
