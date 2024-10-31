package LPHT
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class BranchPredictorTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "BranchPredictor"

  it should "correctly predict branch outcomes" in {
    test(new BranchPredictor) { dut =>
      // Define a sequence of PCs and branch outcomes
      val testCases = Seq(
        (0x00000000.U, false.B),
        (0x00000004.U, true.B),
        (0x00000008.U, false.B),
        (0x0000000C.U, true.B),
        (0x00000010.U, true.B)
      )

      // Test each case
      for ((pc, branchTaken) <- testCases) {
        dut.io.pc.poke(pc)
        dut.io.branchTaken.poke(branchTaken)
        dut.clock.step(1)

        // Verify prediction
        val expectedPrediction = false.B // Adjust based on your predictor logic
        dut.io.predictTaken.expect(expectedPrediction)
      }
    }
  }
}