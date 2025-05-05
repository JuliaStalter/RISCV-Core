package benchmark

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import RISCV_TOP.RISCV_TOP
import LPHT.BranchPredictor
import GPHT.global_branch_predictor
import hybrid.hybrid_predictor
import java.io._

class PredictorBenchmarkTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Single Predictor Mode Cycle Test"
  it should "run RISCV_TOP for each predictor mode and collect stats" in {
    test(new RISCV_TOP()) { dut =>
      dut.clock.setTimeout(5000)
      val writer = new PrintWriter(new File("predictor_stats.csv"))
      writer.println("mode,total,correct,miss,accuracy(%),cycles")

      def runTest(modeName: String, modeValue: Int): Unit = {
        println(s"\n--- Testing predictor mode: $modeName ($modeValue) ---")

        dut.reset.poke(true.B)
        dut.io.resetStats.poke(true.B)
        dut.io.predictorMode.poke(modeValue.U)
        dut.clock.step(5)
        dut.reset.poke(false.B)
        dut.io.resetStats.poke(false.B)

        var total = 0
        var correct = 0
        var miss = 0
        var cycleCount = 0

        while (!dut.io.ecall.peek().litToBoolean && cycleCount < 1000) {
          val pc = dut.io.PC.peek().litValue
          val isCorrect = dut.io.correctPrediction.peek().litToBoolean

          total += 1
          if (isCorrect) {
            correct += 1
            dut.clock.step(1)
            cycleCount += 1
          } else {
            miss  += 1
            dut.clock.step(4)
            cycleCount += 4
          }

          println(f"Cycle $cycleCount%3d | PC=0x$pc%08x | correctPrediction=$isCorrect")
          dut.clock.step()
        }

        val accuracy = if (total > 0) correct.toDouble / total * 100 else 0.0
        writer.println(f"$modeName,$total,$correct,$miss,${accuracy}%.2f,$cycleCount")
      }

      runTest("none", 0)
      runTest("local", 1)
      runTest("global", 2)
      runTest("hybrid", 3)

      writer.close()
    }
  }

  def runStandaloneTest(name: String, predictor: => Module, hasPredictionIO: Boolean): Unit = {
    it should s"run standalone $name predictor with pattern+loop+exception test" in {
      test(predictor).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        println(s"\n--- Manual test: $name ---")

        val writer = new PrintWriter(new File(s"${name}_predictor_stats.csv"))
        writer.println("cycle,pc,taken,target,predicted,valid,correct")
        writer.println("cycle,phase,pc,taken,target,predicted,valid,correct")

        dut.reset.poke(true.B)
        dut.clock.step(2)
        dut.reset.poke(false.B)

        val loopStart = 0x100.U
        val loopEnd = 0x120.U
        val loopTarget = loopStart

        val branches = Seq(
   /*       (0x10.U, true.B,  0x20.U),   // Loop-like
          (0x14.U, false.B, 0x18.U),  // Fall-through
          (0x18.U, true.B,  0x30.U),   // Forward jump
          (0x1C.U, true.B,  0x28.U),   // Medium loop
          (0x20.U, false.B, 0x24.U),  // Fall-through
          (0x24.U, true.B,  0x40.U),   // Strong loop

          // New patterns
          (0x28.U, true.B, 0x50.U),    // Distant jump
          (0x2C.U, false.B, 0x30.U),   // Missed branch
          (0x30.U, true.B, 0x10.U),    // Loop to front
          (0x34.U, false.B, 0x38.U),   // No-branch
          (0x38.U, true.B, 0x2C.U) , // Backward jump (non-loop)
*/
  /*        (0x10.U, true.B,  0x20.U),  // loop start
          (0x14.U, false.B, 0x18.U), // not taken
          (0x18.U, true.B,  0x30.U), // chain pattern
          (0x1C.U, true.B,  0x28.U), // loop end
          (0x20.U, false.B, 0x24.U), // fallthrough
          (0x24.U, true.B,  0x10.U), // loop back
          (0x28.U, true.B,  0x3C.U), // non-loop, pattern target
          (0x2C.U, false.B, 0x30.U), // noise
          (0x30.U, true.B,  0x44.U), // global branch pattern
          (0x34.U, true.B,  0x10.U), // fake loop back
          (0x38.U, false.B, 0x3C.U), // fallthrough
          (0x3C.U, true.B,  0x50.U), // deep chain
          (0x40.U, true.B,  0x60.U) , // unrelated jump
*/
          (0x10.U, true.B,  0x20.U),  // start of a loop
          (0x14.U, true.B,  0x30.U),  // pattern element A
          (0x18.U, false.B, 0x1C.U),  // noise
          (0x1C.U, true.B,  0x28.U),  // pattern element B
          (0x20.U, true.B,  0x10.U),  // loop back
          (0x28.U, true.B,  0x50.U),  // pattern element C
          (0x2C.U, false.B, 0x30.U),  // noise
          (0x30.U, true.B,  0x44.U),  // global-sensitive branch
          (0x34.U, true.B,  0x10.U)   // fake loop back


        )

        var correct = 0
        var total = 0
        var cycle = 0


        // Phase 1: Loop training
        for (_ <- 0 until 8) {
          val phase = "loop"
          dut match {

            case local: BranchPredictor =>
              local.io.pc.poke(loopEnd)
              local.io.branchTaken.poke(true.B)
              local.io.branchTarget.poke(loopTarget)
              local.io.update.poke(true.B)
              local.io.preloadEnable.poke(false.B)
              local.io.preloadHistory.poke("b11".U)
              val predicted = local.io.nextPC.peek().litValue
              val correctPrediction = predicted == loopTarget.litValue
              val valid = true
              if (correctPrediction) {
                correct += 1
                cycle += 1
                dut.clock.step(1)
              } else {
                cycle += 4
                dut.clock.step(4)
              }
              total += 1
              writer.println(s"$cycle,$phase,0x${loopEnd.litValue.toString(16)},true,0x${loopTarget.litValue.toString(16)},0x${predicted.toString(16)},$valid,$correctPrediction")

            case global: global_branch_predictor =>
              global.io.currentPC.poke(loopEnd)
              global.io.branchTaken.poke(true.B)
              global.io.branchTarget.poke(loopTarget)
              global.io.update.poke(true.B)
              global.io.shiftHistory.poke(true.B)
              global.io.resetHistory.poke(false.B)
              val predicted = global.io.predictedNextPC.peek().litValue
              val valid = global.io.validPrediction.peek().litToBoolean
              val correctPrediction = valid && predicted == loopTarget.litValue
              if (correctPrediction) {
                correct += 1
                cycle += 1
                dut.clock.step(1)
              } else {
                cycle += 4
                dut.clock.step(4)
              }
              total += 1
              writer.println(s"$cycle,$phase,0x${loopEnd.litValue.toString(16)},true,0x${loopTarget.litValue.toString(16)},0x${predicted.toString(16)},$valid,$correctPrediction")

            case hybrid: hybrid_predictor =>
              hybrid.io.pc.poke(loopEnd)
              hybrid.io.branchTaken.poke(true.B)
              hybrid.io.branchTarget.poke(loopTarget)
              hybrid.io.update.poke(true.B)
              hybrid.io.shiftHistory.poke(true.B)
              hybrid.io.resetHistory.poke(false.B)
              val predicted = hybrid.io.predictedTarget.peek().litValue
              val valid = true
              val correctPrediction = predicted == loopTarget.litValue
              if (correctPrediction) {
                correct += 1
                cycle += 1
                dut.clock.step(1)
              } else {
                cycle += 4
                dut.clock.step(4)
              }
              total += 1
              writer.println(s"$cycle,$phase,0x${loopEnd.litValue.toString(16)},true,0x${loopTarget.litValue.toString(16)},0x${predicted.toString(16)},$valid,$correctPrediction")

          }
          dut.clock.step()
        }

        // Phase 2: Realistic pattern
        for (_ <- 0 until 12) {

          for ((pc, taken, target) <- branches) {
            val phase = "pattern"
            dut match {

              case local: BranchPredictor =>
                local.io.pc.poke(pc)
                local.io.branchTaken.poke(taken)
                local.io.branchTarget.poke(target)
                local.io.update.poke(true.B)
                local.io.preloadEnable.poke(false.B)
                local.io.preloadHistory.poke("b11".U)
                val predicted = local.io.nextPC.peek().litValue
                val correctPrediction = predicted == target.litValue
                val valid = true
                if (correctPrediction) {
                  correct += 1
                  cycle += 1
                  dut.clock.step(1)
                } else {
                  cycle += 4
                  dut.clock.step(4)
                }
                total += 1
                writer.println(s"$cycle,$phase,0x${pc.litValue.toString(16)},${taken.litToBoolean},0x${target.litValue.toString(16)},0x${predicted.toString(16)},$valid,$correctPrediction")

              case global: global_branch_predictor =>
                global.io.currentPC.poke(pc)
                global.io.branchTaken.poke(taken)
                global.io.branchTarget.poke(target)
                global.io.update.poke(true.B)
                global.io.shiftHistory.poke(true.B)
                global.io.resetHistory.poke(false.B)
                val predicted = global.io.predictedNextPC.peek().litValue
                val valid = global.io.validPrediction.peek().litToBoolean
                val correctPrediction = valid && predicted == target.litValue
                if (correctPrediction) {
                  correct += 1
                  cycle += 1
                  dut.clock.step(1)
                } else {
                  cycle += 4
                  dut.clock.step(4)
                }
                total += 1
                writer.println(s"$cycle,$phase,0x${pc.litValue.toString(16)},${taken.litToBoolean},0x${target.litValue.toString(16)},0x${predicted.toString(16)},$valid,$correctPrediction")

              case hybrid: hybrid_predictor =>
                hybrid.io.pc.poke(pc)
                hybrid.io.branchTaken.poke(taken)
                hybrid.io.branchTarget.poke(target)
                hybrid.io.update.poke(true.B)
                hybrid.io.shiftHistory.poke(true.B)
                hybrid.io.resetHistory.poke(false.B)
                val predicted = hybrid.io.predictedTarget.peek().litValue
                val correctPrediction = predicted == target.litValue
                val valid = true
                if (correctPrediction) {
                  correct += 1
                  cycle += 1
                  dut.clock.step(1)
                } else {
                  cycle += 4
                  dut.clock.step(4)
                }
                total += 1
                writer.println(s"$cycle,$phase,0x${pc.litValue.toString(16)},${taken.litToBoolean},0x${target.litValue.toString(16)},0x${predicted.toString(16)},$valid,$correctPrediction")
            }
            dut.clock.step()
          }
        }
        val accuracy = if (total > 0) 100.0 * correct / total else 0.0
        println(f"$name accuracy: $correct / $total = $accuracy%.2f%%")
        writer.println(f"$name accuracy: $correct / $total = $accuracy%.2f%%")
        writer.println()
        writer.println(f"SUMMARY,total=$total,correct=$correct,accuracy=${accuracy}%.2f,total_cycles=$cycle")
        writer.close()
      }
    }
  }

  runStandaloneTest("local", new BranchPredictor, hasPredictionIO = true)
  runStandaloneTest("global", new global_branch_predictor(3, 1024), hasPredictionIO = true)
  runStandaloneTest("hybrid", new hybrid_predictor, hasPredictionIO = true)
}
