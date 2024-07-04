/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package Stage_EX

import chisel3._
import chisel3.util._
import ALU.ALU
import MDU.MDU
import Branch_OP.Branch_OP
import config.branch_types._
import config.op1sel._
import config.op2sel._
import config.{ControlSignals, Instruction, branch_types, op1sel, op2sel}

class EX extends Module {

  val io = IO(
    new Bundle {
      val instruction = Input(new Instruction)
      val controlSignals = Input(new ControlSignals)
      val PC = Input(UInt(32.W))
      val branchType = Input(UInt())
      val op1Select = Input(UInt())
      val op2Select = Input(UInt())
      val rs1 = Input(UInt())
      val rs2 = Input(UInt())
      val immData = Input(UInt())
      val ALUop = Input(UInt())
      val rs1Select = Input(UInt(2.W)) // Used to select input to ALU in case of forwarding
      val rs2Select = Input(UInt(2.W))
      val ALUresultEXB = Input(UInt(32.W))
      val ALUresultMEMB = Input(UInt(32.W))
      val btbHit = Input(Bool())
      val btbTargetPredict = Input(UInt(32.W))
      val newBranch = Output(Bool())
      val updatePrediction = Output(Bool())
      val outPCplus4 = Output(UInt(32.W))
      val ALUResult = Output(UInt(32.W))
      val branchTarget = Output(UInt(32.W))
      val branchTaken = Output(Bool())
      val wrongAddrPred = Output(Bool())
      val Rs2Forwarded = Output(UInt(32.W))
      val exBranchTaken = Output(Bool())
      val exBranchAddr = Output(UInt(32.W))
      val exUpdatePrediction = Output(Bool())
    }
  )

  io.newBranch := false.B
  io.updatePrediction := false.B
  io.outPCplus4 := 0.U
  io.ALUResult := 0.U
  io.branchTarget := 0.U
  io.branchTaken := false.B
  io.wrongAddrPred := false.B
  io.Rs2Forwarded := 0.U
  io.exBranchTaken := false.B
  io.exBranchAddr := 0.U
  io.exUpdatePrediction := false.B

  val ALU = Module(new ALU).io
  val MDU = Module(new MDU).io
  val ResolveBranch = Module(new Branch_OP).io

  val mdu_op_flag = WireInit(false.B)
  val mdu_exception_flag = WireInit(false.B)

  val alu_operand1 = Wire(UInt())
  val alu_operand2 = Wire(UInt())
  val PCplus4 = Wire(UInt(32.W))
  // Control signals to ALU and Branch
  ResolveBranch.branchType := io.branchType
  ALU.ALUop := io.ALUop

  // Choosing ALU and Branch_Op Inputs  -- 2 consecutive MUXes
  // Forwarded operands -- 1st MUX
  when(io.rs1Select === 1.U) {
    alu_operand1 := io.ALUresultEXB
    ResolveBranch.src1 := io.ALUresultEXB
  }
    .elsewhen(io.rs1Select === 2.U) {
      alu_operand1 := io.ALUresultMEMB
      ResolveBranch.src1 := io.ALUresultMEMB
    }
    .otherwise {
      alu_operand1 := io.rs1
      ResolveBranch.src1 := io.rs1
    }
  when(io.rs2Select === 1.U) {
    alu_operand2 := io.ALUresultEXB
    ResolveBranch.src2 := io.ALUresultEXB
  }
    .elsewhen(io.rs2Select === 2.U) {
      alu_operand2 := io.ALUresultMEMB
      ResolveBranch.src2 := io.ALUresultMEMB
    }
    .otherwise {
      alu_operand2 := io.rs2
      ResolveBranch.src2 := io.rs2
    }
  // Operand 1, 2nd Mux
  //when(io.op1Select === op1sel.PC) {
   // ALU.src1 := io.PC
 // }.otherwise {
  //  ALU.src1 := alu_operand1
 //}
  // Operand 2, 2nd Mux
 // when(io.op2Select === op2sel.rs2) {
   // ALU.src2 := alu_operand2
 // }.otherwise {
  //  ALU.src2 := io.immData
 // }

// Operand 1, 2nd Mux
ALU.src1 := Mux(io.op1Select === op1sel.PC, io.PC, alu_operand1)

// Operand 2, 2nd Mux
ALU.src2 := Mux(io.op2Select === op2sel.rs2, alu_operand2, io.immData)

  // MDU
  MDU.src1 := alu_operand1
  MDU.src2 := alu_operand2
  MDU.MDUop := io.ALUop

  PCplus4 := io.PC + 4.U
  io.outPCplus4 := PCplus4


  io.ALUResult := ALU.aluRes // Assuming the ALU result is aluRes
  io.branchTaken := ResolveBranch.branchTaken // Assuming the branch taken signal is branchTaken
  io.branchTarget := ResolveBranch.branchTarget // Assuming the branch target is branchTarget
  io.wrongAddrPred := io.btbHit && (io.btbTargetPredict =/= ResolveBranch.branchTarget)
  io.newBranch := io.branchTaken
  io.updatePrediction := io.branchTaken
  io.Rs2Forwarded := alu_operand2
  io.exBranchTaken := io.branchTaken
  io.exBranchAddr := ResolveBranch.branchTarget
  io.exUpdatePrediction := io.updatePrediction
}