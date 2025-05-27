/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package Stage_IF

import chisel3._
import chisel3.util._
import config.{ControlSignals, IMEMsetupSignals, Inst, Instruction}
import config.Inst._
import InstructionMemory.InstructionMemory
import prediction.lpht
import prediction.gpht
import prediction.hybrid

class IF(BinaryFile: String) extends Module
{

  val testHarness = IO(
    new Bundle {
      val InstructionMemorySetup = Input(new IMEMsetupSignals)
      val PC        = Output(UInt())
    }
  )


  val io = IO(new Bundle {
    val branchAddr         = Input(UInt())
    val IFBarrierPC        = Input(UInt())
    val stall              = Input(Bool())
    val predictionMode      = Input(UInt(3.W))

    // Inputs for BTB, will come from EX stage and Hazard Unit
    val updatePrediction   = Input(Bool())
    val newBranch          = Input(Bool())
    val entryPC            = Input(UInt(32.W))
    val branchTaken        = Input(Bool())  // 1 means Taken -- 0 means Not Taken
    val branchMispredicted = Input(Bool())
    val shiftHistory       = Input(Bool())
    val PCplus4ExStage     = Input(UInt(32.W))
    val btbHit             = Output(Bool())
    val predictorHit            = Output(Bool())
    val btbPrediction      = Output(Bool())
    val predictorPrediction     = Output(Bool())
    val predictorpredictedTarget = Output(UInt(32.W))
    val btbTargetPredict   = Output(UInt(32.W))
    val PC                 = Output(UInt())
    val instruction        = Output(new Instruction)
  })

  val InstructionMemory = Module(new InstructionMemory(BinaryFile))
  val BTB               = Module(new BTB_2way) // changed to work in parallel with the lpht first
  val nextPC            = WireInit(UInt(), 0.U)
  val PC                = RegInit(UInt(32.W), 0.U)
  val PCplus4           = Wire(UInt(32.W))
  val instruction       = Wire(new Instruction)
  val branch            = WireInit(Bool(), false.B)
  val lpht              = Module(new lpht)
  val gpht              = Module(new gpht(3,1024))
  val hybrid            = Module(new hybrid)

  InstructionMemory.testHarness.setupSignals := testHarness.InstructionMemorySetup
  testHarness.PC := InstructionMemory.testHarness.requestedAddress

  instruction := InstructionMemory.io.instruction.asTypeOf(new Instruction)

  // Adder to increment PC
  PCplus4 := PC + 4.U

  // BTB signals
  BTB.io.currentPC := PC
  BTB.io.newBranch := io.newBranch
  BTB.io.updatePrediction := io.updatePrediction
  BTB.io.entryPC := io.entryPC
  BTB.io.entryBrTarget := io.branchAddr
  BTB.io.branchMispredicted := io.branchMispredicted
  BTB.io.stall := io.stall
  io.btbPrediction := BTB.io.prediction
  io.btbHit := BTB.io.btbHit
  io.btbTargetPredict := BTB.io.targetAdr

  //lpht signals

  lpht.io.pc := PC
  lpht.io.branchTaken := io.branchTaken
  lpht.io.update := io.updatePrediction
  lpht.io.branchTarget := io.branchAddr
  lpht.io.preloadEnable := false.B
  lpht.io.entryTarget := io.branchAddr
  lpht.io.entryPC := io.entryPC

  //gpht signals
  gpht.io.pc  := PC
  gpht.io.branchTaken := io.branchTaken
  gpht.io.branchTarget := io.branchAddr
  gpht.io.update := io.updatePrediction
  gpht.io.shiftHistory := io.shiftHistory
  gpht.io.resetHistory := false.B

//hybrid

  hybrid.io.pc := PC
  hybrid.io.branchTaken := io.branchTaken
  hybrid.io.branchTarget := io.branchAddr
  hybrid.io.update := io.updatePrediction
  hybrid.io.shiftHistory := io.shiftHistory
  hybrid.io.resetHistory := false.B
  hybrid.io.mispredicted := io.branchMispredicted
  hybrid.io.actualTarget := Mux(io.branchTaken, io.branchAddr, io.PCplus4ExStage)


  //default values before predictionMode is set
  io.predictorPrediction     := false.B
  io.predictorHit            := false.B
  io.predictorpredictedTarget := 0.U


  switch(io.predictionMode) {

    is(0.U) { // No predictor
     io.predictorPrediction               := false.B
      lpht.io.update                      := false.B
      gpht.io.update                      := false.B
      hybrid.io.update                    := false.B
      BTB.io.updatePrediction             := false.B
      io.predictorpredictedTarget         := PC + 4.U
      io.predictorHit                     := false.B
    }
    is(1.U) { //lpht
      io.predictorPrediction              := lpht.io.prediction
      io.predictorpredictedTarget         := lpht.io.lphtpredictedTarget
      io.predictorHit                     := lpht.io.lphtHit

      gpht.io.update                      := false.B
      hybrid.io.update                    := false.B
      BTB.io.updatePrediction             := false.B
    }

  is(2.U) { //gpht
      io.predictorPrediction              := gpht.io.gphtPrediction
      io.predictorpredictedTarget         := gpht.io.gphtpredictedTarget
      io.predictorHit                     := gpht.io.gphtHit

      lpht.io.update                      := false.B
      hybrid.io.update                    := false.B
      BTB.io.updatePrediction             := false.B
    }

    is(3.U) { //hybrid

      io.predictorPrediction              := hybrid.io.hybridPrediction
      io.predictorpredictedTarget         := hybrid.io.hybridpredictedTarget
      io.predictorHit                     := hybrid.io.hybridHit

      BTB.io.updatePrediction             := false.B
   }
  }



  // Stall PC
  when(io.stall){
    PC := PC
    //Fetch prev instruction -- Stalling the part of IF Barrier that holds the instruction
    InstructionMemory.io.instructionAddress := io.IFBarrierPC

  }.otherwise{
    //Fetch instruction
    InstructionMemory.io.instructionAddress := PC
    // PC register gets nextPC
    PC := nextPC
  }

  val predictedTaken = io.predictorHit && io.predictorPrediction
  val predictedTargetValid = io.predictorpredictedTarget =/= 0.U
  val usePrediction = predictedTaken && predictedTargetValid



  //Mux for controlling which address to go to next
  when(io.branchMispredicted){  // Case of branch mispredicted, we realize that in EX stage
    when(io.branchTaken){  // Branch Behavior is Taken, but Predicted Not-Taken
      nextPC := io.branchAddr
    }
    .otherwise{
      nextPC := io.PCplus4ExStage
    }
  }
  .elsewhen(usePrediction){
    nextPC := io.predictorpredictedTarget
  }
  .otherwise{ // Normal instruction OR assume not taken (BTB miss)
    nextPC := PCplus4
  }
  
  // Send PC to the res.entryPC <= VOIDt of the pipeline
  io.PC := PC

  io.instruction := instruction

  when(testHarness.InstructionMemorySetup.setup) {
    PC := 0.U
    instruction := Inst.NOP
    gpht.io.resetHistory := true.B
    hybrid.io.resetHistory := true.B
  }.otherwise {
    gpht.io.resetHistory := false.B
    hybrid.io.resetHistory := false.B
  }
}
