
import zio.{UIO, ZIO, ExitCode}
import zio._


object ZioFibers extends zio.App {
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

  def printThread = s"[${Thread.currentThread().getName}]"

  def synchronousRoutine() = for{
    _ <- showerTime.debug(printThread + " - Showering")
    _ <- breakfastTime.debug(printThread + " - Breakfast")
    _ <- preparingCoffee.debug(printThread + " - Preparing coffee")
  } yield ()

  //fibers- async now

  def concurrentBathroomTimeAndBoilingWater(): ZIO[Any, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread).fork
    _ <- boilingWater.debug(printThread) } yield ()

  def concurrentWakeUpRoutine(): ZIO[Any, Nothing, Unit] = for {
    bathFiber <- bathTime.debug(printThread).fork
    boilingFiber <- boilingWater.debug(printThread).fork
    zippedFiber = bathFiber.zip(boilingFiber)
    result <- zippedFiber.join.debug(printThread)
    _ <- ZIO.succeed(s"$result...done").debug(printThread) *> preparingCoffee.debug(printThread)
  } yield ()

  override def run(args: List[String]) =
    concurrentWakeUpRoutine().exitCode
    //concurrentWakeUpRoutine().exitCode
    //concurrentBathroomTimeAndBoilingWater().exitCode
    //synchronousRoutine().exitCode
}


