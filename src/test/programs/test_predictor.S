# test_predictor.S

.section .text
.globl _start

_start:
    addi t0, zero, 0      # i = 0
    addi t1, zero, 100    # outer loop limit
    addi t2, zero, 0      # sum = 0

outer_loop:
    beq t0, t1, done_outer  # if i == 100, exit

    addi t3, zero, 0     # j = 0
    addi t4, zero, 50    # inner loop limit

inner_loop:
    beq t3, t4, end_inner   # if j == 50, break inner

    # j % 7 == 0 check using subtraction loop
    addi t6, zero, 0     # remainder = j
    add t6, t3, zero     # t6 = t3
mod_loop:
    addi t6, t6, -7
    blt t6, zero, mod_check
    jal zero, mod_loop

mod_check:
    bne t6, zero, skip_branch

    # Some unpredictable path
    addi a0, zero, 1
    add t2, t2, a0       # sum += 1

skip_branch:
    addi t3, t3, 1
    jal zero, inner_loop

end_inner:
    addi t0, t0, 1
    jal zero, outer_loop

done_outer:
    ecall                # end