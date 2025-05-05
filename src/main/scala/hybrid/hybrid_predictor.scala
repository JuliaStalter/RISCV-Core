package hybrid

import chisel3._
import chisel3.util._
import LPHT.BranchPredictor
import GPHT.global_branch_predictor

class hybrid_predictor extends Module {

  val io = IO(new Bundle {

    val pc            = Input(UInt(32.W))
    val branchTaken   = Input(Bool())
    val branchTarget  = Input(UInt(32.W))
    val update        = Input(Bool())
    val shiftHistory  = Input(Bool())
    val resetHistory  = Input(Bool())


    val prediction         = Output(Bool())
    val predictedTarget    = Output(UInt(32.W))
    val preloadLocal       = Output(Bool())
    val preloadHistory     = Output(UInt(2.W))
    val correctPrediction  = Output(Bool())
  })

  val chooserSize   = 1024
  val chooserTable  = RegInit(VecInit(Seq.fill(chooserSize)(2.U(2.W))))

  val local   = Module(new BranchPredictor)
  val global  = Module(new global_branch_predictor(3, 1024))

  val pcIndex = io.pc(11, 2)


  local.io.pc              := io.pc
  local.io.branchTaken     := io.branchTaken
  local.io.branchTarget    := io.branchTarget
  local.io.update          := io.update
  local.io.preloadEnable   := false.B
  local.io.preloadHistory  := 0.U


  global.io.currentPC      := io.pc
  global.io.branchTaken    := io.branchTaken
  global.io.update         := io.update
  global.io.shiftHistory   := io.shiftHistory
  global.io.resetHistory   := io.resetHistory
  global.io.branchTarget   := io.branchTarget


  val useGlobal     = chooserTable(pcIndex) >= 2.U
  val globalPred    = global.io.predictedNextPC
  val globalValid   = global.io.validPrediction
  val localPred     = local.io.nextPC
  val localValid    = true.B

  val chosenPred    = Wire(UInt(32.W))
  val chosenValid   = Wire(Bool())


  when(useGlobal) {

    chosenPred  := globalPred
    chosenValid := globalValid

  }.otherwise {

    chosenPred  := localPred
    chosenValid := localValid

  }

  io.predictedTarget   := chosenPred
  io.prediction        := chosenValid
  io.correctPrediction := io.predictedTarget === io.branchTarget


  when(io.update) {

    val localCorrect  = localPred === io.branchTarget
    val globalCorrect = globalPred === io.branchTarget

    when(localCorrect && !globalCorrect && chooserTable(pcIndex) > 0.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) - 1.U

    }.elsewhen(globalCorrect && !localCorrect && chooserTable(pcIndex) < 3.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) + 1.U

    }
  }


  io.preloadLocal   := false.B
  io.preloadHistory := 0.U
}