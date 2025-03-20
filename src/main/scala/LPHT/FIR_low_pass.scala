/*package LPHT

class FIR_low_pass(coeffs: Seq[Double]) extends Module{

  val io = IO(new Bundle {

  val input = Input(SInt(16.W))
  val output = Output(SInt(16.W))

  })


  val taps = coeffs.length
  val regs = RegInit(VecInit(Seq.fill(taps)(0.S(16.W))))
  val coeffsFixed = coeffs.map(c => (c * 32768).toInt.S(6.W))


  regs := io.in +: regs.init

  val sum = RegInit(0.S(16.W))
  sum := 0.S

  for(i <- 0 until taps){
    for(j <- i to 0 by -1) {
      sum := sm + (regs(j) * coeffsFixed(j))
    }
  }

io.out := sum


}
// input signal : x b (abh von sinc funktion )
// input 1 + input 2 usw soviel wie i :

// output[i]

// for int i = 0; i < input.len; i++ {
// for int j = i; j >0; j-- {
// output[i] += input[j]*b[j]
//  }
// b++ ? woher sinc func in signal ?
//}



 */