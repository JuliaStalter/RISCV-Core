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

  val lht = RegInit(VecInit(Seq(true.B,false.B)))

  val TargetTable = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
  val validTable = RegInit(VecInit(Seq.fill(64)(false.B)))

  val predIndex = io.pc(7,2)
  val updateIndex = io.entryPC(7,2)


  val prediction = WireDefault(false.B)

  val lhtBits = Cat(lht(1), lht(0))
  val predictedTarget = TargetTable(predIndex)
  val isValid         = validTable(predIndex)



  when(io.preloadEnable) {
    lht(0) := 1.U
    lht(1) := 1.U
  }


  when(io.update) {
    lht(1) := lht(0)
    lht(0) := io.branchTaken

  }
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

  val regPrediction = RegNext(prediction)
  val regValid      = RegNext(validTable(predIndex))
  val regTarget     = RegNext(TargetTable(predIndex))

  io.prediction          := regPrediction
  io.lphtHit             := regPrediction && regValid
  io.lphtpredictedTarget := Mux(io.lphtHit, regTarget, 0.U)
  io.nextPC              := Mux(io.lphtHit, regTarget, io.pc + 4.U)
  io.correctPrediction   := (prediction === io.branchTaken)
  io.predictTaken := regPrediction

    when(io.branchTaken) {
      TargetTable(updateIndex) := io.entryTarget
      validTable(updateIndex) := true.B
    }

}