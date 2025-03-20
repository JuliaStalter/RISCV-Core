package LPHT

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class BranchPredictorTest extends AnyFlatSpec with ChiselScalatestTester {

  "BranchPredictor" should "predict branches correctly in a loop pattern" in {
    test(new BranchPredictor) { dut =>


      val loopStart = 0x100.U
      val loopEnd = 0x120.U
      val loopTarget = loopStart
      val numIterations = 10


      dut.io.pc.poke(0.U)
      dut.io.branchTaken.poke(false.B)
      dut.io.branchTarget.poke(0.U)
      dut.io.update.poke(false.B)
      dut.clock.step(5)


      for (i <- 0 until numIterations) {
        dut.io.pc.poke(loopEnd)
        dut.io.branchTaken.poke(true.B)
        dut.io.branchTarget.poke(loopTarget)
        dut.io.update.poke(true.B)
        dut.clock.step(1)


        dut.io.pc.poke(loopEnd)
        dut.io.update.poke(false.B)
        dut.clock.step(1)

        val prediction = dut.io.prediction.peek().litToBoolean
        println(s"Iteration $i: Prediction = $prediction")

        if (i > 3) {
          assert(prediction == true, s"Failed at iteration $i: expected true")
        }
      }
    }
  }
}