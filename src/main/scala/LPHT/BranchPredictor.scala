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
    val preloadHistory  = Input(UInt(2.W))
    val preloadEnable   = Input(Bool())

    val prediction          = Output(Bool())
    val nextPC              = Output(UInt(32.W))
    val predictTaken        = Output(Bool())
    val correctPrediction   = Output(Bool())

  })

  val tableSize = 256
  val table     = RegInit(VecInit(Seq.fill(tableSize)(0.U(2.W))))
  val pcIndex   = io.pc(9, 2)

  val counter    = table(pcIndex)
  val prediction = WireDefault(false.B)

  switch(counter) {

    is("b11".U) { prediction := true.B }
    is("b10".U) { prediction := true.B }
    is("b01".U) { prediction := false.B }
    is("b00".U) { prediction := false.B }

  }

  val nextPC = Mux(prediction, io.branchTarget, io.pc + 4.U)

  io.prediction        := prediction
  io.nextPC            := nextPC
  io.correctPrediction := prediction === io.branchTaken

  when(io.preloadEnable) {

    table(pcIndex) := io.preloadHistory

  }. elsewhen (io.update) {

    val updated = MuxLookup(counter, counter, Seq(

      "b00".U -> Mux(io.branchTaken, "b01".U, "b00".U),
      "b01".U -> Mux(io.branchTaken, "b10".U, "b00".U),
      "b10".U -> Mux(io.branchTaken, "b11".U, "b01".U),
      "b11".U -> Mux(io.branchTaken, "b11".U, "b10".U)
    ))

    table(pcIndex) := updated

  }

  io.predictTaken := io.branchTaken === prediction
/*
  val lht = RegInit(0.U(2.W))
  val nextPCReg = RegInit(0.U(32.W))


  when(io.preloadEnable) {
    lht := io.preloadHistory
  }

  when (io.update){
    lht := Cat(io.branchTaken, lht(0))
  }
  io.prediction := WireDefault(false.B)

  switch(Cat(lht(0), lht(1))){

  is("b11".U) {io.prediction := true.B }
  is("b00".U) {io.prediction := false.B}
  is("b10".U) {io.prediction := false.B}
  is("b01".U) {io.prediction := true.B }
}
  nextPCReg := Mux(io.prediction, io.branchTarget, io.pc + 4.U)
  io.nextPC := nextPCReg


  io.predictTaken := io.branchTaken === io.prediction
  io.correctPrediction := io.predictTaken
*/
}
