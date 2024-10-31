package LPHT
import chisel3._
import chisel3.util._
import config.{ControlSignalsOB, Inst}


class LPHT extends Bundle{

  val counter = UInt(2.W)

}

class BranchPredictor extends Module {

  val io = IO(new Bundle {

    val pc = Input(UInt(32.W))
    val branchTaken = Input(Bool())
    val branchTarget = Input(UInt(32.W))
    val update = Input(Bool())
    val predictTaken = Output(Bool())
    val nextPC = Output(UInt(32.W))
  })

  val LHT_SIZE = 1024
  val PHT_SIZE = 256

  val lht = Mem(LHT_SIZE, UInt(log2Ceil(PHT_SIZE).W))
  val pht = Mem(PHT_SIZE, UInt(2.W))

  val lhtIndex = io.pc(log2Ceil(LHT_SIZE) - 1,0)
  val phtIndex = lht(lhtIndex)

  val counter = pht(phtIndex)
  io.predictTaken := counter(1)
  io.nextPC := Mux(io.predictTaken, io.branchTarget, io.pc + 4.U)

  when(io.update){

    when(io.branchTaken){

      when(counter =/= 3.U) {

        pht(phtIndex) := counter + 1.U

      }

    }.otherwise {

      when(counter =/= 0.U){

        pht(phtIndex) := counter - 1.U

      }

    }

    val newLHTValue = Cat(lht(lhtIndex)(log2Ceil(PHT_SIZE) - 2,0), io.branchTaken)
    lht(lhtIndex) := newLHTValue

  }
}
