0   addi    t0, x0, 10              //filter order
4   addi    t1, x0, 1000            //sampling freq
8   addi    t2, x0, 50              //cutof freq
12  addi    t3, x0, 100              //signal length
16  addi    t4, x0, 0                  //loop 1
20  addi    t5, x0, 0
24  bge     t4, t0, 24
28  sub     t6, t4, t0
32  srai    t6, t6, 1
26  beq     t4, t5, 4
40  addi    t7, x0, 2                       //special case thingi
44  mul     t8, t7, t2
48  addi    t4, x0, 0               // done calc
52  addi    t9, x0, 0x54             //hammingwindow
56  addi    t10, x0, 0x46
60  bge     t4, t0, 32             //apply hamming
64  lw      t6, 0(t0)                   //get h[n]
68  mul     t7, t6, t9                  // h[n]*0.54
72  add     t6, t7, t10                  // + 0.46
76  sw      t6, 0(t0)                   // speichert erg
80  addi    t4, t4, 1                   // i++
84  addi    t0, t0, 4                   // next coeff in h[]
88  beq     x0, x0, -28                    // apply haming
92  addi    t4, x0, 0                   // done wind
96  addi    t11, x0, 0
102 bge     t4, t3, 44             //apply filter
106 addi    t12, x0, 0                      // i 0
110 addi    t13, x0, 0
114 bge     t12, t0, 32                    //fir inner
118  lw      t6, 0(t0
122  lw      t7, 0(t12)
126 mul     t8, t6, t
130 add     t13, t13, t8
134  addi    t12, t12, 1
138 addi    t0, t0, 4
142  beq     x0, x0, -28
146  sw      t13, 0(t11)                // Ddone inner
150 addi    t4, t4, 1
154 addi    t11, t11, 4
158  beq     x0, x0, -56
162  la      t0, -56
166  mul     t12, t4, 4
170  add     t11, t11, t12
174 sw      t13, 0(t11)
178 li      a7, 10
182  ecall
