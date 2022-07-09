	.data
	.align 2		#align on a word boundary
_m:	.space 4
	.text
	.globl main
main:
# Push space for the locals
	subu  $sp, $sp, 4
	.data
STRING_2:	.asciiz "a: "
	.text
	la    $t0, STRING_2
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $a0, 4($sp)	#POP
	addu  $sp, $sp, 4
	li    $v0, 4
	syscall
	li    $v0, 5
	syscall
	la    $t0, -8($fp)	#Generate Address
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	#POP
	addu  $sp, $sp, 4
	sw    $v0, 0($t0)
	li    $t0, 2
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	la    $t0, _m
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	#POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	#POP
	addu  $sp, $sp, 4
	sw    $t0, 0($t1)
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	#POP
	addu  $sp, $sp, 4
	lw    $t0, -8($fp)
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t0, _m
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	#POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	#POP
	addu  $sp, $sp, 4
	sub   $t0, $t1, $t0
	bgez  $t0, .L2
	li    $t0, 1
	b     .L3
.L2:
	li    $t0, 0
.L3:
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	li    $t1, 1
	bne   $t0, $t1, ELSE_1
	.data
STRING_3:	.asciiz "a is greater"
	.text
	la    $t0, STRING_3
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $a0, 4($sp)	#POP
	addu  $sp, $sp, 4
	li    $v0, 4
	syscall
	j     ENDIF_1
ELSE_1:
	.data
STRING_4:	.asciiz "m is greater"
	.text
	la    $t0, STRING_4
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $a0, 4($sp)	#POP
	addu  $sp, $sp, 4
	li    $v0, 4
	syscall
ENDIF_1:
exit_main:
	li    $v0, 10
	syscall
