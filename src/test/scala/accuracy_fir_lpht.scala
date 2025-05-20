package main_tb
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import RISCV_TOP.RISCV_TOP

import java.io._

class accuracy_fir_lpht extends AnyFlatSpec with ChiselScalatestTester {
  "LPHT" should "accuracy fir" in {
    test(new RISCV_TOP("src/test/programs/fir_filter_new"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        val writer = new PrintWriter(new File("fir_lpht_accuracy.csv"))
        writer.println("cycle,isBranch,wasPredicted,correctPrediction")

        dut.io.setup.poke(true.B)
        dut.io.predictorMode.poke(1.U)
        dut.clock.step(1)
        dut.io.setup.poke(false.B)

        var cycle = 0
        while (!dut.io.ecall.peek().litToBoolean && cycle < 800) {
          val correct = dut.io.correctPrediction.peek().litToBoolean
          val prediction =  dut.io.predictedTaken.peek().litToBoolean

          writer.println(s"$cycle,$prediction,$correct")
          dut.clock.step()
          cycle += 1
        }

        writer.close()
      }
  }
}