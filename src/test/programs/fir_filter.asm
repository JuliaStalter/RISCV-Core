0           addi t0, t0, 16
4           add t1, a0, t0
8            lw   t2, 0(t1)
12           slli t3, t2, 2
16          add  t4, a0, t3
20           sw   a1, 0(t4)
24           addi t5, x0, 0
28           addi t6, x0, 0
32           addi s1, x0, 0

//loopstart
36           addi t0, x0, 4
40           bge  s1, t0, 80
44          slli t3, s1, 2
48          add  t4, a2, t3
52           lw   s0, 0(t4)
56          slli t3, t2, 2
60              add  t4, a0, t3
64              lw   s1, 0(t4)
68                  mul  t3, s0, s1
72                mulh t4, s0, s1
76              add  t5, t5, t3
80              sltu t0, t5, t3
84              add  t6, t6, t4
88              add  t6, t6, t0
92               addi t2, t2, -1
96               blt  t2, x0, 12
100              addi s1, s1, 1
104                 jal x0, -68

//resetj
108                 addi t2, x0,3
112              addi s1, s1, 1
116                 jal x0,   -80

//loopend
120             addi t2, t2, 1
124             addi t0, x0, 4
128              bne  t2, t0, 8
132             addi t2, x0, 0

//storej
136             addi t0, x0, 16
140             add  t1, a0, t0
144                 sw   t2, 0(t1)
148             addi t0, x0, 3
152             srl  a3, t5, t0
156              addi t1, x0, 32
160             sub  t1, t1, t0
164             sll  t2, t6, t1
168               or   a3, a3, t2
172
176             ecall
180
184
