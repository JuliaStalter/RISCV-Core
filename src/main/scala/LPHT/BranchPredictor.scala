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
    val prediction = Output(Bool())
    val nextPC = Output(UInt(32.W))


    val branchTaken = Input(Bool())
    val branchTarget = Input(UInt(32.W))
    val update = Input(Bool())

    val predictTaken = Output(Bool())

  })

  val lht = RegInit(0.U(2.W))
  val pcLast = RegInit(0.U(32.W))
  val nextPCReg = RegInit(0.U(32.W))

  val taken = Wire(Bool())
  taken := false.B

  when (io.update){
    taken := io.pc === pcLast
    lht := Cat(taken, lht(0))
    pcLast := io.pc
  }
io.prediction := true.B

  switch(Cat(lht(0), lht(1))){

  is("b11".U) {io.prediction := true.B }
  is("b00".U) {io.prediction := false.B}
  is("b10".U) {io.prediction := false.B}
  is("b01".U) {io.prediction := true.B }
  //default {io.prediction := false.B}
}
  nextPCReg := Mux(io.prediction, io.branchTarget, io.pc + 4.U)
  io.nextPC := nextPCReg


  io.predictTaken := io.branchTaken === io.prediction

}