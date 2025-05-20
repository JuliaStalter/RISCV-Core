package GPHT

import chisel3._
import chisel3.util._

class global_branch_predictor(val historyLength: Int = 3, val tableSize: Int = 1024) extends Module {
  val io = IO(new Bundle {


    val currentPC     = Input(UInt(32.W))
    val branchTaken   = Input(Bool())
    val update        = Input(Bool())
    val branchTarget  = Input(UInt(32.W))
    val shiftHistory  = Input(Bool())
    val resetHistory  = Input(Bool())


    val correctPrediction  = Output(Bool())
    val predictedNextPC    = Output(UInt(32.W))
    val validPrediction    = Output(Bool())
    val historyOut         = Output(UInt(historyLength.W))
    val loop               = Output(Bool())
  })



  val globalHistory   = RegInit(0.U(historyLength.W))
  val predictionTable = RegInit(VecInit(Seq.fill(tableSize)(0.U(32.W))))
  val index           = Cat(io.currentPC(9, 2), globalHistory)(log2Ceil(tableSize)-1, 0)
  val predictedTarget = predictionTable(index)


  io.predictedNextPC :=  predictedTarget
  io.validPrediction := predictedTarget =/= 0.U
  io.historyOut      := globalHistory

  when(io.update) {

    predictionTable(index) := io.branchTarget

  }

  when(io.resetHistory) {

    globalHistory := 0.U

  } .elsewhen(io.shiftHistory) {

    globalHistory := Cat(globalHistory(historyLength - 2, 0), io.branchTaken)

  }

  val loop = globalHistory === "b111".U
  io.loop := loop

  val wasPredictionCorrect = (predictedTarget === io.branchTarget) && io.validPrediction
  io.correctPrediction := wasPredictionCorrect

}