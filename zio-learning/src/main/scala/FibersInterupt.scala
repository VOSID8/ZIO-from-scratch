
import zio.{UIO, ZIO, ExitCode}
import zio.clock._
import zio.duration._
import zio._


object FibersInterupt extends zio.App {
  val program = for {
    _ <- UIO(println("Hello from fiber!"))
  } yield ()

  //synchronous down here
  val zmol: UIO[Int] = ZIO.effectTotal(42)

  val showerTime = ZIO.succeed("Showering!")
  val breakfastTime = ZIO.succeed("Breakfast!")
  val preparingCoffee = ZIO.succeed("Preparing coffee!")
  val bathTime = ZIO.succeed("Going to the bathroom")
  val boilingWater = ZIO.succeed("Boiling some water")
  val aliceCalling = ZIO.succeed("Alice's call")
  
  val preparingCoffeeWithSleep =
  preparingCoffee.debug(printThread) *>
    ZIO.sleep(5.seconds) *>
    ZIO.succeed("Coffee ready")

  val boilingWaterWithSleep =
  boilingWater.debug(printThread) *>
    ZIO.sleep(5.seconds) *>
    ZIO.succeed("Boiled water ready")


  def printThread = s"[${Thread.currentThread().getName}]"

  def concurrentWakeUpRoutineWithAliceCall(): ZIO[Clock, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread)
    boilingFiber <- boilingWaterWithSleep.fork
    _ <- aliceCalling.debug(printThread).fork *> boilingFiber.interrupt.debug(printThread)
    _ <- ZIO.succeed("Going to the Cafe with Alice").debug(printThread)
  } yield ()

  def concurrentWakeUpRoutineWithAliceCallingUsTooLate(): ZIO[Clock, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread)
    _ <- boilingWater.debug(printThread)
    coffeeFiber <- preparingCoffeeWithSleep.debug(printThread).fork.uninterruptible
    result <- aliceCalling.debug(printThread).fork *> coffeeFiber.interrupt.debug(printThread)
    _ <- result match {
        case Exit.Success(value) => ZIO.succeed("Making breakfast at home").debug(printThread)
        case _ => ZIO.succeed("Going to the Cafe with Alice").debug(printThread)
    }
    } yield ()

  override def run(args: List[String]) =
    concurrentWakeUpRoutineWithAliceCallingUsTooLate().exitCode
}



