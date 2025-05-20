package main_tb

import RISCV_TOP.RISCV_TOP
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import top_MC._
import chisel3._
import DataTypes.Data._

class main_tb extends AnyFlatSpec with ChiselScalatestTester {

  "Local Predictor" should "run test" in {
    test(new RISCV_TOP("src/test/programs/test_predictor"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.setup.poke(true.B)
        dut.io.predictorMode.poke(1.U) // Local
        dut.clock.step(2)
        dut.io.setup.poke(false.B)
        while (!dut.io.ecall.peek().litToBoolean) {
          dut.clock.step()
        }
      }
  }

  "Global Predictor" should "run test" in {
    test(new RISCV_TOP("src/test/programs/test_predictor"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.predictorMode.poke(2.U) // Global
        dut.io.setup.poke(true.B)

        dut.clock.step(2)
        dut.io.setup.poke(false.B)
        while (!dut.io.ecall.peek().litToBoolean) {
          dut.clock.step()
        }
      }
  }

  "Hybrid Predictor" should "run test" in {
    test(new RISCV_TOP("src/test/programs/test_predictor"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.predictorMode.poke(3.U) // Hybrid
        dut.io.setup.poke(true.B)
        dut.clock.step(2)
        dut.io.setup.poke(false.B)

        while (!dut.io.ecall.peek().litToBoolean) {
          dut.clock.step()
        }
      }
  }
}