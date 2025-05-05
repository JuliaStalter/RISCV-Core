package performance

import chisel3._

class PerformanceMonitor extends Module {
  val io = IO(new Bundle {
    val updateSignal = Input(Bool())   // Signals computation happening
    val misprediction = Input(Bool())  // Tracks mispredictions
    val cycles = Output(UInt(32.W))    // Total execution cycles
    val energyEstimate = Output(UInt(32.W)) // Approximate energy usage
    val computationLoad = Output(UInt(32.W)) // Count of computations
  })

  val cycles = RegInit(0.U(32.W))
  val energyEstimate = RegInit(0.U(32.W))
  val computationLoad = RegInit(0.U(32.W))


  cycles := cycles + 1.U


  when(io.updateSignal) {
    val logicSwitching = io.misprediction.asUInt
    val registerWrites = 1.U            
    energyEstimate := energyEstimate + (registerWrites + logicSwitching)
    computationLoad := computationLoad + 1.U
  }

  // Outputs
  io.cycles := cycles
  io.energyEstimate := energyEstimate
  io.computationLoad := computationLoad

  // Print results
  printf(p"Cycles: ${cycles}, Energy: ${energyEstimate}, Load: ${computationLoad}\n")
}
