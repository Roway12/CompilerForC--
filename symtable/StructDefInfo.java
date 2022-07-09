package symtable;

/**
 * The StructDefInfo class is a subclass of the Sym class just for the 
 * definition of a struct type. 
 * Each StructDefInfo contains a symbol table to hold information about its 
 * fields.
 */
public class StructDefInfo extends SymInfo {
    // new fields
    private SymTable symTab;
    private int Size = 0;
    public StructDefInfo(SymTable table) {
        super(new Type.StructDefType());
        symTab = table;
    }

    public SymTable getSymTable() {
        return symTab;
    }

    public void setSize(int size){ this.Size = size; }

    public int getSize(){ return this.Size; }
}
