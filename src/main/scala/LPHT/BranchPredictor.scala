package LPHT
import chisel3._
import chisel3.util._
import config.{ControlSignalsOB, Inst}


class BranchPredictor extends Module {

  val io = IO(new Bundle {

    val pc              = Input(UInt(32.W))
    val branchTaken     = Input(Bool())
    val branchTarget    = Input(UInt(32.W))
    val update          = Input(Bool())
    val preloadHistory = Input(Vec(2, Bool()))
    val preloadEnable   = Input(Bool())

    val prediction        = Output(Bool())
    val nextPC            = Output(UInt(32.W))
    val predictTaken      = Output(Bool())
    val correctPrediction = Output(Bool())

  })

  val lht = RegInit(VecInit(Seq.fill(2)(false.B)))


  when(io.preloadEnable) {
    lht(0) := io.preloadHistory(0)
    lht(1) := io.preloadHistory(1)
  }
  when(io.update) {
    lht(1):= lht(0)
    lht(0) := io.branchTaken
  }

  io.prediction := WireDefault(false.B)
  val lhtBits = Cat(lht(1), lht(0))
  switch(lhtBits) {

    is("b11".U) {
      io.prediction := true.B
    }
    is("b00".U) {
      io.prediction := false.B
    }
    is("b10".U) {
      io.prediction := false.B
    }
    is("b01".U) {
      io.prediction := true.B
    }
  }

  when(io.prediction) {
    io.nextPC := io.branchTarget
  }.otherwise {
    io.nextPC := (io.pc + 4.U)
  }
  io.predictTaken      := io.branchTaken === io.prediction
  io.correctPrediction := io.predictTaken


}
