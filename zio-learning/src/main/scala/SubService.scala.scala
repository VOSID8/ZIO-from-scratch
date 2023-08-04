
import zio.{ExitCode, Has, Task, ZIO, ZLayer}

case class user(name: String, email: String)

object userSender {
// type alias to use for other layers
    type userSenderEnv = Has[userSender.Service]

    // service definition
    trait Service {
        def notify(u: user, msg: String): Task[Unit]
    }

    // layer; includes service implementation
    val live: ZLayer[Any, Nothing, userSenderEnv] = ZLayer.succeed(new Service {
        override def notify(u: user, msg: String): Task[Unit] =
            Task {
                println(s"[Email service] Sending $msg to ${u.email}")
            }
    })

// front-facing API, aka "accessor"
    def notify(u: user, msg: String): ZIO[userSenderEnv, Throwable, Unit] = ZIO.accessM(_.get.notify(u, msg))
}

object userDb {
// type alias, to use for other layers
    type userDbEnv = Has[userDb.Service]

// service definition
    trait Service {
        def insert(user: user): Task[Unit]
    }

// layer - service implementation
    val live: ZLayer[Any, Nothing, userDbEnv] = ZLayer.succeed {
        new Service {
            override def insert(user: user): Task[Unit] = Task {
                // can replace this with an actual DB SQL string
                println(s"[Database] insert into public.user values ('${user.name}')")
            }
        }
    }

// accessor
    def insert(u: user): ZIO[userDbEnv, Throwable, Unit] = ZIO.accessM(_.get.insert(u))
}


object userSubscription {
import userSender._
import userDb._

// type alias
type userSubscriptionEnv = Has[userSubscription.Service]

// service definition
class Service(notifier: userSender.Service, userModel: userDb.Service) {
    def subscribe(u: user): Task[user] = {
    for {
        _ <- userModel.insert(u)
        _ <- notifier.notify(u, s"Welcome, ${u.name}! Here are some ZIO articles for you here at Rock the JVM.")
    } yield u
    }
}

// layer with service implementation via dependency injection
    val live: ZLayer[userSenderEnv with userDbEnv, Nothing, userSubscriptionEnv] =
        ZLayer.fromServices[userSender.Service, userDb.Service, userSubscription.Service] { (emailer, db) =>
        new Service(emailer, db)
        }

// accessor
    def subscribe(u: user): ZIO[userSubscriptionEnv, Throwable, user] = ZIO.accessM(_.get.subscribe(u))
}

object ZLayersPlayground extends zio.App {
    override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
        val userRegistrationLayer = (userDb.live ++ userSender.live) >>> userSubscription.live
        
        userSubscription.subscribe(user("Sid", "Sid@vos.com"))
        .provideLayer(userRegistrationLayer)
        .catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
        .map { u =>
            println(s"Registered user: $u")
            ExitCode.success
        }
    }
}