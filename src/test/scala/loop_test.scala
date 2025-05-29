package main_tb
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import RISCV_TOP.RISCV_TOP
class loop_test extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "none"
  it should "loop_20_new" in {
    test(new RISCV_TOP("src/test/programs/loop_test"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.setup.poke(1.B)
        dut.io.predictionMode.poke(3.U)
        dut.clock.step()
        disableTestSignals

        dut.io.setup.poke(0.B)

        for(i <- 0 until 200){
          dut.clock.step()
        }


        def disableTestSignals: Unit = {
          dut.io.setup.poke(1.B)
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
