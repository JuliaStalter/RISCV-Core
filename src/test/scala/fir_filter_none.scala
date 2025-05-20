package main_tb
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import RISCV_TOP.RISCV_TOP

class fir_filter_none extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "none"
  it should "run FIR" in {
    test(new RISCV_TOP("src/test/programs/fir_filter_new"))

      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.setup.poke(true.B)
        dut.io.predictorMode.poke(0.U)
        dut.clock.step(2)
        dut.io.setup.poke(false.B)
        dut.clock.setTimeout(0)
        for (_ <- 0 until 800) {
          dut.clock.step()
        }

      }

  }

}