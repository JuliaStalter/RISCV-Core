
package prediction

import chisel3._
import chisel3.util._

class hybrid extends Module {

  val io = IO(new Bundle {

    val pc            = Input(UInt(32.W))
    val entryPC         = Input(UInt(32.W))
    val branchTaken   = Input(Bool())
    val branchTarget  = Input(UInt(32.W))
    val entryTarget     = Input(UInt(32.W))
    val update        = Input(Bool())
    val shiftHistory  = Input(Bool())
    val resetHistory  = Input(Bool())
    val mispredicted     = Input(Bool())
    val actualTarget    = Input(UInt(32.W))

    val hybridPrediction         = Output(Bool())
    val hybridpredictedTarget    = Output(UInt(32.W))
    val hybridcorrectPrediction  = Output(Bool())
    val hybridHit                = Output(Bool()) // Always true or computed
    val hybridNextPC             = Output(UInt(32.W)) // Same as predictedTarget
    val preloadEnable            = Output(Bool())

  })

  val chooserSize   = 1024
  val chooserTable  = RegInit(VecInit(Seq.fill(chooserSize)(0.U(2.W)))) // when 1 choose local first, when 2 choose global first

  val lpht   = Module(new lpht)
  val gpht  = Module(new gpht(3, 1024))

  val pcIndex = io.pc(7, 2)

  io.preloadEnable   := false.B

  gpht.io.pc             := io.pc
  gpht.io.branchTaken    := io.branchTaken
  gpht.io.update         := io.update
  gpht.io.shiftHistory   := io.shiftHistory
  gpht.io.resetHistory   := io.resetHistory
  gpht.io.branchTarget   := io.branchTarget

  val loop = gpht.io.gphtloop

  when(loop){
    io.preloadEnable := true.B

  }

  lpht.io.pc              := io.pc
  lpht.io.branchTaken     := io.branchTaken
  lpht.io.branchTarget    := io.branchTarget
  lpht.io.update          := io.update
  lpht.io.preloadEnable   := io.preloadEnable
  lpht.io.entryPC         := io.entryPC
  lpht.io.entryTarget     := io.entryTarget

  val useGlobal     = chooserTable(pcIndex) >= 2.U

  val globalPred    = gpht.io.gphtpredictedTarget
  val globalValid   = gpht.io.gphtcorrectPrediction
  val localPred     = lpht.io.lphtpredictedTarget
  val localValid    = lpht.io.correctPrediction

  val regGlobalValid = RegNext(globalValid, false.B)
  val regLocalValid  = RegNext(localValid,  false.B)

  val chosenPred    = Wire(UInt(32.W))
  val chosenValid   = Wire(Bool())


  chosenPred  := Mux(useGlobal, globalPred, localPred)
  chosenValid := Mux(useGlobal, regGlobalValid, regLocalValid)

  when(io.mispredicted) {
    when(globalPred === io.actualTarget) {
      chooserTable(pcIndex) := 2.U

    }.elsewhen(localPred === io.actualTarget) {
      chooserTable(pcIndex) := 1.U
    }
  }

    when(io.update && !io.mispredicted) {

    val localCorrect  = localPred === io.branchTarget
    val globalCorrect = globalPred === io.branchTarget

    when(localCorrect && !globalCorrect && chooserTable(pcIndex) > 0.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) - 1.U


    }.elsewhen(globalCorrect && !localCorrect && chooserTable(pcIndex) < 3.U) {

      chooserTable(pcIndex) := chooserTable(pcIndex) + 1.U

    }.elsewhen(!localCorrect && !globalCorrect && chooserTable(pcIndex) > 0.U) {

      chooserTable(pcIndex) := 2.U

    }.elsewhen(localCorrect && globalCorrect && chooserTable(pcIndex) < 3.U){

      chooserTable(pcIndex) := chooserTable(pcIndex) + 1.U
    }

  }

  when (io.resetHistory){
    for(i<- 0 until chooserSize) {
      chooserTable(i) := 2.U        //2.U wenn normal global verwendet werden soll, 1.U wenn normal local verwendet werden soll
    }
  }

  val selectedPrediction       = Mux(useGlobal, gpht.io.gphtPrediction, lpht.io.prediction)

  io.hybridpredictedTarget   := chosenPred
  io.hybridPrediction        := selectedPrediction
  io.hybridcorrectPrediction :=  (chosenPred === io.actualTarget) && selectedPrediction
  io.hybridNextPC := chosenPred
  io.hybridHit  := selectedPrediction
}
