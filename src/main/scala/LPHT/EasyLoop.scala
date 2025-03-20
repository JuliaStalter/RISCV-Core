
package LPHT
import chisel3._


class EasyLoop extends Module{

  val io = IO(new Bundle {

    val start   = Input (Bool())
    val antwort = Output (UInt(8.W))

  })

  val counter = RegInit(0.U(8.W))
  val active = RegInit (false.B)
  val antwortflag = RegInit (0.U(8.W))



  when (io.start) {

    active := true.B
  }

  when(active){

    when (counter < 42.U) {
      counter := counter + 1.U
    }.otherwise {

      active := false.B
      antwortflag := counter
      counter := 0.U

    }
  }

  io.antwort := antwortflag

}