package terminal;
/*
    The file defines a class ConsolePanel.  Objects of type
    ConsolePanel can be used for simple input/output exchanges with
    the user.  Various routines are provided for reading and writing
    values of various types from the output.  (This class gives all
    the I/O behavior of another class, Console, that represents a
    separate window for doing console-style I/O.)

    This class is dependent on another class, ConsoleCanvas.

    Note that when the console has the input focus, it is outlined with
    a bright blue border.  If, in addition, the console is waiting for
    user input, then there will be a blinking cursor.  If the console
    is not outlined in light blue, the user has to click on it before
    any input will be accepted.

    This is an update an earlier version of the same class,
    rewritten to use realToString() for output of floating point
    numbers..

    Written by:  David Eck
                 Department of Mathematics and Computer Science
                 Hobart and William Smith Colleges
                 Geneva, NY 14456
                 Email:  eck@hws.edu
                 WWW:  http://math.hws.edu/eck/

    July 17, 1998.

*/

   
import java.awt.*;

public class ConsolePanel extends Panel {

   // ***************************** Constructors *******************************
	private static final long serialVersionUID = 1L;


	public ConsolePanel() {   // default constructor just provides default window title and size
      setBackground(Color.white);
      setLayout(new BorderLayout(0,0));
      canvas = new ConsoleCanvas();
      add("Center",canvas);
   }
   
      
   public void clear() {  // clear all characters from the canvas
      canvas.clear();
   }

      
   // *************************** I/O Methods *********************************
   
         // Methods for writing the primitive types, plus type String,
         // to the console window, with no extra spaces.
         //
         // Note that the real-number data types, float
         // and double, a rounded version is output that will
         // use at most 10 or 11 characters.  If you want to
         // output a real number with full accuracy, use
         // "con.put(String.valueOf(x))", for example.

   public void put(int x)     { put(x,0); }   // Note: also handles byte and short!
   public void put(long x)    { put(x,0); }
   public void put(double x)  { put(x,0); }   // Also handles float.
   public void put(char x)    { put(x,0); }
   public void put(boolean x) { put(x,0); }
   public void put(String x)  { put(x,0); }


         // Methods for writing the primitive types, plus type String,
         // to the console window,followed by a carriage return, with
         // no extra spaces.

   public void putln(int x)      { put(x,0); newLine(); }   // Note: also handles byte and short!
   public void putln(long x)     { put(x,0); newLine(); }
   public void putln(double x)   { put(x,0); newLine(); }   // Also handles float.
   public void putln(char x)     { put(x,0); newLine(); }
   public void putln(boolean x)  { put(x,0); newLine(); }
   public void putln(String x)   { put(x,0); newLine(); }
  

         // Methods for writing the primitive types, plus type String,
         // to the console window, with a minimum field width of w,
         // and followed by a carriage  return.
         // If outut value is less than w characters, it is padded
         // with extra spaces in front of the value.

   public void putln(int x, int w)     { put(x,w); newLine(); }   // Note: also handles byte and short!
   public void putln(long x, int w)    { put(x,w); newLine(); }
   public void putln(double x, int w)  { put(x,w); newLine(); }   // Also handles float.
   public void putln(char x, int w)    { put(x,w); newLine(); }
   public void putln(boolean x, int w) { put(x,w); newLine(); }
   public void putln(String x, int w)  { put(x,w); newLine(); }


          // Method for outputting a carriage return

   public void putln() { newLine(); }
   

         // Methods for writing the primitive types, plus type String,
         // to the console window, with minimum field width w.
   
   public void put(int x, int w)     { dumpString(String.valueOf(x), w); }   // Note: also handles byte and short!
   public void put(long x, int w)    { dumpString(String.valueOf(x), w); }
   public void put(double x, int w)  { dumpString(realToString(x), w); }   // Also handles float.
   public void put(char x, int w)    { dumpString(String.valueOf(x), w); }
   public void put(boolean x, int w) { dumpString(String.valueOf(x), w); }
   public void put(String x, int w)  { dumpString(x, w); }
   
   
         // Methods for reading in the primitive types, plus "words" and "lines".
         // The "getln..." methods discard any extra input, up to and including
         //    the next carriage return.
         // A "word" read by getlnWord() is any sequence of non-blank characters.
         // A "line" read by getlnString() or getln() is everything up to next CR;
         //    the carriage return is not part of the returned value, but it is
         //    read and discarded.
         // Note that all input methods except getAnyChar(), peek(), the ones for lines
         //    skip past any blanks and carriage returns to find a non-blank value.
         // getln() can return an empty string; getChar() and getlnChar() can 
         //    return a space or a linefeed ('\n') character.
         // peek() allows you to look at the next character in input, without
         //    removing it from the input stream.  (Note that using this
         //    routine might force the user to enter a line, in order to
         //    check what the next character.)
         // Acceptable boolean values are the "words": true, false, t, f, yes,
         //    no, y, n, 0, or 1;  uppercase letters are OK.
         // None of these can produce an error; if an error is found in input,
         //    the user is forced to re-enter.
         // Available input routines are:
         //
         //            getByte()      getlnByte()    getShort()     getlnShort()
         //            getInt()       getlnInt()     getLong()      getlnLong()
         //            getFloat()     getlnFloat()   getDouble()    getlnDouble()
         //            getChar()      getlnChar()    peek()         getAnyChar()
         //            getWord()      getlnWord()    getln()        getString()    getlnString()
         //
         // (getlnString is the same as getln and is onlyprovided for consistency.)
   
   public byte getlnByte()       { byte x=getByte();       emptyBuffer();  return x; }
   public short getlnShort()     { short x=getShort();     emptyBuffer();  return x; }
   public int getlnInt()         { int x=getInt();         emptyBuffer();  return x; }
   public long getlnLong()       { long x=getLong();       emptyBuffer();  return x; }
   public float getlnFloat()     { float x=getFloat();     emptyBuffer();  return x; }
   public double getlnDouble()   { double x=getDouble();   emptyBuffer();  return x; }
   public char getlnChar()       { char x=getChar();       emptyBuffer();  return x; }
   public boolean getlnBoolean() { boolean x=getBoolean(); emptyBuffer();  return x; }
   public String getlnWord()     { String x=getWord();     emptyBuffer();  return x; }
   public String getlnString()   { return getln(); }  // same as getln()
   public String getln() {
      StringBuffer s = new StringBuffer(100);
      char ch = readChar();
      while (ch != '\n') {
         s.append(ch);
         ch = readChar();
      }
      return s.toString();
   }
   
   
   public byte getByte()   { return (byte)readInteger(-128L,127L); }
   public short getShort() { return (short)readInteger(-32768L,32767L); }   
   public int getInt()     { return (int)readInteger((long)Integer.MIN_VALUE, (long)Integer.MAX_VALUE); }
   public long getLong()   { return readInteger(Long.MIN_VALUE, Long.MAX_VALUE); }
   
   public char getAnyChar(){ return readChar(); }
   public char peek()      { return lookChar(); }
   
   public char getChar() {  // skip spaces & cr's, then return next char
      char ch = lookChar();
      while (ch == ' ' || ch == '\n') {
         readChar();
         if (ch == '\n')
            dumpString("? ",0);
         ch = lookChar();
      }
      return readChar();
   }

   public float getFloat() {  // can return positive or negative infinity
      float x = 0.0F;
      while (true) {
         String str = readRealString();
         if (str.equals("")) {
             errorMessage("Illegal floating point input.",
                          "Real number in the range " + Float.MIN_VALUE + " to " + Float.MAX_VALUE);
         }
         else {
            Float f = null;
            try { f = Float.valueOf(str); }
            catch (NumberFormatException e) {
               errorMessage("Illegal floating point input.",
                            "Real number in the range " + Float.MIN_VALUE + " to " + Float.MAX_VALUE);
               continue;
            }
            if (f.isInfinite()) {
               errorMessage("Floating point input outside of legal range.",
                            "Real number in the range " + Float.MIN_VALUE + " to " + Float.MAX_VALUE);
               continue;
            }
            x = f.floatValue();
            break;
         }
      }
      return x;
   }
   
   public double getDouble() {
      double x = 0.0;
      while (true) {
         String str = readRealString();
         if (str.equals("")) {
             errorMessage("Illegal floating point input",
                          "Real number in the range " + Double.MIN_VALUE + " to " + Double.MAX_VALUE);
         }
         else {
            Double f = null;
            try { f = Double.valueOf(str); }
            catch (NumberFormatException e) {
               errorMessage("Illegal floating point input",
                            "Real number in the range " + Double.MIN_VALUE + " to " + Double.MAX_VALUE);
               continue;
            }
            if (f.isInfinite()) {
               errorMessage("Floating point input outside of legal range.",
                            "Real number in the range " + Double.MIN_VALUE + " to " + Double.MAX_VALUE);
               continue;
            }
            x = f.doubleValue();
            break;
         }
      }
      return x;
   }
   
   public String getWord() {
      char ch = lookChar();
      while (ch == ' ' || ch == '\n') {
         readChar();
         if (ch == '\n')
            dumpString("? ",0);
         ch = lookChar();
      }
      StringBuffer str = new StringBuffer(50);
      while (ch != ' ' && ch != '\n') {
         str.append(readChar());
         ch = lookChar();
      }
      return str.toString();
   }
   
   public boolean getBoolean() {
      boolean ans = false;
      while (true) {
         String s = getWord();
         if ( s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t") ||
                 s.equalsIgnoreCase("yes")  || s.equalsIgnoreCase("y") ||
                 s.equals("1") ) {
              ans = true;
              break;
          }
          else if ( s.equalsIgnoreCase("false") || s.equalsIgnoreCase("f") ||
                 s.equalsIgnoreCase("no")  || s.equalsIgnoreCase("n") ||
                 s.equals("0") ) {
              ans = false;
              break;
          }
          else
             errorMessage("Illegal boolean input value.",
                          "one of:  true, false, t, f, yes, no, y, n, 0, or 1");
      }
      return ans;
   }
   
   // ***************** Everything beyond this point is private *******************
   
   // ********************** Utility routines for input/output ********************

   private ConsoleCanvas canvas;  // the canvas where I/O is displayed
   private String buffer = null;  // one line read from input
   private int pos = 0;           // position next char in input line that has
                                  //      not yet been processed

   
   private String readRealString() {   // read chars from input following syntax of real numbers
      StringBuffer s=new StringBuffer(50);
      char ch=lookChar();
      while (ch == ' ' || ch == '\n') {
          readChar();
          if (ch == '\n')
             dumpString("? ",0);
          ch = lookChar();
      }
      if (ch == '-' || ch == '+') {
          s.append(readChar());
          ch = lookChar();
          while (ch == ' ') {
             readChar();
             ch = lookChar();
          }
      }
      while (ch >= '0' && ch <= '9') {
          s.append(readChar());
          ch = lookChar();
      }
      if (ch == '.') {
         s.append(readChar());
         ch = lookChar();
         while (ch >= '0' && ch <= '9') {
             s.append(readChar());
             ch = lookChar();
         }
      }
      if (ch == 'E' || ch == 'e') {
         s.append(readChar());
         ch = lookChar();
         if (ch == '-' || ch == '+') {
             s.append(readChar());
             ch = lookChar();
         }
         while (ch >= '0' && ch <= '9') {
             s.append(readChar());
             ch = lookChar();
         }
      }
      return s.toString();
   }

   private long readInteger(long min, long max) {  // read long integer, limited to specified range
      long x=0;
      while (true) {
         StringBuffer s=new StringBuffer(34);
         char ch=lookChar();
         while (ch == ' ' || ch == '\n') {
             readChar();
             if (ch == '\n')
                dumpString("? ",0);
             ch = lookChar();
         }
         if (ch == '-' || ch == '+') {
             s.append(readChar());
             ch = lookChar();
             while (ch == ' ') {
                readChar();
                ch = lookChar();
             }
         }
         while (ch >= '0' && ch <= '9') {
             s.append(readChar());
             ch = lookChar();
         }
         if (s.equals("")){
             errorMessage("Illegal integer input.",
                          "Integer in the range " + min + " to " + max);
         }
         else {
             String str = s.toString();
             try { 
                x = Long.parseLong(str);
             }
             catch (NumberFormatException e) {
                errorMessage("Illegal integer input.",
                             "Integer in the range " + min + " to " + max);
                continue;
             }
             if (x < min || x > max) {
                errorMessage("Integer input outside of legal range.",
                             "Integer in the range " + min + " to " + max);
                continue;
             }
             break;
         }
      }
      return x;
   }
   
   private static String realToString(double x) {
         // Goal is to get a reasonable representation of x in at most
         // 10 characters, or 11 characters if x is negative.
      if (Double.isNaN(x))
         return "undefined";
      if (Double.isInfinite(x))
         if (x < 0)
            return "-INF";
         else
            return "INF";
      if (Math.abs(x) <= 5000000000.0 && Math.rint(x) == x)
         return String.valueOf( (long)x );
      String s = String.valueOf(x);
      if (s.length() <= 10)
         return s;
      boolean neg = false;
      if (x < 0) {
         neg = true;
         x = -x;
         s = String.valueOf(x);
      }
      if (x >= 0.00005 && x <= 50000000 && (s.indexOf('E') == -1 && s.indexOf('e') == -1)) {  // trim x to 10 chars max
         s = round(s,10);
         s = trimZeros(s);
      }
      else if (x > 1) { // construct exponential form with positive exponent
          long power = (long)Math.floor(Math.log(x)/Math.log(10));
          String exp = "E" + power;
          int numlength = 10 - exp.length();
          x = x / Math.pow(10,power);
          s = String.valueOf(x);
          s = round(s,numlength);
          s = trimZeros(s);
          s += exp;
      }
      else { // constuct exponential form
          long power = (long)Math.ceil(-Math.log(x)/Math.log(10));
          String exp = "E-" + power;
          int numlength = 10 - exp.length();
          x = x * Math.pow(10,power);
          s = String.valueOf(x);
          s = round(s,numlength);
          s = trimZeros(s);
          s += exp;
      }
      if (neg)
         return "-" + s;
      else
         return s;
   }
   
   private static String trimZeros(String num) {  // used by realToString
     if (num.indexOf('.') >= 0 && num.charAt(num.length() - 1) == '0') {
        int i = num.length() - 1;
        while (num.charAt(i) == '0')
           i--;
        if (num.charAt(i) == '.')
           num = num.substring(0,i);
        else
           num = num.substring(0,i+1);
     }
     return num;
   }
   
   private static String round(String num, int length) {  // used by realToString
      if (num.indexOf('.') < 0)
         return num;
      if (num.length() <= length)
         return num;
      if (num.charAt(length) >= '5' && num.charAt(length) != '.') {
         char[] temp = new char[length+1];
         int ct = length;
         boolean rounding = true;
         for (int i = length-1; i >= 0; i--) {
            temp[ct] = num.charAt(i); 
            if (rounding && temp[ct] != '.') {
               if (temp[ct] < '9') {
                  temp[ct]++;
                  rounding = false;
               }
               else
                  temp[ct] = '0';
            }
            ct--;
         }
         if (rounding) {
            temp[ct] = '1';
            ct--;
         }
         // ct is -1 or 0
         return new String(temp,ct+1,length-ct);
      }
      else 
         return num.substring(0,length);
      
   }

   private void dumpString(String str, int w) {   // output string to console
      for (int i=str.length(); i<w; i++)
         canvas.addChar(' ');
      for (int i=0; i<str.length(); i++)
         if ((int)str.charAt(i) >= 0x20 && (int)str.charAt(i) != 0x7F)  // no control chars or delete
            canvas.addChar(str.charAt(i));
         else if (str.charAt(i) == '\n' || str.charAt(i) == '\r')
            newLine();
   }
   
   private void errorMessage(String message, String expecting) {
                  // inform user of error and force user to re-enter.
       newLine();
       dumpString("  *** Error in input: " + message + "\n", 0);
       dumpString("  *** Expecting: " + expecting + "\n", 0);
       dumpString("  *** Discarding Input: ", 0);
       if (lookChar() == '\n')
          dumpString("(end-of-line)\n\n",0);
       else {
          while (lookChar() != '\n')
             canvas.addChar(readChar());
          dumpString("\n\n",0);
       }
       dumpString("Please re-enter: ", 0);
       readChar();  // discard the end-of-line character
   }

   private char lookChar() {  // return next character from input
      if (buffer == null || pos > buffer.length())
         fillBuffer();
      if (pos == buffer.length())
         return '\n';
      return buffer.charAt(pos);
   }

   private char readChar() {  // return and discard next character from input
      char ch = lookChar();
      pos++;
      return ch;
   }

   private void newLine() {   // output a CR to console
      canvas.addCR();
   }

   private void fillBuffer() {    // Wait for user to type a line and press return,
                                  // and put the typed line into the buffer.
      buffer = canvas.readLine();
      pos = 0;
   }

   private void emptyBuffer() {   // discard the rest of the current line of input
      buffer = null;
   }

   public void clearBuffers() {   // I expect this will only be called by
                                  // CanvasApplet when a program ends.  It should
                                  // not be called in the middle of a running program.
      buffer = null;
      canvas.clearTypeAhead();
   }
   
   
} // end of class Console