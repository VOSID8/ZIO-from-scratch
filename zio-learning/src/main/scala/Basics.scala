import zio.console._
import zio.{App, ExitCode, URIO, ZIO, Task}
import zio.ZLayer
import zio.Has

// Separate the userEmailer object from the Basics object
object userEmailer {
  case class User(name: String, email: String)

  //service def
  trait Service {
    def notify(user: User, message: String): Task[Unit] // Task is a ZIO with Throwable as the error type
    //ZIO[Any, Throwable, Unit]
  }

  //service impl
  val live: ZLayer[Any, Nothing, Has[userEmailer.Service]] = ZLayer.succeed(new Service {
    override def notify(user: User, message: String) = Task {
      println(s"Sending email to ${user.email} with message ${message}")
    }
  })

  //front facing api
  def notify(user: User, message: String): ZIO[Has[userEmailer.Service], Throwable, Unit] = {
    ZIO.accessM(hasService => hasService.get.notify(user, message))
    // ZIO.accessM(_.get.notify(user, message))
    // accessM is a combinator that allows us to access the environment
    // hasService is a Has[userEmailer.Service] which is a type alias for Has[userEmailer.Service.Service]
    // hasService.get is a Service
  }
}

object Basics extends zio.App {
  import userEmailer._

  val program = for {
    _ <- putStrLn("Hello! What is your name?")
    name <- getStrLn
    _ <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
    _ <- putStrLn("What is your favorite programming language?")
    lang <- getStrLn
    _ <- putStrLn(s"Your favorite programming language is ${lang}")
  } yield ()

  val sid = User("sid", "sidd@gmail.com")
  val sidMessage = "Hello Sid!"

  // Move the run method outside the userEmailer object
  override def run(args: List[String]) =
    userEmailer.notify(sid, sidMessage) //what kind of trigger we wanna do, kind of effect
      .provideLayer(userEmailer.live) //input for that effect
      .exitCode
}
