# CompilerForC--
A Java Compiler For New Language C-- (Simplified C Language)

语法在/lexer/Yylex中定义

编译的文件通过一个ASTnode表示，类结构如下：

![image](https://github.com/Roway12/CompilerForC--/blob/main/images/AST.png)

最终对于一个如下文件：

```
int m;
void main(){
	int a;
	cin>>a;
	m = 4;
	m = m+a;
	cout<<m;
}
```
生成对应的MIPS指令集：
```
	.data
	.align 2		#align on a word boundary
_m:	.space 4
	.text
	.globl main
main:
# Push space for the locals
	subu  $sp, $sp, 4
	li    $v0, 5
	syscall
	la    $t0, -8($fp)	#Generate Address
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t0, 4($sp)	#POP
	addu  $sp, $sp, 4
	sw    $v0, 0($t0)
	li    $t0, 4
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
	lw    $t0, _m
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t0, -8($fp)
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $t1, 4($sp)	#POP
	addu  $sp, $sp, 4
	lw    $t0, 4($sp)	#POP
	addu  $sp, $sp, 4
	add   $t0, $t0, $t1
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
	lw    $t0, _m
	sw    $t0, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	lw    $a0, 4($sp)	#POP
	addu  $sp, $sp, 4
	li    $v0, 1
	syscall
exit_main:
	li    $v0, 10
	syscall
```
对变量的识别
```
int max[3](int x[0], int y[1]) {
    int m[2];
    if ((x[0] > y[1])) {
        m[2] = x[0];
    }
    else {
        m[2] = y[1];
    }
    return m[2];
}

void main[3]() {
    int a[0];
    int b[1];
    int m[2];
    cout << "a: ";
    cin >> a[0];
    cout << "b: ";
    cin >> b[1];
    m[2] = max[3](a[0], b[1]);
    cout << "max = ";
    cout << m[2];
    cout << "\n";
}
```
编译失败，抛出语法错误
```
int max(int x, int y) {
  if ( x + y ) { // 2:8 ***ERROR*** Non-bool expression used as an if condition
    return x;
  }
  else {
    return y;
  }
}

void main() {
  int a;
  bool b;
  int m;
  
  cout << "a: ";
  cin >> a;
  cout << "b: ";
  cin >> b;
  
  m = max(a,b); // 20:13 ***ERROR*** Type of actual does not match type of formal
  cout << "the maximum is ";
  cout << m;
}
```
