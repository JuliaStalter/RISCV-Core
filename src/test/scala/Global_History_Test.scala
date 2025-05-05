package GPHT

import chisel3._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Global_History_Test extends AnyFlatSpec with ChiselScalatestTester {
  "global_branch_predictor" should "track prediction accuracy over multiple branches" in {
    test(new global_branch_predictor(3, 1024)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val branches = Seq(
        (0x10.U, true.B,  0x20.U),
        (0x14.U, false.B, 0x18.U),
        (0x18.U, true.B,  0x30.U),
        (0x1C.U, true.B,  0x28.U),
        (0x20.U, false.B, 0x24.U),
        (0x24.U, true.B,  0x40.U)
      )

      var correct = 0
      var total = 0

      dut.io.resetHistory.poke(true.B)
      dut.io.shiftHistory.poke(false.B)
      dut.io.branchTaken.poke(false.B)
      dut.io.update.poke(false.B)
      dut.io.branchTarget.poke(0.U)
      dut.io.currentPC.poke(0.U)
      dut.clock.step()

      dut.io.resetHistory.poke(false.B)


      for (_ <- 0 until 10) {
        for ((pc, taken, target) <- branches) {

          dut.io.currentPC.poke(pc)
          dut.io.branchTaken.poke(taken)
          dut.io.branchTarget.poke(target)
          dut.io.update.poke(true.B)
          dut.io.shiftHistory.poke(true.B)


          val predicted = dut.io.predictedNextPC.peek().litValue
          val valid = dut.io.validPrediction.peek().litToBoolean

          if (valid && predicted == target.litValue) {
            correct += 1
          }

          total += 1
          dut.clock.step()
        }
      }

      val accuracy = if (total > 0) 100.0 * correct / total else 0.0
      println(s"Global predictor accuracy: $correct / $total = ${"%.2f".format(accuracy)}%")
    }
  }
}

/*

 Working Testbench with manual training and testing

package GPHT


import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Global_History_Test extends AnyFlatSpec with ChiselScalatestTester {
  "global_branch_predictor" should "predict next branch correctly from sequence" in {
    test(new global_branch_predictor(historyLength = 3, tableSize = 1024)) { dut =>
      val pcA = 0x10.U
      val pcB = 0x20.U
      val pcC = 0x30.U
      val pcD = 0x40.U
      val pcE = 0x50.U
      val pcF = 0x60.U


      def trainSequence(seq: Seq[UInt], nextPC: UInt): Unit = {
        for (pc <- seq.dropRight(1)) {
          dut.io.currentPC.poke(pc)
          dut.io.branchTaken.poke(true.B)
          dut.io.update.poke(false.B)
          dut.io.shiftHistory.poke(true.B)
          dut.io.branchTarget.poke(0.U)
          dut.io.resetHistory.poke(false.B)
          dut.clock.step()
        }


        val lastPC = seq.last
        dut.io.currentPC.poke(lastPC)
        dut.io.branchTaken.poke(true.B)
        dut.io.update.poke(true.B)
        dut.io.shiftHistory.poke(true.B)
        dut.io.branchTarget.poke(nextPC)
        dut.io.resetHistory.poke(false.B)
        dut.clock.step()
      }


      def predictFrom(seq: Seq[UInt], expected: UInt): Unit = {
        for (pc <- seq) {
          dut.io.currentPC.poke(pc)
          dut.io.branchTaken.poke(true.B)
          dut.io.shiftHistory.poke(true.B)
          dut.io.update.poke(false.B)
          dut.io.branchTarget.poke(0.U)
          dut.io.resetHistory.poke(false.B)
          dut.clock.step()
        }

        dut.io.branchTaken.poke(false.B)
        dut.io.update.poke(false.B)
        dut.io.branchTarget.poke(0.U)
        dut.io.shiftHistory.poke(true.B)
        dut.io.resetHistory.poke(false.B)


        val pred = dut.io.predictedNextPC.peek().litValue
        val expectedVal = expected.litValue
        println(f"Predicted PC: 0x$pred%04x (Expected: 0x$expectedVal%04x)")
        assert(pred == expectedVal, "Prediction mismatch")
      }

      def resetHistory(): Unit = {
        dut.io.resetHistory.poke(true.B)
        dut.clock.step()
        dut.io.resetHistory.poke(false.B)
      }

      println("=== TRAINING PHASE ===")
      trainSequence(Seq(pcA, pcB, pcC), pcD)
      dut.clock.step(1)
      trainSequence(Seq(pcB, pcC, pcD), pcE)
      dut.clock.step(1)
      trainSequence(Seq(pcC, pcD, pcE), pcF)
      dut.clock.step(1)
      resetHistory()
      println("=== TESTING PHASE ===")

      predictFrom(Seq(pcA, pcB, pcC), pcD)

      predictFrom(Seq(pcB, pcC, pcD), pcE)

      predictFrom(Seq(pcC, pcD, pcE), pcF)

      println("All predictions passed.")
    }
  }
}

*/
