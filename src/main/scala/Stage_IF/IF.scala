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

import LPHT.BranchPredictor
import GPHT.global_branch_predictor
import hybrid.hybrid_predictor

class IF(BinaryFile: String) extends Module {

  val testHarness = IO(
    new Bundle {
      val InstructionMemorySetup = Input(new IMEMsetupSignals)
      val PC = Output(UInt())
    }
  )

  val io = IO(new Bundle {
    val branchAddr = Input(UInt())
    val IFBarrierPC = Input(UInt())
    val stall = Input(Bool())
    // Inputs for BTB, will come from EX stage and Hazard Unit
    val updatePrediction = Input(Bool())
    val newBranch = Input(Bool())
    val entryPC = Input(UInt(32.W))
    val branchTaken = Input(Bool()) // 1 means Taken -- 0 means Not Taken
    val branchMispredicted = Input(Bool())
    val PCplus4ExStage = Input(UInt(32.W))
    val btbHit = Output(Bool())
    val btbPrediction = Output(Bool())
    val btbTargetPredict = Output(UInt(32.W))
    val PC = Output(UInt())
    val instruction = Output(new Instruction)
    val exBranchTaken = Input(Bool())
    val exBranchAddr = Input(UInt(32.W))
    val exUpdatePrediction = Input(Bool())
    val predictorMode = Input(UInt(2.W)) // if use lpht, gpht, hybrid or none for testing
    val cycleCounter = Output(UInt(32.W))
    val correctPrediction = Output(Bool())

  })

  val InstructionMemory = Module(new InstructionMemory(BinaryFile))
  val BTB = Module(new BTB_direct)
  val BranchPredictor = Module(new BranchPredictor)

  val nextPC = WireInit(UInt(), 0.U)
  val PC = RegInit(UInt(32.W), 0.U)
  val PCplus4 = Wire(UInt(32.W))
  val instruction = Wire(new Instruction)
  val branch = WireInit(Bool(), false.B)
  val gpht = Module(new global_branch_predictor(historyLength = 3, tableSize = 1024))
  val defaultNextPC = PC + 4.U
  val hybrid = Module(new hybrid_predictor)
  InstructionMemory.testHarness.setupSignals := testHarness.InstructionMemorySetup
  testHarness.PC := InstructionMemory.testHarness.requestedAddress

  instruction := InstructionMemory.io.instruction.asTypeOf(new Instruction)


  PCplus4 := PC + 4.U


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

  // Branch Predictor signals
  BranchPredictor.io.pc := PC
  BranchPredictor.io.branchTaken := io.branchTaken
  BranchPredictor.io.branchTarget := io.branchAddr
  BranchPredictor.io.update := io.updatePrediction
  BranchPredictor.io.preloadEnable := false.B
  BranchPredictor.io.preloadHistory := 0.U

  // GPHT signals

  gpht.io.currentPC := PC
  gpht.io.branchTaken := io.branchTaken
  gpht.io.branchTarget := io.branchAddr
  gpht.io.update := io.updatePrediction
  gpht.io.shiftHistory := !io.stall
  gpht.io.resetHistory := testHarness.InstructionMemorySetup.setup


  // hybrid signals
  hybrid.io.pc := PC
  hybrid.io.branchTaken := io.branchTaken
  hybrid.io.branchTarget := io.branchAddr
  hybrid.io.update := io.updatePrediction
  hybrid.io.shiftHistory := !io.stall
  hybrid.io.resetHistory := testHarness.InstructionMemorySetup.setup


  when(io.stall) {
    PC := PC

    InstructionMemory.io.instructionAddress := io.IFBarrierPC
  }.otherwise {

    InstructionMemory.io.instructionAddress := PC

    PC := nextPC
  }
  val cycleCounter = RegInit(0.U(32.W))
  when(!io.stall) {
    cycleCounter := cycleCounter + 1.U
  }
  io.cycleCounter := cycleCounter

  when(io.branchMispredicted) {
    when(io.branchTaken) {
      nextPC := io.branchAddr
    }.otherwise {
      nextPC := io.PCplus4ExStage
    }
  }.elsewhen(io.predictorMode === 1.U && BranchPredictor.io.prediction) {
    nextPC := BranchPredictor.io.nextPC
  }.elsewhen(io.predictorMode === 2.U && gpht.io.validPrediction) {
    nextPC := gpht.io.predictedNextPC
  }.elsewhen(io.predictorMode === 3.U && hybrid.io.prediction) {
    nextPC := hybrid.io.predictedTarget
  }.otherwise {
    nextPC := PCplus4
  }

  // Send PC to the rest of the pipeline
  io.PC := PC

  io.instruction := instruction

  when(testHarness.InstructionMemorySetup.setup) {
    PC := 0.U
    instruction := Inst.NOP
  }

  val predictionTaken = WireDefault(false.B)
  val predictedTarget = WireDefault(defaultNextPC)


  switch(io.predictorMode) {
    is(1.U) { // Local predictor
      predictionTaken := BranchPredictor.io.prediction
      predictedTarget := BranchPredictor.io.nextPC
      io.btbHit := BranchPredictor.io.prediction
    }
    is(2.U) { // Global predictor
      predictionTaken := gpht.io.validPrediction
      predictedTarget := gpht.io.predictedNextPC
      io.btbHit := gpht.io.validPrediction
    }
    is(3.U) { // Hybrid
      predictionTaken := hybrid.io.prediction
      predictedTarget := hybrid.io.predictedTarget
      io.btbHit := hybrid.io.prediction

      BranchPredictor.io.preloadEnable := hybrid.io.preloadLocal
      BranchPredictor.io.preloadHistory := hybrid.io.preloadHistory

    }
  }

  io.correctPrediction := (predictedTarget === io.branchAddr && predictionTaken === io.branchTaken && io.updatePrediction)
}