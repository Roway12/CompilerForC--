package ast;

import java.io.*;
import java.util.*;
import lexer.*;
import symtable.*;
import codegen.*;

// **********************************************************************
// ASTnode class (container class for all other classes)
// **********************************************************************
public class AST {


// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************
public abstract static  class ASTnode { 
    
    public void unparse(PrintWriter p, int indent) {}

    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }
}

// **********************************************************************
// ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

public static class ProgramNode extends ASTnode {

    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    public void resolveOffset(){
        myDeclList.resolveOffset(0);
    }

    public void codeGen(PrintWriter p) {
        myDeclList.codeGen(p);
    }

    public void typeCheck() {
        myDeclList.typeCheck();
    }

    public void nameAnalysis() {
        SymTable symTab = new SymTable();
        myDeclList.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    private DeclListNode myDeclList;
}

public static class DeclListNode extends ASTnode {

    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }
    
    public int resolveOffset(int offset){
        for (DeclNode node : myDecls) {
            offset = node.resolveOffset(offset);
        }
        return offset;
    }

    public void codeGen(PrintWriter p){
        for (DeclNode node : myDecls) {
            try{
                node.codeGen(p);

                //function
                Codegen.genLabel(p, Codegen.nextReturnLabel());
                Codegen.generateIndexed(p, "lw", Codegen.RA, Codegen.FP, 0, "load return address");
                Codegen.generateWithComment(p, "move", "store address from FP to SP",  Codegen.T0, Codegen.FP);
                Codegen.generateIndexed(p, "lw", Codegen.FP, Codegen.FP, -4, "restore FP");
                Codegen.generateWithComment(p, "move", "restore SP",  Codegen.SP, Codegen.T0);
                Codegen.generate(p, "jr", Codegen.RA);
            }catch (NoSuchElementException e){
                System.err.println("unexpected NoSuchElementException in DeclListNode.CodeGen");
                System.exit(-1);
            }
        }
    }
       
    public void typeCheck() {
        boolean main = false;
        for (DeclNode node : myDecls) {
            if(node.typeCheck()){
                main = true;
            }
        }
        if(!main){
            ErrMsg.fatal(0,0,"No main function");
        }
    }

    public void nameAnalysis(SymTable symTab) {
        for (DeclNode node : myDecls) {
            node.nameAnalysis(symTab);
        }
    }
     
    public void nameAnalysis(SymTable structSymTab, SymTable globalTab) {
        for (DeclNode node : myDecls) {
            if (node instanceof VarDeclNode) {
                ((VarDeclNode)node).nameAnalysis(structSymTab, globalTab);
            } else {
                // this should never happen
                node.nameAnalysis(globalTab);
            }
        }
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        Iterator<DeclNode> it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                (it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }
    
    public List<DeclNode> getList() {
        return myDecls;
    }

    private List<DeclNode> myDecls;
}

public static class FormalsListNode extends ASTnode {

    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public int resolveOffset(int offset){
        if(this.length() == 0){
            return 0;
        }
        for (FormalDeclNode node : myFormals) {
            offset = node.resolveOffset(offset);
        }
        return offset;
    }

    public void codeGen(PrintWriter p) {
        for (FormalDeclNode formal : myFormals) {
            formal.codeGen(p);
        }
    }

    public List<Type.AbstractType> nameAnalysis(SymTable symTab) {
        List<Type.AbstractType> typeList = new LinkedList<Type.AbstractType>();
        for (FormalDeclNode node : myFormals) {
            SymInfo info = node.nameAnalysis(symTab);
            if (info != null) {
                typeList.add(info.getType());
            }
        }
        return typeList;
    }

    public int length() {
        return myFormals.size();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) {
            it.next().unparse(p, indent);
            while (it.hasNext()) {
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    private List<FormalDeclNode> myFormals;
}

public static class FnBodyNode extends ASTnode {

    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public int resolveOffset(int offset){
        offset = myDeclList.resolveOffset(offset);
        return myStmtList.resolveOffset(offset);
    }

    public void codeGen(PrintWriter p,String label) {
        myDeclList.codeGen(p);
        myStmtList.codeGen(p,label);
    }

    public void typeCheck(Type.AbstractType retType) {
        myStmtList.typeCheck(retType);
    }

    public void nameAnalysis(SymTable symTab) {
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    public int declSize(){
        return myDeclList.getList().size();
    }

    public List<DeclNode> getDeclList() {
        return myDeclList.getList();
    }

    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

public static class StmtListNode extends ASTnode {

    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public int resolveOffset(int offset){
        for( StmtNode node: myStmts ){
            node.resolveOffset(offset);
        }
        return offset;
    }

    public void codeGen(PrintWriter p,String label) {
        for(StmtNode node : myStmts) {
            node.codeGen(p,label);
        }
    }

    public void typeCheck(Type.AbstractType retType) {
        for(StmtNode node : myStmts) {
            node.typeCheck(retType);
        }
    }

    public void nameAnalysis(SymTable symTab) {
        for (StmtNode node : myStmts) {
            node.nameAnalysis(symTab);
        }
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    private List<StmtNode> myStmts;
}

public static class ExpListNode extends ASTnode {

    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void codeGen(PrintWriter p) {
        int space = 4;
        for ( int i = myExps.size() - 1; i >= 0; i-- ) {
            ExpNode exp = myExps.get(i);
            exp.codeGen(p);
            if (exp instanceof DotAccessExpNode){
                Codegen.genPop(p, Codegen.T0);
                Codegen.generate(p, "lw", Codegen.T1, Codegen.T0);
                Codegen.genPush(p, Codegen.T1);
            }
            Codegen.genPop(p, Codegen.T0);
            Codegen.generateIndexed(p, "sw", Codegen.T0, Codegen.SP,space);
            space += 4;
        }
    }
      
    public void typeCheck(List<Type.AbstractType> typeList) {
        int k = 0;
        try {
            for (ExpNode node : myExps) {
                Type.AbstractType actualType = node.typeCheck(); // actual type of arg
                
                if (!actualType.isErrorType()) { // if this is not an error
                    Type.AbstractType formalType = typeList.get(k); // get the formal type
                    if ( ! formalType.equals(actualType)) {
                        ErrMsg.fatal(node.lineNum, node.charNum,
                                     "Type of actual does not match type of formal");
                    }
                }
                k++;
            }
        } catch (NoSuchElementException e) {
            System.err.println("unexpected NoSuchElementException in ExpListNode.typeCheck");
            System.exit(-1);
        }
    }

    public void nameAnalysis(SymTable symTab) {
        for (ExpNode node : myExps) {
            node.nameAnalysis(symTab);
        }
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    public int size() {
        return myExps.size();
    }

    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its sub classes
// **********************************************************************

public static abstract class DeclNode extends ASTnode {

    public boolean typeCheck() { return false; }

    public void codeGen(PrintWriter p) { }
    
    public int resolveOffset(int offset){ return offset; }
   
    public abstract SymInfo nameAnalysis(SymTable symTab);
}

public static class VarDeclNode extends DeclNode {

    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;

    }

    @Override
    public int resolveOffset(int offset){
        if(myId.info().isGlobal()){
            return offset;
        }
        else{
            myId.info().setOffset(offset);
            return offset + 4;
        }
    }

    @Override
    public void codeGen(PrintWriter p){
        SymInfo info = myId.info();

        if(info.isGlobal()) {
            Codegen.generate(p,".data");
            Codegen.generateWithComment(p,".align 2","align on a word boundary");
            if (mySize == -1) {
                Codegen.generateLabeled(p, "_" + myId.name(), ".space 4", "");
            }
            else {
                Codegen.generateLabeled(p, "_" + myId.name(), ".word "+mySize, "");
            }
        }
    }

    public SymInfo nameAnalysis(SymTable symTab) {
        boolean badDecl = false;
        String name = myId.name();
        SymInfo info = null;
        IdNode structId = null;

        if (myType instanceof VoidNode) {  // check for void type
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Non-function declared void");
            badDecl = true;        
        }
        else if (myType instanceof StructNode) {
            structId = ((StructNode) myType).idNode();//the name of variable
            info = symTab.lookupGlobal(structId.name());//the type
            if (info == null || !(info instanceof StructDefInfo)) {
                ErrMsg.fatal(structId.lineNum, structId.charNum, 
                             "Invalid name of struct type");
                badDecl = true;
            }
            else {
                structId.link(info);
            }
        }
        SymInfo dup = symTab.lookupLocal(name);
        
        if (dup != null) {
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Multiple declaration of identifier");
            badDecl = true;            
        }
        if (!badDecl) {  // insert into symbol table
            if (myType instanceof StructNode) {
                info = new StructInfo(structId);
            }
            else {
                info = new SymInfo(myType.type());
            }
            symTab.addDecl(name, info);
            myId.link(info);

        }
        return info;
    }
    
    public SymInfo nameAnalysis(SymTable structSymTab, SymTable globalTab) {
        boolean badDecl = false;
        String name = myId.name();
        SymInfo info = null;
        IdNode structId = null;

        if (myType instanceof VoidNode) {  // check for void type
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Non-function declared void");
            badDecl = true;        
        }
        else if (myType instanceof StructNode) {
            structId = ((StructNode) myType).idNode();
            info = globalTab.lookupGlobal(structId.name());
            // if the name for the struct type is not found, 
            // or is not a struct type
            if (info == null || !(info instanceof StructDefInfo)) {
                ErrMsg.fatal(structId.lineNum, structId.charNum, 
                             "Invalid name of struct type");
                badDecl = true;
            }
            else {
                structId.link(info);
            }
        }
        SymInfo dup = structSymTab.lookupLocal(name);
        
        if (dup != null) {
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Multiple declaration of struct field");
            badDecl = true;            
        }
        if (!badDecl) {  // insert into symbol table
            if (myType instanceof StructNode) {
                info = new StructInfo(structId);
            }
            else {
                info = new SymInfo(myType.type());
            }
            structSymTab.addDecl(name, info);
            myId.link(info);
        }
        
        return info;
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }

    public IdNode getMyId() {
        return myId;
    }

    private TypeNode myType;
    private IdNode myId;
    private int mySize;

    public static int NOT_STRUCT = -1;
}

public static class FnDeclNode extends DeclNode {

    public FnDeclNode(TypeNode type, IdNode id, FormalsListNode formalList, FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    @Override
    public int resolveOffset(int offset){
        return myBody.resolveOffset(8);
    }

    @Override
    public void codeGen(PrintWriter p) {
        p.print("\t.text\n");

        if(this.myId.name().equals("main")) {
            p.print("\t.globl main\n");
            Codegen.genLabel(p,this.myId.name());
        }
         else {
             // PROJECT
            Codegen.generate(p, "j", this.myId.name());
            Codegen.genLabel(p, "_" + this.myId.name());
         }
        Codegen.genPush(p, Codegen.RA);
        Codegen.genPush(p, Codegen.FP);
        String exitLabel = "exit_" + this.myId.name();
        p.println("# Push space for the locals");
        Codegen.generate(p,"subu",
            Codegen.SP,Codegen.SP,4 * myBody.declSize());
        //Body
        this.myBody.codeGen(p,exitLabel);
        //label for return
        Codegen.genLabel(p,exitLabel); 
        // PROJECT
        
        if(this.myId.name().equals("main")) {
            Codegen.generate(p,"li",Codegen.V0,"10");
            Codegen.generate(p,"syscall");
        }
         else {
             // PROJECT;
            // function isn't main
            Codegen.generate(p, "j", Codegen.V1);
            Codegen.generate(p,"function call");
         }
    }
       
    @Override
    public boolean typeCheck() {
        myBody.typeCheck(myType.type());
        if(myId.name().equals("main") && myType.type().isVoidType() && myFormalsList.length() == 0){
            return true;
        }
        return false;
    }

    public SymInfo nameAnalysis(SymTable symTab) {
        String name = myId.name();
        FnInfo info = null; 

        SymInfo dup = symTab.lookupLocal(name);

        if (dup != null) {
            ErrMsg.fatal(myId.lineNum, myId.charNum,
                         "Multiply declared identifier");
        }
        else { // add function name to local symbol table
            info = new FnInfo(myType.type(), myFormalsList.length());
            symTab.addDecl(name, info);
            myId.link(info);
        }
        symTab.addScope();  // add a new scope for locals and params
        // process the formals
        List<Type.AbstractType> typeList = myFormalsList.nameAnalysis(symTab);
        if (info != null) {
            info.addFormals(typeList);
        }
        myBody.nameAnalysis(symTab);
        symTab.removeScope();
        return null;
    }    

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }

    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

public static class FormalDeclNode extends DeclNode {

    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    @Override
    public int resolveOffset(int offset){
        myId.info().setOffset(offset);
        return offset + 4;
    }

    public SymInfo nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        SymInfo info = null;
        
        if (myType instanceof VoidNode) {
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Non-function declared void");
            badDecl = true;        
        }

        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Multiply declared identifier");
            badDecl = true;
        }
        
        if ( ! badDecl ) {  // insert into symbol table
            info = new SymInfo(myType.type());
            symTab.addDecl(name, info);
            myId.link(info);
        }
        
        return info;
    } 

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    private TypeNode myType;
    private IdNode myId;
}

public static class StructDeclNode extends DeclNode {

    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }

    public SymInfo nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        
        SymInfo dup = symTab.lookupLocal(name);

        if (dup != null) {
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Multiply declared identifier");
            badDecl = true;            
        }

        SymTable structSymTab = new SymTable();

        myDeclList.nameAnalysis(structSymTab, symTab);
        
        if (!badDecl) {
            StructDefInfo info = new StructDefInfo(structSymTab);
            symTab.addDecl(name, info);
            myId.link(info);
        }
        
        return null;
    }    

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
		myId.unparse(p, 0);
		p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");

    }

    private IdNode myId;
	private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its sub classes
// **********************************************************************

public abstract static class TypeNode extends ASTnode {

    public abstract Type.AbstractType type();

    public void codeGen() {}
}

public static class IntNode extends TypeNode {

    public IntNode() {
    }

    public Type.AbstractType type() {
        return new Type.IntType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
}

public static class BoolNode extends TypeNode {

    public BoolNode() {
    }
 
    public Type.AbstractType type() {
        return new Type.BoolType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }
}

public static class VoidNode extends TypeNode {

    public VoidNode() {
    }
 
    public Type.AbstractType type() {
        return new Type.VoidType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
}

public static class StructNode extends TypeNode {

    public StructNode(IdNode id) {
      myId = id;
    }

    public Type.AbstractType type() {
        return new Type.StructType(myId);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        myId.unparse(p, 0);
    }

    public IdNode idNode() {
      return myId;
    }

    public int resolveOffset(int offset) {
        SymInfo info = myId.info();
        StructDefInfo Info = (StructDefInfo) info;
        return offset - Info.getSize();
    }

    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

public abstract static class StmtNode extends ASTnode {

    public void nameAnalysis(SymTable symTab) {}
    
    public int resolveOffset(int offset){
        return offset;
    }

    public void codeGen(PrintWriter p,String label) {}

    public abstract void typeCheck(Type.AbstractType retType);
}

public static class AssignStmtNode extends StmtNode {

    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    @Override
    public void codeGen(PrintWriter p,String label) {
        myAssign.codeGen(p);
        Codegen.genPop(p,Codegen.T0);
    }    

    public void typeCheck(Type.AbstractType retType) {
        myAssign.typeCheck();
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myAssign.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    private AssignNode myAssign;
}

public static class PostIncStmtNode extends StmtNode {

    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    @Override
    public void codeGen(PrintWriter p,String label) {
        IdNode idExp = (IdNode) myExp;            
        idExp.codeGen(p);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"add", Codegen.T0, Codegen.T0, 1);           

        if(myExp instanceof IdNode)
        {
            SymInfo info = idExp.info();
            if(info.isGlobal())
                Codegen.generate(p, "sw", Codegen.T0,"_" + idExp.name());
            else
                Codegen.generateIndexed(p,"sw",Codegen.T0,Codegen.FP,-info.getOffset());
        }
        if (myExp instanceof DotAccessExpNode ){
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.generateWithComment(p, "addi", "PostInc", Codegen.T1, Codegen.T1, "1");
        }
    }
    
    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        
        if ( ! type.isErrorType() && ! type.isIntType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Arithmetic operator applied to non-numeric operand");
        }
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }

    private ExpNode myExp;
}

public static class PostDecStmtNode extends StmtNode {

    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    @Override
    public void codeGen(PrintWriter p,String label) {
        IdNode idExp = (IdNode)myExp;            
        idExp.codeGen(p);
       
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"sub", Codegen.T0, Codegen.T0, 1);           
        
        if(myExp instanceof IdNode)
        {
            SymInfo info = idExp.info();
            if(info.isGlobal())
                Codegen.generate(p, "sw", Codegen.T0,"_" + idExp.name());
            else
                Codegen.generateIndexed(p,"sw",Codegen.T0,Codegen.FP,- info.getOffset());
        }
        if (myExp instanceof DotAccessExpNode ){
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.generateWithComment(p, "addi", "PostDec", Codegen.T1, Codegen.T1, "1");
        }
    }

    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        
        if ( ! type.isErrorType() && ! type.isIntType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Arithmetic operator applied to non-numeric operand");
        }
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }

    private ExpNode myExp;
}

public static class ReadStmtNode extends StmtNode {

    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }
    
    @Override
    public void codeGen(PrintWriter p,String label){
        if(myExp instanceof IdNode){
            Codegen.generate(p,"li", Codegen.V0, 5);
            Codegen.generate(p,"syscall");
            ((IdNode) myExp).genAddr(p);
            Codegen.genPop(p,Codegen.T0);
            Codegen.generateIndexed(p,"sw", Codegen.V0, Codegen.T0, 0 );
        }
    } 

    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        
        if ( type.isFnType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to read a function");
        }
        if ( type.isStructDefType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to read a struct name");
        }
        if ( type.isStructType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to read a struct variable");
        }
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }    

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    private ExpNode myExp;
}

public static class WriteStmtNode extends StmtNode {

    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    @Override
    public void codeGen(PrintWriter p,String label) {
        myExp.codeGen(p);
        Codegen.genPop(p,Codegen.A0);
        
        if( myExp instanceof StringLitNode ) {
            Codegen.generate(p,"li", Codegen.V0, 4);
        }
        else if (myExp instanceof DotAccessExpNode){
            Codegen.genPop(p, Codegen.T0);
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.genPush(p, Codegen.T1);
        }
        else {
            Codegen.generate(p,"li", Codegen.V0, 1);  //int
        }
        Codegen.generate(p,"syscall");         
    }

    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        
        if (type.isFnType()) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to write a function");
        }
        if (type.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to write a struct name");
        }
        if (type.isStructType()) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to write a struct variable");
        }
        if (type.isVoidType()) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Attempt to write void");
        }
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    private ExpNode myExp;
}

public static class IfStmtNode extends StmtNode {

    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }
    
    @Override
    public int resolveOffset(int offset){
        offset = myDeclList.resolveOffset(offset);
        return myStmtList.resolveOffset(offset);
    }

    @Override
    public void codeGen(PrintWriter p,String label) {
        String endiflabel = Codegen.nextEndifLabel();
        myExp.codeGen(p);
        Codegen.generate(p,"li",Codegen.T1,1);
        Codegen.generate(p,"bne", Codegen.T0, Codegen.T1, endiflabel);
        p.println();
        myStmtList.codeGen(p,label);
        Codegen.generate(p,"nop");
        Codegen.genLabel(p,endiflabel);
    }

    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        if ( ! type.isErrorType() && ! type.isBoolType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Non-bool expression used as an if condition");        
        }
        myStmtList.typeCheck(retType);
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();      
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        symTab.removeScope();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

public static class IfElseStmtNode extends StmtNode {

    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    @Override
    public int resolveOffset(int offset){
        offset = myThenDeclList.resolveOffset(offset);
        offset = myThenStmtList.resolveOffset(offset);
        offset = myElseDeclList.resolveOffset(offset);
        return myElseStmtList.resolveOffset(offset);
    }

    @Override
    public void codeGen(PrintWriter p,String label) {
        myExp.codeGen(p);

        String elselabel = Codegen.nextElseLabel();
        Codegen.generate(p,"li",Codegen.T1,1);
        Codegen.generate(p,"bne", Codegen.T0, Codegen.T1,elselabel);
        
        myThenStmtList.codeGen(p,label);
        String endiflabel = Codegen.nextEndifLabel();

        Codegen.generate(p,"j", endiflabel);  
        
        Codegen.genLabel(p,elselabel);
        myElseStmtList.codeGen(p,label);
        
        Codegen.genLabel(p,endiflabel);
    }
    
    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        if ( ! type.isErrorType() && ! type.isBoolType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Non-bool expression used as an if condition");        
        }
        myThenStmtList.typeCheck(retType);
        myElseStmtList.typeCheck(retType);
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();             
        myThenDeclList.nameAnalysis(symTab);
        myThenStmtList.nameAnalysis(symTab);               
        symTab.removeScope();
        symTab.addScope();                
        myElseDeclList.nameAnalysis(symTab);
        myElseStmtList.nameAnalysis(symTab);                
        symTab.removeScope();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");        
    }

    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

public static class WhileStmtNode extends StmtNode {

    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    @Override
    public int resolveOffset(int offset){
        offset = myDeclList.resolveOffset(offset);
        return myStmtList.resolveOffset(offset);
    }

    @Override
    public void codeGen(PrintWriter p,String label) {
        String nextLoopLabel = Codegen.nextLoopLabel();                
        String endlloopLabel = Codegen.nextEndloopLabel();
        Codegen.genLabel(p,nextLoopLabel);
        myExp.codeGen(p);

        Codegen.generate(p,"li",Codegen.T1,1);
        Codegen.generate(p,"bne", Codegen.T0, Codegen.T1, endlloopLabel);
        
        myStmtList.codeGen(p,label);
        Codegen.generate(p,"j", nextLoopLabel);
        Codegen.genLabel(p,endlloopLabel);
    }
     
    public void typeCheck(Type.AbstractType retType) {
        Type.AbstractType type = myExp.typeCheck();
        if ( ! type.isErrorType() && ! type.isBoolType() ) {
            ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                         "Non-bool expression used as a while condition");        
        }
        myStmtList.typeCheck(retType);
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        symTab.removeScope();
    }
	
    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

public static class CallStmtNode extends StmtNode {

    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }
    
    @Override
    public void codeGen(PrintWriter p,String label){
        this.myCall.codeGen(p);
        Codegen.genPop(p,Codegen.T0);
    }

    public void typeCheck(Type.AbstractType retType) {
        myCall.typeCheck();
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myCall.nameAnalysis(symTab);
    }

    private CallExpNode myCall;
}

public static class ReturnStmtNode extends StmtNode {

    public ReturnStmtNode(ExpNode exp) {
        this(exp,0,0);
    }

    public ReturnStmtNode(ExpNode exp, int charnum, int linenum) {
        myExp = exp;
        myCharnum = charnum;
        myLinenum = linenum;
    }

    @Override
    public void codeGen(PrintWriter p,String exitLabel) {
        if (myExp != null) {
            myExp.codeGen(p);
            Codegen.genPop(p,Codegen.V0);
        }
        Codegen.generate(p,"j", exitLabel);
    }
 
    public void typeCheck(Type.AbstractType retType) {
        if (myExp != null) {  // return value given
            Type.AbstractType type = myExp.typeCheck();
            if ( retType.isVoidType() ) {
                ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                             "Return with a value in a void function");                
            }
            else if ( ! retType.isErrorType() && ! type.isErrorType() && ! retType.equals(type) ) {
                ErrMsg.fatal(myExp.lineNum, myExp.charNum,
                             "Bad return value");
            }
        }
        else {  // no return value given -- ok if this is a void function
            if ( ! retType.isVoidType() ) {
                ErrMsg.fatal(myLinenum, myCharnum, "Missing return value");                
            }
        }       
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        if (myExp != null) {
            myExp.nameAnalysis(symTab);
        }
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    private ExpNode myExp; // possibly null
    private int myCharnum;
    private int myLinenum;
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

public abstract static class ExpNode extends ASTnode {

    protected ExpNode() {
        lineNum = 0;
        charNum = 0;
    }

    protected ExpNode(int lineNum, int charNum) {
        this.lineNum = lineNum;
        this.charNum = charNum;
    }
    
    public abstract Type.AbstractType typeCheck();
    
    public void nameAnalysis(SymTable symTab) { }
    public void codeGen(PrintWriter p){}

    protected int lineNum;
    protected int charNum;
}

public static class IntLitNode extends ExpNode {
  
    public IntLitNode(int lineNum, int charNum, int intVal) {
        super(lineNum,charNum);
        myIntVal = intVal;
    }
    
    @Override
    public void codeGen(PrintWriter p) {
        Codegen.generate(p,"li", Codegen.T0, myIntVal);
        Codegen.genPush(p,Codegen.T0);
    }

    public Type.AbstractType typeCheck() {
        return new Type.IntType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    private int myIntVal;
}

public static class StringLitNode extends ExpNode {

    public StringLitNode(int lineNum, int charNum, String strVal) {
        super(lineNum,charNum);
        myStrVal = strVal;
    }

    @Override
    public void codeGen(PrintWriter p) {
        String strlabel = Codegen.StringLabel();
        p.println("\t.data");
        Codegen.generateLabeled(p,strlabel, ".asciiz " + myStrVal, "");
        p.println("\t.text");
        Codegen.generate(p,"la", Codegen.T0, strlabel);
        Codegen.genPush(p,Codegen.T0);
    }

    public Type.AbstractType typeCheck() {
        return new Type.StringType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    private String myStrVal;
}

public static class TrueNode extends ExpNode {

    public TrueNode(int lineNum, int charNum) {
        super(lineNum,charNum);
    }
    
    @Override
    public void codeGen(PrintWriter p) {
        Codegen.generate(p,"li", Codegen.T0, 1);
        Codegen.genPush(p,Codegen.T0);
    }    

    public Type.AbstractType typeCheck() {
        return new Type.BoolType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }
}

public static class FalseNode extends ExpNode {

    public FalseNode(int lineNum, int charNum) {
        super(lineNum,charNum);
    }

    @Override
    public void codeGen(PrintWriter p) {
        Codegen.generate(p,"li", Codegen.T0, 0);
        Codegen.genPush(p,Codegen.T0);
    }

    public Type.AbstractType typeCheck() {
        return new Type.BoolType();
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }
}

public static class IdNode extends ExpNode {

    public IdNode(int lineNum, int charNum, String strVal) {
        super(lineNum,charNum);
        myStrVal = strVal;
    }

    public void genJumpAndLink(PrintWriter p) {
        String fn = myStrVal;
        if(!myStrVal.equals("main")){
            fn = "_" + fn;
        }
        Codegen.generate(p, "la", Codegen.T0, fn);
        Codegen.generate(p, "jalr", Codegen.T0);
    }

    @Override
    public void codeGen(PrintWriter p) {
        if(this.info().isGlobal()) {
            Codegen.generate(p,"lw", Codegen.T0, "_" + this.myStrVal);
            Codegen.genPush(p,Codegen.T0);
        } 
        else {
            Codegen.generateIndexed(p,"lw", Codegen.T0, Codegen.FP, 
                    - this.info().getOffset());
            Codegen.genPush(p,Codegen.T0);
        }
    }
    
    public void genAddr(PrintWriter p) {
        if(this.info().isGlobal()) {
            Codegen.generate(p,"la", Codegen.T0, "_" + this.myStrVal);
            Codegen.genPush(p,Codegen.T0);
        } else {
            Codegen.generateIndexed(p,"la", Codegen.T0, Codegen.FP, 
                    -this.info().getOffset(), "Generate Address");
            Codegen.genPush(p,Codegen.T0);
        }       
    }

    public Type.AbstractType typeCheck() {
        if ( myInfo != null ) {
            return myInfo.getType();
        } 
        else {
            ErrMsg.fatal(lineNum, charNum, "ID with null info field in IdNode.typeCheck()");
            System.exit(-1);
        }
        return null;
    }
    
    @Override
    public void nameAnalysis(SymTable symTab) {
        SymInfo info = symTab.lookupGlobal(myStrVal);
        if (info == null) {
            ErrMsg.fatal(lineNum, charNum, "Undeclared identifier");
        } else {
            link(info);
        }
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal + "[" + myInfo.getOffset() + "]");
    }
    
    public void link(SymInfo info) {
        myInfo = info;
    }
    
    public String name() {
        return myStrVal;
    }

    public SymInfo info() {
        return myInfo;
    }   

    private String myStrVal;
    private SymInfo myInfo;
}

public static class DotAccessExpNode extends ExpNode {

    public DotAccessExpNode(ExpNode lhs, IdNode id) {
        super(id.lineNum,id.charNum);
        myLhs = lhs;
        myId = id;
        myInfo = null;
    }
  
    public Type.AbstractType typeCheck() {
        return myId.typeCheck();
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        badAccess = false;
        SymTable structSymTab = null; // to lookup RHS of dot-access
        SymInfo info = null;
        myLhs.nameAnalysis(symTab);  // do name analysis on LHS
        if (myLhs instanceof IdNode) {
            IdNode id = (IdNode)myLhs;
            info = id.info();
            if (info == null) { // ID was undeclared
                badAccess = true;
            }
            else if (info instanceof StructInfo) { 
                SymInfo tempSym = ((StructInfo)info).getStructType().info();
                structSymTab = ((StructDefInfo)tempSym).getSymTable();
            } 
            else {  // LHS is not a struct type
                ErrMsg.fatal(id.lineNum, id.charNum, 
                             "Dot-access of non-struct type");
                badAccess = true;
            }
        }
        else if (myLhs instanceof DotAccessExpNode) {
            DotAccessExpNode lhs = (DotAccessExpNode) myLhs;
            if (lhs.badAccess) {  // if errors in processing myLhs
                badAccess = true; // don't continue proccessing this dot-access
            }
            else { //  no errors in processing myLhs
                info = lhs.info();

                if (info == null) {  // no struct in which to look up RHS
                    ErrMsg.fatal(lhs.lineNum, lhs.charNum, 
                                 "Dot-access of non-struct type");
                    badAccess = true;
                }
                else {  // get the struct's symbol table in which to lookup RHS
                    if (info instanceof StructDefInfo) {
                        structSymTab = ((StructDefInfo)info).getSymTable();
                    }
                    else {
                        System.err.println("Unexpected Sym type in DotAccessExpNode");
                        System.exit(-1);
                    }
                }
            }
        }
        else { // don't know what kind of thing myLhs is
            System.err.println("Unexpected node type in LHS of dot-access");
            System.exit(-1);
        }
        // do name analysis on RHS of dot-access in the struct's symbol table
        if (!badAccess) {
            info = structSymTab.lookupGlobal(myId.name()); // lookup
                
            if (info == null) { // not found - RHS is not a valid field name
                ErrMsg.fatal(myId.lineNum, myId.charNum, 
                             "Invalid struct field name");
                badAccess = true;
            }
            else {
                myId.link(info);  // link the symbol
                // if RHS is itself as struct type, link the symbol for its struct 
                // type to this dot-access node (to allow chained dot-access)
                if (info instanceof StructInfo) {
                    myInfo = ((StructInfo)info).getStructType().info();
                }
            }
        }
    }

    public void codeGen(PrintWriter p){

        int offset = 0;

        // if myLhs is IdNode
        if(myLhs instanceof IdNode) {
            IdNode idnode = (IdNode)myLhs;
            SymInfo info1 = idnode.info();
            int offset1 = info1.getOffset();
            SymInfo info2 = myId.info();
            int offset2 = info2.getOffset();
            offset = offset1 + offset2;
            Codegen.generateIndexed(p, "la", Codegen.T0, Codegen.FP, offset);
            Codegen.genPush(p, Codegen.T0);
        }
        if (myLhs instanceof DotAccessExpNode){

        }
    }

    public SymInfo info() {
        return myInfo;
    }    
    
    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myLhs.unparse(p, 0);
		p.print(").");
		myId.unparse(p, 0);
    }

    private ExpNode myLhs;	
    private IdNode myId;
    private SymInfo myInfo;    // link to SymInfo for struct type
    private boolean badAccess; // to prevent multiple, cascading errors
}

public static class AssignNode extends ExpNode {

    public AssignNode(ExpNode lhs, ExpNode rhs) {
        super();
        myLhs = lhs;
        myRhs = rhs;
    }

    @Override
    public void codeGen(PrintWriter p){
        myRhs.codeGen(p);
        if (myRhs instanceof DotAccessExpNode){
            Codegen.genPop(p, Codegen.T0);   // T0 store the address
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.genPush(p, Codegen.T1);
        }
        Codegen.genPop(p, Codegen.T1);

        if (myLhs instanceof IdNode){
            ((IdNode)myLhs).genAddr(p);
            Codegen.genPop(p,Codegen.T1);
            Codegen.genPop(p,Codegen.T0);
            Codegen.generateIndexed(p,"sw", Codegen.T0, Codegen.T1, 0);
            Codegen.genPush(p,Codegen.T0);
        }
        else if (myLhs instanceof DotAccessExpNode){
            myLhs.codeGen(p);
            Codegen.genPop(p, Codegen.T0);
            Codegen.genPush(p, Codegen.T1);  //push the value
            Codegen.generate(p, "sw",Codegen.T1, "(" + Codegen.T0 + ")");
        }
        Codegen.genPop(p,Codegen.T1);

    } 

    public Type.AbstractType typeCheck() {
        Type.AbstractType typeLhs = myLhs.typeCheck();
        Type.AbstractType typeExp = myRhs.typeCheck();
        Type.AbstractType retType = typeLhs;
        
        if (typeLhs.isFnType() && typeExp.isFnType()) {
            ErrMsg.fatal(myLhs.lineNum, charNum, "Function assignment");
            retType = new Type.ErrorType();
        }
        if (typeLhs.isStructDefType() && typeExp.isStructDefType()) {
            ErrMsg.fatal(myLhs.lineNum, charNum, "Struct name assignment");
            retType = new Type.ErrorType();
        }
        if (typeLhs.isStructType() && typeExp.isStructType()) {
            ErrMsg.fatal(myLhs.lineNum, charNum, "Struct variable assignment");
            retType = new Type.ErrorType();
        }        
        if (!typeLhs.equals(typeExp) && !typeLhs.isErrorType() && !typeExp.isErrorType()) {
            ErrMsg.fatal(myLhs.lineNum, charNum, "Type mismatch");
            retType = new Type.ErrorType();
        }
        if (typeLhs.isErrorType() || typeExp.isErrorType()) {
            retType = new Type.ErrorType();
        }
        
        return retType;
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myRhs.nameAnalysis(symTab);
        myLhs.nameAnalysis(symTab);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myRhs.unparse(p, 0);
        if (indent != -1)  p.print(")");
    }

    private ExpNode myLhs;
    private ExpNode myRhs;
}

public static class CallExpNode extends ExpNode {

    public CallExpNode(IdNode name, ExpListNode elist) {
        super();
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        this(name,new ExpListNode(new LinkedList<>()));
    }

    public Type.AbstractType typeCheck() {
        if ( ! myId.typeCheck().isFnType() ) {  
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Attempt to call a non-function");
            return new Type.ErrorType();
        }
        FnInfo fnInfo = (FnInfo)(myId.info());
        if ( fnInfo == null ) {
            System.err.println("null sym for Id in CallExpNode.typeCheck");
            System.exit(-1);
        }
        
        if ( myExpList.size() != fnInfo.getNumParams() ) {
            ErrMsg.fatal(myId.lineNum, myId.charNum, 
                         "Function call with wrong number of args");
            return fnInfo.getReturnType();
        }
        myExpList.typeCheck(fnInfo.getParamTypes());
        return fnInfo.getReturnType();
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    }    

    @Override
    public void unparse(PrintWriter p, int indent) {
	    myId.unparse(p, 0);
		  p.print("(");
		  if (myExpList != null) {
			 myExpList.unparse(p, 0);
		  }
		  p.print(")");
    }

    public void codeGen(PrintWriter p) {
        myExpList.codeGen(p);
        myId.genJumpAndLink(p);
        Codegen.genPush(p, Codegen.V0); //push return value and pop it in callStmtNode
    }

    private IdNode myId;
    private ExpListNode myExpList;
}

public abstract static class UnaryExpNode extends ExpNode {

    protected UnaryExpNode(ExpNode exp) {
        super(exp.lineNum,exp.charNum);
        myExp = exp;
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    protected ExpNode myExp;
}

public abstract static class BinaryExpNode extends ExpNode {

    protected BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1.lineNum,exp1.charNum);
        myExp1 = exp1;
        myExp2 = exp2;
    }

    @Override
    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    }

    public void codeGen(PrintWriter p) {
        myExp1.codeGen(p);
        if(myExp1 instanceof DotAccessExpNode) {
            Codegen.genPop(p, Codegen.T0);
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.genPush(p, Codegen.T1);
        }

        myExp2.codeGen(p);
        if(myExp2 instanceof DotAccessExpNode) {
            Codegen.genPop(p, Codegen.T0);
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.genPush(p, Codegen.T1);
        }
        Codegen.genPop(p, Codegen.T1);
        Codegen.genPop(p, Codegen.T0);
    }

    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

public static class UnaryMinusNode extends UnaryExpNode {

    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    @Override
    public void codeGen(PrintWriter p) {
        this.myExp.codeGen(p);
        if(myExp instanceof DotAccessExpNode) {
            Codegen.genPop(p, Codegen.T0);
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.genPush(p, Codegen.T1);
        }
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"li", Codegen.T1,0);
        Codegen.generate(p,"sub",Codegen.T0, Codegen.T1, Codegen.T0);
        Codegen.genPush(p,Codegen.T0);
    }

    public Type.AbstractType typeCheck() {
        Type.AbstractType type = myExp.typeCheck();
        Type.AbstractType retType = new Type.IntType();
        
        if ( ! type.isErrorType() && ! type.isIntType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Arithmetic operator applied to non-numeric operand");
            retType = new Type.ErrorType();
        }
        if (type.isErrorType()) {
            retType = new Type.ErrorType();
        }
        return retType;
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

public static class NotNode extends UnaryExpNode {

    public NotNode(ExpNode exp) {
        super(exp);
    }

    @Override
    public void codeGen(PrintWriter p) {
        this.myExp.codeGen(p);
        if(myExp instanceof DotAccessExpNode) {
            Codegen.genPop(p, Codegen.T0);
            Codegen.generate(p, "lw", Codegen.T1, "(" + Codegen.T0 + ")");
            Codegen.genPush(p, Codegen.T1);
        }
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"seq",Codegen.T0, Codegen.T0, "0");
        Codegen.genPush(p,Codegen.T0);
    }

    public Type.AbstractType typeCheck() {
        Type.AbstractType type = myExp.typeCheck();
        Type.AbstractType retType = new Type.BoolType();
        if ( ! type.isErrorType() && ! type.isBoolType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Logical operator applied to non-bool operand");
            retType = new Type.ErrorType();
        }
        if ( type.isErrorType() ) {
            retType = new Type.ErrorType();
        }
        return retType;
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(!");
		myExp.unparse(p, 0);
		p.print(")");
    }
}

//////////

public abstract static class ArithmeticExpNode extends BinaryExpNode {

    protected ArithmeticExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public Type.AbstractType typeCheck() {
        Type.AbstractType type1 = myExp1.typeCheck();
        Type.AbstractType type2 = myExp2.typeCheck();
        Type.AbstractType retType = new Type.IntType();
        
        if ( ! type1.isErrorType() && ! type1.isIntType() ) {
            ErrMsg.fatal(myExp1.lineNum, myExp1.charNum,
                         "Arithmetic operator applied to non-numeric operand");
            retType = new Type.ErrorType();
        }
        if ( ! type2.isErrorType() && ! type2.isIntType() ) {
            ErrMsg.fatal(myExp2.lineNum, myExp2.charNum,
                         "Arithmetic operator applied to non-numeric operand");
            retType = new Type.ErrorType();
        }
        if ( type1.isErrorType() || type2.isErrorType() ) {
            retType = new Type.ErrorType();
        }
        return retType;
    }
}

public abstract static class LogicalExpNode extends BinaryExpNode {

    protected LogicalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public Type.AbstractType typeCheck() {
        Type.AbstractType type1 = myExp1.typeCheck();
        Type.AbstractType type2 = myExp2.typeCheck();
        Type.AbstractType retType = new Type.BoolType();
        
        if ( ! type1.isErrorType() && ! type1.isBoolType() ) {
            ErrMsg.fatal(myExp1.lineNum, myExp1.charNum,
                         "Logical operator applied to non-bool operand");
            retType = new Type.ErrorType();
        }
        if ( ! type2.isErrorType() && ! type2.isBoolType() ) {
            ErrMsg.fatal(myExp2.lineNum, myExp2.charNum,
                         "Logical operator applied to non-bool operand");
            retType = new Type.ErrorType();
        }
        if ( type1.isErrorType() || type2.isErrorType() ) {
            retType = new Type.ErrorType();
        }
        return retType;
    }
}

public abstract static class EqualityExpNode extends BinaryExpNode {

    protected EqualityExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public Type.AbstractType typeCheck() {
        Type.AbstractType type1 = myExp1.typeCheck();
        Type.AbstractType type2 = myExp2.typeCheck();
        Type.AbstractType retType = new Type.BoolType();
        
        if ( type1.isVoidType() && type2.isVoidType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Equality operator applied to void functions");
            retType = new Type.ErrorType();
        }
        if ( type1.isFnType() && type2.isFnType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Equality operator applied to functions");
            retType = new Type.ErrorType();
        }
        if ( type1.isStructDefType() && type2.isStructDefType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Equality operator applied to struct names");
            retType = new Type.ErrorType();
        }
        if ( type1.isStructType() && type2.isStructType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Equality operator applied to struct variables");
            retType = new Type.ErrorType();
        }        
        if ( ! type1.equals(type2) && ! type1.isErrorType() && ! type2.isErrorType() ) {
            ErrMsg.fatal(lineNum, charNum,
                         "Type mismatch");
            retType = new Type.ErrorType();
        }
        if ( type1.isErrorType() || type2.isErrorType() ) {
            retType = new Type.ErrorType();
        }
        return retType;
    }
}

public abstract static class RelationalExpNode extends BinaryExpNode {

    protected RelationalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public Type.AbstractType typeCheck() {
        Type.AbstractType type1 = myExp1.typeCheck();
        Type.AbstractType type2 = myExp2.typeCheck();
        Type.AbstractType retType = new Type.BoolType();
        
        if ( ! type1.isErrorType() && ! type1.isIntType() ) {
            ErrMsg.fatal(myExp1.lineNum, myExp1.charNum,
                         "Relational operator applied to non-numeric operand");
            retType = new Type.ErrorType();
        }
        if ( ! type2.isErrorType() && ! type2.isIntType() ) {
            ErrMsg.fatal(myExp2.lineNum, myExp2.charNum,
                         "Relational operator applied to non-numeric operand");
            retType = new Type.ErrorType();
        }
        if ( type1.isErrorType() || type2.isErrorType() ) {
            retType = new Type.ErrorType();
        }
        return retType;
    }
}

// **********************************************************************
// Subp classes of BinaryExpNode
// **********************************************************************

public static class PlusNode extends ArithmeticExpNode {

    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        myExp1.codeGen(p);
        myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"add",Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" + ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class MinusNode extends ArithmeticExpNode {

    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    @Override
    public void codeGen(PrintWriter p) {
        myExp1.codeGen(p);
        myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"sub", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" - ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class TimesNode extends ArithmeticExpNode {

    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        myExp1.codeGen(p);
        myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"mul", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" * ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class DivideNode extends ArithmeticExpNode {

    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        myExp1.codeGen(p);
        myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"div", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" / ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class AndNode extends LogicalExpNode {

    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        String shortLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"bne",Codegen.T0, "0", shortLabel); // If not false, use RHS as solution
        // the left is 0
        Codegen.genPush(p,Codegen.T0); // Uses LHS as solution
        Codegen.generate(p,"b", exitLabel);
        // the left is not 0,so start short label
        Codegen.genLabel(p,shortLabel);
        this.myExp2.codeGen(p); // Just leave RHS on stack as solution
        Codegen.genLabel(p,exitLabel);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" && ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class OrNode extends LogicalExpNode {

    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        String shortLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        Codegen.genPop(p,Codegen.T0);
        Codegen.generate(p,"bne",Codegen.T0, "1", shortLabel); //If not true, use RHS as solution

        Codegen.genPush(p,Codegen.T0); //Uses LHS as solution
        Codegen.generate(p,"b", exitLabel);
        
        Codegen.genLabel(p,shortLabel);
        this.myExp2.codeGen(p); //Just leave RHS on stack as solution
        Codegen.genLabel(p,exitLabel);    
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" || ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class EqualsNode extends EqualityExpNode {

    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p){
        String trueLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        this.myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        
        Codegen.generate(p,"beq",Codegen.T0, Codegen.T1, trueLabel); 
        
        //Set false
        Codegen.generate(p,"li",Codegen.T0, 0); 
        Codegen.generate(p,"b",exitLabel);    
        
        Codegen.genLabel(p,trueLabel);
        Codegen.generate(p,"li",Codegen.T0, 1);
       
        Codegen.genLabel(p,exitLabel);
        Codegen.genPush(p,Codegen.T0);    
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" == ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class NotEqualsNode extends EqualityExpNode {

    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p){
        String trueLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        
        this.myExp1.codeGen(p);
        this.myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        
        Codegen.generate(p,"bne",Codegen.T0, Codegen.T1, trueLabel); 
       
        //Set false
        Codegen.generate(p,"li",Codegen.T0, 0); 
        Codegen.generate(p,"b",exitLabel);    
        
        Codegen.genLabel(p,trueLabel);
        Codegen.generate(p,"li",Codegen.T0, 1);
       
        Codegen.genLabel(p,exitLabel);
        Codegen.genPush(p,Codegen.T0);    
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" != ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class LessNode extends RelationalExpNode {

    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        String falseLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        this.myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        
        //T0 > T1 -> !(0 <= T1-T0)
        Codegen.generate(p,"sub",Codegen.T0, Codegen.T0, Codegen.T1); 
        Codegen.generate(p,"bgez",Codegen.T0, falseLabel); 
       
       //Set false
        Codegen.generate(p,"li",Codegen.T0, 1); 
        Codegen.generate(p,"b",exitLabel);    
        
        Codegen.genLabel(p,falseLabel);
        Codegen.generate(p,"li",Codegen.T0, 0);
       
        Codegen.genLabel(p,exitLabel);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" < ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class GreaterNode extends RelationalExpNode {

    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        String falseLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        this.myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        
        //T0 > T1 -> !(0 <= T1-T0)
        Codegen.generate(p,"sub",Codegen.T0, Codegen.T1, Codegen.T0); 
        Codegen.generate(p,"bgez",Codegen.T0, falseLabel); 
       
        //Set false
        Codegen.generate(p,"li",Codegen.T0, 1); 
        Codegen.generate(p,"b",exitLabel);    
        
        Codegen.genLabel(p,falseLabel);
        Codegen.generate(p,"li",Codegen.T0, 0);
       
        Codegen.genLabel(p,exitLabel);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" > ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class LessEqNode extends RelationalExpNode {

    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        String trueLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        this.myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        
        Codegen.generate(p,"sub",Codegen.T0, Codegen.T1, Codegen.T0); 
        Codegen.generate(p,"bgez",Codegen.T0, trueLabel); 
       
       //Set false
        Codegen.generate(p,"li",Codegen.T0, 0); 
        Codegen.generate(p,"b",exitLabel);    
        
        Codegen.genLabel(p,trueLabel);
        Codegen.generate(p,"li",Codegen.T0, 1);
       
        Codegen.genLabel(p,exitLabel);
        Codegen.genPush(p,Codegen.T0);
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" <= ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

public static class GreaterEqNode extends RelationalExpNode {

    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    @Override
    public void codeGen(PrintWriter p) {
        String trueLabel = Codegen.nextLabel();
        String exitLabel = Codegen.nextLabel();
        
        this.myExp1.codeGen(p);
        this.myExp2.codeGen(p);
        Codegen.genPop(p,Codegen.T1);
        Codegen.genPop(p,Codegen.T0);
        
        Codegen.generate(p,"sub",Codegen.T0, Codegen.T0, Codegen.T1); 
        Codegen.generate(p,"bgez",Codegen.T0, trueLabel); 
       
       //Set false
        Codegen.generate(p,"li",Codegen.T0, 0); 
        Codegen.generate(p,"b",exitLabel);    
        
        Codegen.genLabel(p,trueLabel);
        Codegen.generate(p,"li",Codegen.T0, 1);
       
        Codegen.genLabel(p,exitLabel);
        Codegen.genPush(p,Codegen.T0);        
    }

    @Override
    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" >= ");
	 	myExp2.unparse(p, 0);
		p.print(")");
    }
}
}
