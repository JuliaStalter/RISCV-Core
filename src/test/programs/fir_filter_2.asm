# Base addresses
0       ADDI x9,  x0, 0x100     # input base
4       ADDI x10, x0, 0x140     # coeff base
8       ADDI x11, x0, 0x180     # output base

12      ADDI x3, x0, 0          # i = 0

loop_i:
16      ADDI x8, x0, 0          # sum = 0
20      ADDI x4, x0, 0          # j = 0

loop_j:
24      SUB  x5, x3, x4         # x5 = i - j
28      BLT  x5, x0, skip       # if (i-j) < 0 → skip  BLT x5, x0, 44

32      SLLI x6, x5, 2          # x6 = (i-j)*4
36      ADD  x6, x6, x9         # x6 = input + offset
40      LW   x6, 0(x6)          # x6 = input[i-j]

44      SLLI x7, x4, 2          # x7 = j*4
60      ADD  x7, x7, x10        # x7 = coeff + offset
64      LW   x7, 0(x7)          # x7 = coeff[j]

66      MUL  x7, x6, x7         # x7 = input[i-j] * coeff[j]
68      ADD  x8, x8, x7         # sum += result

skip:
72      ADDI x4, x4, 1          # j++
76      ADDI x6, x0, 4          # K = 4
80      BNE  x4, x6, loop_j     # if j != 4 → loop_j  BNE x4, x6, -56

84      SLLI x7, x3, 2          # x7 = i*4
88      ADD  x7, x7, x11        # x7 = output + offset
92      SW   x8, 0(x7)          # output[i] = sum

96      ADDI x3, x3, 1          # i++
100     ADDI x6, x0, 16         # N = 16
104     BNE  x3, x6, loop_i     # if i != 16 → loop_i BNE x3, x6, -88

# Mark success (exit)
108     ADDI x10, x0, 1         # success flag