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
    val mispredicted     = Input(Bool())
    val actualTarget     = Input(UInt(32.W))

    val prediction         = Output(Bool())
    val predictedTarget    = Output(UInt(32.W))
    val preloadLocal       = Output(Bool())
    val preloadHistory     = Output(Vec(2,Bool()))
    val correctPrediction  = Output(Bool())
    val predictTaken = Output(Bool())
  })

  val chooserSize   = 1024
  val chooserTable  = RegInit(VecInit(Seq.fill(chooserSize)(2.U(2.W)))) // when 1 choose local first, when 2 choose global first

  val local   = Module(new BranchPredictor)
  val global  = Module(new global_branch_predictor(3, 1024))

  val pcIndex = io.pc(11, 2)

  io.preloadLocal   := false.B
  io.preloadHistory := VecInit(Seq.fill(2)(false.B))

  local.io.pc              := io.pc
  local.io.branchTaken     := io.branchTaken
  local.io.branchTarget    := io.branchTarget
  local.io.update          := io.update
  local.io.preloadEnable   := io.preloadLocal
  local.io.preloadHistory  := io.preloadHistory


  global.io.currentPC      := io.pc
  global.io.branchTaken    := io.branchTaken
  global.io.update         := io.update
  global.io.shiftHistory   := io.shiftHistory
  global.io.resetHistory   := io.resetHistory
  global.io.branchTarget   := io.branchTarget

  val loop = global.io.loop


  val useGlobal     = chooserTable(pcIndex) >= 2.U
  val globalPred    = global.io.predictedNextPC
  val globalValid   = global.io.validPrediction
  val localPred     = local.io.nextPC
  val localValid    = true.B

  val chosenPred    = Wire(UInt(32.W))
  val chosenValid   = Wire(Bool())

when(io.mispredicted) {
  when(globalPred === io.actualTarget) {
    chooserTable(pcIndex) := 2.U

  }.elsewhen(localPred === io.actualTarget) {
    chooserTable(pcIndex) := 1.U

  }
}


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

  when (io.resetHistory){
  for(i<- 0 until chooserSize) {
    chooserTable(i) := 0.U
  }
  }

when(loop){
  io.preloadLocal := true.B
  io.preloadHistory := VecInit(Seq(true.B, true.B))
}

  val predictedTaken = chosenPred =/= (io.pc + 4.U)
  io.predictTaken := predictedTaken
}