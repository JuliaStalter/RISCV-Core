package prediction

import chisel3._
import chisel3.util._

class gpht(val historyLength: Int = 3, val tableSize: Int = 1024) extends Module {
  val io = IO(new Bundle {


    val pc            = Input(UInt(32.W))
    val branchTaken   = Input(Bool())
    val update        = Input(Bool())
    val branchTarget  = Input(UInt(32.W))
    val shiftHistory  = Input(Bool())
    val resetHistory  = Input(Bool())


    val gphtPrediction             = Output(Bool())
    val gphtNextPC                 = Output(UInt(32.W))
    val gphtcorrectPrediction      = Output(Bool())
    val gphtHistoryOut             = Output(UInt(historyLength.W))
    val gphtHit                    = Output(Bool())
    val gphtloop                   = Output(Bool())
    val gphtpredictedTarget        = Output(UInt(32.W))
  })


  val globalHistory   = RegInit(0.U(historyLength.W))
  val predictionTable = RegInit(VecInit(Seq.fill(tableSize)(0.U(32.W))))

  val readindex           = Cat(io.pc(9, 2), globalHistory)(log2Ceil(tableSize)-1, 0)
  val writeindex          = Cat(io.pc(9, 2), globalHistory)(log2Ceil(tableSize)-1, 0)
  val doWrite             = RegNext(io.update, false.B)
  val writeData           = RegNext(io.branchTarget)


  val predictedTarget = predictionTable(readindex)








  when(doWrite) {

    predictionTable(writeindex) := writeData

  }


  io.gphtNextPC            :=  predictedTarget
  io.gphtpredictedTarget   :=  predictedTarget
  io.gphtPrediction        :=  predictedTarget =/= 0.U
  io.gphtHit               :=  io.gphtPrediction
  io.gphtHistoryOut        :=  globalHistory

  val loop = globalHistory === "b111".U
  io.gphtloop := loop

  val wasPredictionCorrect = (predictedTarget === io.branchTarget) && io.gphtPrediction
  io.gphtcorrectPrediction := wasPredictionCorrect


  when(io.resetHistory) {

    globalHistory := 0.U

  } .elsewhen(io.shiftHistory) {

    globalHistory := Cat(globalHistory(historyLength - 2, 0), io.branchTaken)

  }


}