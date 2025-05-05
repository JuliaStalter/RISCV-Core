package hybrid

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Hybrid_Test extends AnyFlatSpec with ChiselScalatestTester {
  "hybrid_predictor" should "autonomously predict correctly over time" in {
    test(new hybrid_predictor()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val sequence = Seq(
        (0x10.U, true.B, 0x20.U),
        (0x20.U, true.B, 0x30.U),
        (0x30.U, true.B, 0x40.U),
        (0x40.U, true.B, 0x50.U),
        (0x50.U, true.B, 0x60.U),
        (0x60.U, true.B, 0x10.U)  // loop back
      )

      // Reset history to start clean
      dut.io.resetHistory.poke(true.B)
      dut.io.shiftHistory.poke(false.B)
      dut.io.branchTaken.poke(false.B)
      dut.io.update.poke(false.B)
      dut.io.branchTarget.poke(0.U)
      dut.io.pc.poke(0.U)
      dut.clock.step()
      dut.io.resetHistory.poke(false.B)

      var correct = 0
      var total = 0

      // Repeat branch pattern to simulate "real" execution
      for (_ <- 0 until 20) {
        for ((pc, taken, target) <- sequence) {
          dut.io.pc.poke(pc)
          dut.io.branchTaken.poke(taken)
          dut.io.branchTarget.poke(target)
          dut.io.update.poke(true.B)
          dut.io.shiftHistory.poke(true.B)

          val predicted = dut.io.predictedTarget.peek().litValue
          val valid = dut.io.prediction.peek().litToBoolean

          if (valid && predicted == target.litValue) {
            correct += 1
          }

          total += 1
          dut.clock.step()
        }
      }

      val accuracy = if (total > 0) 100.0 * correct / total else 0.0
      println(f"Hybrid predictor accuracy: $correct / $total = ${accuracy}%.2f%%")
    }
  }
}



/*

package hybrid
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Hybrid_Test extends AnyFlatSpec with ChiselScalatestTester {
  "hybrid_predictor" should "select between LPHT and GPHT correctly" in {
    test(new hybrid_predictor()) { dut =>
      val pcA = 0x10.U
      val pcB = 0x20.U
      val pcC = 0x30.U
      val pcD = 0x40.U
      val pcE = 0x50.U
      val pcF = 0x60.U
      val pcG = 0x70.U
      val pcH = 0x80.U

      def trainHybrid(seq: Seq[UInt], target: UInt): Unit = {
        for (i <- 0 until seq.length) {
          val pc = seq(i)
          val nextTarget = if (i == seq.length - 1) target else seq(i + 1)

          dut.io.pc.poke(pc)
          dut.io.branchTaken.poke(true.B)
          dut.io.update.poke((i == seq.length - 1).B)
          dut.io.shiftHistory.poke((i != seq.length - 1).B)
          dut.io.branchTarget.poke(nextTarget)
          dut.io.resetHistory.poke(false.B)
          dut.clock.step()
        }
      }

      def predictHybrid(seq: Seq[UInt], expected: UInt): Unit = {
        for (i <- 0 until seq.length) {
          val pc = seq(i)
          dut.io.pc.poke(pc)
          dut.io.branchTaken.poke(true.B)
          dut.io.update.poke(false.B)
          dut.io.shiftHistory.poke((i != seq.length - 1).B)
          dut.io.branchTarget.poke(0.U)
          dut.io.resetHistory.poke(false.B)
          dut.clock.step()
        }


        val predictedTarget = dut.io.predictedTarget.peek().litValue
        val expectedTarget = expected.litValue
        println(f"Predicted Target: 0x$predictedTarget%04x, Expected: 0x${expected.litValue}%04x")
        assert(predictedTarget == expectedTarget, s"Hybrid predictor mismatch: expected 0x${expectedTarget.toString(16)}, got 0x${predictedTarget.toString(16)}")
      }

      def resetHistory(): Unit = {
        dut.io.resetHistory.poke(true.B)
        dut.io.shiftHistory.poke(false.B)
        dut.io.branchTarget.poke(0.U)
        dut.io.branchTaken.poke(false.B)
        dut.io.update.poke(false.B)
        dut.clock.step()
        dut.io.resetHistory.poke(false.B)
      }

      println("=== TRAINING ===")
      for (_ <- 0 until 15) {
        trainHybrid(Seq(pcA, pcB, pcC), pcD)

        trainHybrid(Seq(pcB, pcC, pcD), pcE)

        trainHybrid(Seq(pcC, pcD, pcE), pcF)

        trainHybrid(Seq(pcF, pcG, pcH), pcA)

        trainHybrid(Seq(pcG, pcH, pcA), pcB)
      }

      println("=== TESTING ===")
      for (_ <- 0 until 15) {

        predictHybrid(Seq(pcA, pcB, pcC), pcD)

        predictHybrid(Seq(pcB, pcC, pcD), pcE)

        predictHybrid(Seq(pcC, pcD, pcE), pcF)

        predictHybrid(Seq(pcF, pcG, pcH), pcA)

        predictHybrid(Seq(pcG, pcH, pcA), pcB)
      }
    }
  }
}

*/