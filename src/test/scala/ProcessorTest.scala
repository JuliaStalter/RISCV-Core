import chiseltest._
import chisel3._
import org.scalatest.flatspec.AnyFlatSpec
import RISCV_TOP.RISCV_TOP

class ProcessorTest_tb extends AnyFlatSpec with ChiselScalatestTester {


  "ProcessorTest" should "pass" in {
    test(new RISCV_TOP("src/test/programs/fir_filter")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val addresses = Array(
        BigInt("80000000", 16), BigInt("80000004", 16), BigInt("80000008", 16),
        BigInt("8000000C", 16), BigInt("80000010", 16), BigInt("80000014", 16)
      )

      val values = Array(5, 10, 15, 20, 25, 30)

      for (i <- 0 until addresses.length) {
        val addr = addresses(i)
        val value = values(i)

        dut.io.DMEMAddr.poke(addr.U(32.W))
        dut.io.DMEMWriteEnable.poke(true.B)
        dut.io.DMEMWriteData.poke(value.U(32.W))
        dut.clock.step(1)
        dut.io.DMEMWriteEnable.poke(false.B)
        dut.clock.step(1)


        dut.io.DMEMAddr.poke(addr.U(32.W))
        dut.io.DMEMReadEnable.poke(true.B)
        dut.clock.step(1)

        val readValue = dut.io.DMEMReadData.peek().litValue
        println(f"Memory[${addr.toString(16)}] = $readValue (Expected: $value)")
      }

      for (i <- 0 until 10) {
        val addr = BigInt("80000000", 16) + (i * 4)
        dut.io.DMEMAddr.poke(addr.U(32.W))
        dut.io.DMEMReadEnable.poke(true.B)
        dut.clock.step(1)

        val result = dut.io.DMEMReadData.peek().litValue
        println(f"Memory[$addr%08X] = $result")
      }

    }
  }
}
