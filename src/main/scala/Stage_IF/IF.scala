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

    val branchAddr           = Input(UInt())
    val IFBarrierPC          = Input(UInt())
    val stall                = Input(Bool())

    // Inputs for BTB, will come from EX stage and Hazard Unit
    val updatePrediction      = Input(Bool())
    val newBranch             = Input(Bool())
    val entryPC               = Input(UInt(32.W))
    val branchTaken           = Input(Bool()) // 1 means Taken -- 0 means Not Taken
    val PCplus4ExStage        = Input(UInt(32.W))
    val exBranchTaken         = Input(Bool())
    val exBranchAddr          = Input(UInt(32.W))
    val exUpdatePrediction    = Input(Bool())
    val predictorMode         = Input(UInt(2.W)) // if use lpht, gpht, hybrid or none for testing
    val branchMispredicted    = Input(Bool())
    val actualTarget          = Input(UInt(32.W))

    val btbHit               = Output(Bool())
    val btbPrediction        = Output(Bool())
    val btbTargetPredict     = Output(UInt(32.W))
    val PC                   = Output(UInt())
    val instruction          = Output(new Instruction)
    val cycleCounter         = Output(UInt(32.W))
    val correctPrediction    = Output(Bool())
    val predictedTaken       = Output(Bool())
    val nextPC               = Output(UInt(32.W))

  })

  val InstructionMemory = Module(new InstructionMemory(BinaryFile))
  val BTB = Module(new BTB_direct)

  val lpht    = Module(new BranchPredictor)
  val gpht    = Module(new global_branch_predictor(historyLength = 3, tableSize = 1024))
  val hybrid  = Module(new hybrid_predictor)


  val nextPC        = WireInit(UInt(32.W), 0.U)
  val PCplus4       = Wire(UInt(32.W))
  val PC            = RegInit(UInt(32.W), 0.U)
  val instruction   = Wire(new Instruction)
  val branch        = WireInit(Bool(), false.B)
  val defaultNextPC = PC + 4.U

  InstructionMemory.testHarness.setupSignals := testHarness.InstructionMemorySetup
  testHarness.PC := InstructionMemory.testHarness.requestedAddress

  io.PC          := PC
  io.instruction := instruction
  instruction    := InstructionMemory.io.instruction.asTypeOf(new Instruction)

  PCplus4 := PC + 4.U

  BTB.io.currentPC          := PC
  BTB.io.newBranch          := io.newBranch
  BTB.io.updatePrediction   := io.updatePrediction
  BTB.io.entryPC            := io.entryPC
  BTB.io.entryBrTarget      := io.branchAddr
  BTB.io.branchMispredicted := io.branchMispredicted
  BTB.io.stall              := io.stall
  io.btbPrediction          := BTB.io.prediction
  io.btbHit                 := BTB.io.btbHit
  io.btbTargetPredict       := BTB.io.targetAdr

  // Branch Predictor signals
  lpht.io.pc                := PC
  lpht.io.branchTaken       := io.branchTaken
  lpht.io.branchTarget      := io.branchAddr
  lpht.io.update            := io.updatePrediction
  lpht.io.preloadEnable     := false.B
  lpht.io.preloadHistory    := VecInit(Seq(false.B, false.B))

  // GPHT signals
  gpht.io.currentPC         := PC
  gpht.io.branchTaken       := io.branchTaken
  gpht.io.branchTarget      := io.branchAddr
  gpht.io.update            := io.updatePrediction
  gpht.io.shiftHistory      := !io.stall
  gpht.io.resetHistory      := testHarness.InstructionMemorySetup.setup

  // hybrid signals
  hybrid.io.pc              := PC
  hybrid.io.branchTaken     := io.branchTaken
  hybrid.io.branchTarget    := io.branchAddr
  hybrid.io.update          := io.updatePrediction
  hybrid.io.shiftHistory    := !io.stall
  hybrid.io.resetHistory    := testHarness.InstructionMemorySetup.setup
  hybrid.io.mispredicted    := io.branchMispredicted
  hybrid.io.actualTarget    := io.actualTarget

  val cycleCounter        = RegInit(0.U(32.W))
  val predictedTarget     = WireDefault(PC + 4.U)
  val predictionTaken     = WireDefault(false.B)
  val updatePrediction    = WireDefault(false.B)

  switch(io.predictorMode) {

    is(0.U) { // No predictor
      predictionTaken         := false.B
      lpht.io.update          := false.B
      gpht.io.update          := false.B
      hybrid.io.update        := false.B
      BTB.io.updatePrediction := false.B
      predictedTarget         := PC + 4.U

    }
    is(1.U) { //lpht
      predictionTaken         := lpht.io.predictTaken
      predictedTarget         := lpht.io.nextPC
      gpht.io.update          := false.B
      hybrid.io.update        := false.B
    }

    is(2.U) { //gpht
      predictionTaken         := gpht.io.validPrediction
      predictedTarget         := gpht.io.predictedNextPC
      lpht.io.update          := false.B
      hybrid.io.update        := false.B
    }

    is(3.U) { //hybrid

      predictionTaken         := hybrid.io.predictTaken
      predictedTarget         := hybrid.io.predictedTarget
    }
  }

  when(io.branchMispredicted) {
    nextPC := Mux(io.exBranchTaken, io.exBranchAddr, io.PCplus4ExStage)
  }.elsewhen(predictionTaken) {
    nextPC := predictedTarget
  }.otherwise {
    nextPC := PCplus4
  }

  val fetchAddress = Wire(UInt(32.W))
  fetchAddress := Mux(io.stall, io.IFBarrierPC, nextPC)
    InstructionMemory.io.instructionAddress := fetchAddress

  val resetDelayed = RegNext(reset.asBool(), true.B)

  when(!io.stall && !resetDelayed) {
    PC := nextPC
    cycleCounter := cycleCounter + 1.U
  }

  io.cycleCounter := cycleCounter

  val delayedBranchTaken = RegNext(io.branchTaken)
  val delayedBranchAddr  = RegNext(io.branchAddr)

  io.correctPrediction := (predictedTarget === delayedBranchAddr &&
    predictionTaken === delayedBranchTaken)
  io.predictedTaken := predictionTaken
  io.nextPC := nextPC
}