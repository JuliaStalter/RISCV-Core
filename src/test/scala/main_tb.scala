package main_tb
import RISCV_TOP.RISCV_TOP
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import top_MC._
import chisel3._
import DataTypes.Data._
import java.sql.Driver

class main_tb extends AnyFlatSpec with ChiselScalatestTester {

  "main_tb" should "pass" in {
    test(new RISCV_TOP("src/test/programs/test_predictor")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val modes = Seq(
        0 -> "No Prediction",
        1 -> "LPHT",
        2 -> "GPHT",
        3 -> "Hybrid"
      )
      // for testing predictors using this to set the right predictor (Julia Stalter)
      for ((mode, label) <- modes) {
        println(s"\n=== Running with $label (mode = $mode) ===\n")

        dut.reset.poke(true.B)
        dut.clock.step(2)
        dut.reset.poke(false.B)

        dut.io.predictorMode.poke(mode.U)

        dut.io.setup.poke(1.B)
        disableTestSignals()
        dut.clock.step(10)
        dut.io.setup.poke(0.B)

        var cycles = 0
        val maxCycles = 1000 // safety cap
        while (cycles < maxCycles&& !dut.io.ecall.peek().litToBoolean) {
         // dut.clock.step()
          println(f"[Cycle $cycles%3d] PC = 0x${dut.io.PC.peek().litValue}%08x")
          cycles += 1
        }

        println(s"\n=== Finished $label ===\n")
      }

      def disableTestSignals(): Unit = {
        dut.io.DMEMWriteData.poke(0.U)
        dut.io.DMEMAddr.poke(0.U)
        dut.io.DMEMWriteEnable.poke(0.B)
        dut.io.regsWriteData.poke(0.U)
        dut.io.regsAddr.poke(0.U)
        dut.io.regsWriteEnable.poke(0.B)
        dut.io.IMEMWriteData.poke(0.U)
        dut.io.IMEMAddr.poke(4092.U)
      }
    }
  }

}