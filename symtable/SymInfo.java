package symtable;

import java.util.*;

/**
 * The SymInfo class defines a Symbol Table entry. 
 * Each SymInfo contains a type (a Type object).
 */
public class SymInfo {
    
    private Type.AbstractType type;
    private int offset;
    private boolean isGlobalVar = false;
    /**
     * Create a SymInfo object of type 'type'
     */
    public SymInfo(Type.AbstractType type) {
        this.type = type;
    }

    public SymInfo(Type.AbstractType type, int offset) {
        this.type = type;
        this.offset = offset;
    }
    /**
     * Return the type of the SymInfo
     */
    public Type.AbstractType getType() {
        return type;
    }
    /**
     * Return a String representation of the SymInfo
     * (for debuggung purpose)
     */
    public String toString() {
        return type.toString();
    }

    //Offset set/get
    public int getOffset(){
        return this.offset;
    }

    public void setOffset(int offset){
        this.offset = offset;
    }

    //isGlobalVar set/get
    public void setGlobal(boolean isGlobalVar){
        this.isGlobalVar = isGlobalVar;
    }

    public boolean isGlobal(){
        return this.isGlobalVar;
    }
}
