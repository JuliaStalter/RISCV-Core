package prediction

import chisel3._
import chisel3.util._
import config.{ControlSignalsOB, Inst}

class lpht extends Module {

  val io = IO(new Bundle {

    val pc              = Input(UInt(32.W))
    val branchTaken     = Input(Bool())
    val branchTarget    = Input(UInt(32.W))

    val update          = Input(Bool())
    val preloadEnable   = Input(Bool())

    val entryPC         = Input(UInt(32.W))
    val entryTarget     = Input(UInt(32.W))

    val prediction        = Output(Bool())
    val nextPC            = Output(UInt(32.W))
    val predictTaken      = Output(Bool())
    val correctPrediction = Output(Bool())
    val lphtHit           = Output(Bool())
    val lphtpredictedTarget = Output(UInt(32.W))
  })

  val lht = RegInit(VecInit(Seq.fill(2)(false.B)))

  val TargetTable = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
  val validTable = RegInit(VecInit(Seq.fill(64)(false.B)))

  val index =Wire(UInt(6.W))
  index := Mux(io.update,io.entryPC(7,2), io.pc(7,2))

  val target = TargetTable(index)
  val valid = validTable(index)

  io.prediction := WireDefault(false.B)
  io.nextPC := WireDefault(io.pc + 4.U)
  io.lphtpredictedTarget := WireDefault(0.U)
  io.lphtHit := WireDefault(false.B)


  when(io.preloadEnable) {
    lht(0) := 1.U
    lht(1) := 1.U
  }
  when(io.update) {
    lht(1) := lht(0)
    lht(0) := io.branchTaken


    when(io.branchTaken) {
      TargetTable(io.entryPC(7, 2)) := io.entryTarget
      validTable(io.entryPC(7, 2)) := true.B
    }
  }


  val prediction = WireDefault(false.B)

  val lhtBits = Cat(lht(1), lht(0))
  switch(lhtBits) {

    is("b11".U) {
      prediction := true.B
    }
    is("b00".U) {
      prediction := false.B
    }
    is("b10".U) {
      prediction := false.B
    }
    is("b01".U) {
      prediction := true.B
    }
  }
  io.prediction := prediction



  io.nextPC := Mux(prediction, target, io.pc + 4.U)
  io.lphtpredictedTarget := Mux(prediction && valid, target, 0.U)
  io.lphtHit := prediction && valid
  io.predictTaken := io.branchTaken === prediction
  io.correctPrediction := io.predictTaken
}