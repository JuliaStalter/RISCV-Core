/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package Piplined_RISC_V

import chisel3._
import chisel3.util._
import config.{Instruction, Inst}
class IFpipe extends Module
{
  val io = IO(
    new Bundle {
      val inCurrentPC         = Input(UInt(32.W))
      val inInstruction       = Input(new Instruction)
      val stall               = Input(Bool())
      val flush               = Input(Bool())
      val inpredictorHit            = Input(Bool())
      val inpredictorPrediction     = Input(Bool())
      val inpredictorpredictedTarget  = Input(UInt(32.W))
      val outpredictorHit           = Output(Bool())
      val outpredictorPrediction    = Output(Bool())
      val outpredictorpredictedTarget = Output(UInt(32.W))
      val outCurrentPC        = Output(UInt(32.W))
      val outInstruction      = Output(new Instruction)
    }
  )

  val currentPCReg   = RegEnable(io.inCurrentPC, 0.U, !io.stall)  
  val flushDelayed = RegInit(Bool(), 0.U)

  flushDelayed := io.flush // Note: Delay flush signal because io.outInstruction is combinational (because Read iMem is synchronous)

  // Propagate BTB signals
  val predictorHitReg        = RegInit(false.B)
  val predictorPredictionReg = RegInit(false.B)
  val predictorpredictedTarget = RegInit(0.U(32.W))

  predictorHitReg        := io.inpredictorHit
  predictorPredictionReg := io.inpredictorPrediction
  predictorpredictedTarget := io.inpredictorpredictedTarget

  io.outpredictorHit          := predictorHitReg
  io.outpredictorPrediction   := predictorPredictionReg
  io.outpredictorpredictedTarget := predictorpredictedTarget

  // Flush, Stall, or Propagate Instruction
  when(flushDelayed === 1.U){
    io.outInstruction := Inst.NOP
  }
  .otherwise{
    io.outInstruction := io.inInstruction
  }

  // Propagate PC
  io.outCurrentPC := currentPCReg   

}
