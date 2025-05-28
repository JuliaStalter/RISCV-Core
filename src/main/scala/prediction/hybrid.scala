
package prediction

import chisel3._
import chisel3.util._

class hybrid extends Module {

  val io = IO(new Bundle {

    val pc            = Input(UInt(32.W))
    val branchTaken   = Input(Bool())
    val branchTarget  = Input(UInt(32.W))
    val update        = Input(Bool())
    val shiftHistory  = Input(Bool())
    val resetHistory  = Input(Bool())
    val mispredicted     = Input(Bool())
    val actualTarget     = Input(UInt(32.W))

    val hybridPrediction         = Output(Bool())
    val hybridpredictedTarget    = Output(UInt(32.W))
    val hybridcorrectPrediction  = Output(Bool())
    val hybridHit                = Output(Bool()) // Always true or computed
    val hybridNextPC             = Output(UInt(32.W)) // Same as predictedTarget
    val preloadLocal             = Output(Bool())

  })

  val chooserSize   = 1024
  val chooserTable  = RegInit(VecInit(Seq.fill(chooserSize)(2.U(2.W)))) // when 1 choose local first, when 2 choose global first

  val lpht   = Module(new lpht)
  val gpht  = Module(new gpht(3, 1024))

  val pcIndex = io.pc(7, 2)

  io.preloadLocal   := false.B

  lpht.io.pc              := io.pc
  lpht.io.branchTaken     := io.branchTaken
  lpht.io.branchTarget    := io.branchTarget
  lpht.io.update          := io.update
  lpht.io.preloadEnable   := io.preloadLocal
  lpht.io.entryPC         := io.pc
  lpht.io.entryTarget     := io.branchTarget



  gpht.io.pc             := io.pc
  gpht.io.branchTaken    := io.branchTaken
  gpht.io.update         := io.update
  gpht.io.shiftHistory   := io.shiftHistory
  gpht.io.resetHistory   := io.resetHistory
  gpht.io.branchTarget   := io.branchTarget

  val loop = gpht.io.gphtloop


  val useGlobal     = chooserTable(pcIndex) >= 2.U
  val globalPred    = gpht.io.gphtpredictedTarget
  val globalValid   = gpht.io.gphtcorrectPrediction
  val localPred     = lpht.io.lphtpredictedTarget
  val localValid    = lpht.io.correctPrediction

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

  when(io.update) {

    val localCorrect  = localPred === io.branchTarget
    val globalCorrect = globalPred === io.branchTarget

    when(localCorrect && !globalCorrect && chooserTable(pcIndex) > 0.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) - 1.U

    }.elsewhen(globalCorrect && !localCorrect && chooserTable(pcIndex) < 3.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) + 1.U

    }.elsewhen(!localCorrect && !globalCorrect && chooserTable(pcIndex) > 0.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) - 1.U
    }

  }

  when (io.resetHistory){
    for(i<- 0 until chooserSize) {
      chooserTable(i) := 2.U        //2.U wenn normal global verwendet werden soll, 1.U wenn normal local verwendet werden soll
    }
  }

  when(loop){
    io.preloadLocal := true.B

  }
  io.hybridpredictedTarget   := chosenPred
  io.hybridPrediction        := chosenValid
  io.hybridcorrectPrediction := io.hybridpredictedTarget === io.branchTarget
  io.hybridHit := true.B
  io.hybridNextPC := chosenPred
}
