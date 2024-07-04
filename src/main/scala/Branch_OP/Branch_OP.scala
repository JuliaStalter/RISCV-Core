/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package Branch_OP
import config.branch_types._
import chisel3._
import chisel3.util._

class Branch_OP extends Module {
  val io = IO(
    new Bundle {
      val branchType  = Input(UInt(32.W))
      val src1        = Input(UInt(32.W))
      val src2        = Input(UInt(32.W))
      val branchTaken = Output(Bool())
      val branchTarget = Output(UInt(32.W))
    }
  )

  io.branchTaken := false.B
  io.branchTarget := 0.U

  //Branch lookup
  val lhs = io.src1.asSInt
  val rhs = io.src2.asSInt
  switch(io.branchType) {
    is(beq) {
      io.branchTaken := (lhs === rhs).asBool()
    }
    is(neq) {
      io.branchTaken := (lhs =/= rhs).asBool()
    }
    is(gte) {
      io.branchTaken := (lhs >= rhs).asBool()
    }
    is(lt) {
      io.branchTaken := (lhs < rhs).asBool()
    }
    is(gteu) {
      io.branchTaken := (lhs >= rhs).asBool()
    }
    is(ltu) {
      io.branchTaken := (lhs < rhs).asBool()
    }
    is(jump) {
      io.branchTaken := true.B
    }
    is(DC) {
      io.branchTaken := false.B
    }
  }
}
