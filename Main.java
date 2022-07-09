import java.io.*;
import java_cup.runtime.*;
import lexer.*;
import ast.*;
import parser.*;

/**
 * Main program to do generate MIPS code from C-- language.
 *
 * The program reads a in-file, creates a scanner and a parser, and
 * calls the parser.  If the parse is successful, the program performs
 * name analysis, type-checking and code generation
 */

public class Main {
    public static void main(String[] args) throws IOException { // may be thrown by the scanner
        java.util.Scanner console = new java.util.Scanner(System.in);
        String stop = "stop";
        String filename = readFileName(console,stop);
        
        while ( ! filename.equals(stop) ) {
            FileReader inFile = null;
            try {
                inFile = new FileReader(filename);
            } catch (FileNotFoundException ex) {
                System.err.println("Error while reading " + filename);
                System.exit(-1);
            }

            PrintWriter outFile = null;
            String outfilepath = null;
            try {
            outfilepath = filename.substring(0,filename.lastIndexOf(".")) + ".asm";
                outFile = new PrintWriter(outfilepath);
            } catch (FileNotFoundException ex) {
                System.err.println("File " + outfilepath + " could not be opened for writing.");
                System.exit(-1);
            }
            try {
                processInputFile(inFile,outFile);
            }
            catch (SyntaxErrorException see) {
                System.out.println("syntax error: parsing aborted");
            }
            inFile.close();
            outFile.close();
            filename = readFileName(console,stop);
        }
    }

    private static void processInputFile(FileReader inFile, PrintWriter outFile) {
        CmmParser P = new CmmParser(new Yylex(inFile));
        Symbol root = null; // the parser will return a Symbol whose value
                            // field is the translation of the root nonterminal
                            // (i.e., of the nonterminal "program")
        try {
            root = P.parse(); // do the parse
            System.out.println ("program parsed correctly.");
        } catch (SyntaxErrorException see) {
            throw see;
        } catch (Exception ex){
            System.err.println("Exception occured during parse: " + ex);
            System.exit(-1);
        }
        AST.ProgramNode astRoot = (AST.ProgramNode) root.value;
        ErrMsg.reset(); // reset the control for analysis errors
        astRoot.nameAnalysis();  // perform name analysis
        astRoot.typeCheck();     // type checking
        astRoot.resolveOffset(); // offset resolution for local variables
        // astRoot.unparse(outFile, 0); // perform the unparsing
        if ( ErrMsg.hasFatalError() )
            System.err.println("Compilation aborted");
        else
            astRoot.codeGen(outFile); // perform the code generation
    }
    
    /**
     * To read a valid input file name or the keyword 'stop' from the user
     */
    private static String readFileName(java.util.Scanner input, String stop) {
        String filename = null;
        do {
            System.out.print("file name? ");
            filename = input.nextLine().trim();
            if ( ! filename.equals(stop) ) {
                File file = new File(filename);	
                if ( ! file.exists() ) {
                    System.out.println(filename + " not found");
                    filename = null;
                }
            }
        } while ( filename == null );
        return filename;
    }
}
