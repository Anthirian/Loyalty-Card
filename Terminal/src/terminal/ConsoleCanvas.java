package terminal;

/* A class that implements basic console-oriented input/output, for use with
   Console.java and ConsolePanel.java.  This class provides the basic character IO.
   Higher-leve fucntions (reading and writing numbers, booleans, etc) are provided
   in Console.java and ConolePanel.java.

   (This vesion of ConsoleCanvas is an udate of an earilier version, rewritten to
   be compliand with Java 1.1.  David Eck; July 17, 1998.)

   (Modified August 16, 1998 to add the MouseListener interface and
   a mousePressed method to ConsoleCanvas.  The mousePressed method requests
   the focus.  This is necessary for Sun's Java implementation -- though not,
   apparently for anyone else's.  Also added: an isFocusTraversable() method)
   
   Minor modifications, February 9, 2000, some glitches in the graphics.
*/

import java.awt.*;
import java.awt.event.*;

public class ConsoleCanvas extends Canvas implements FocusListener, KeyListener, MouseListener {

   // public interface, constructor and methods

	private static final long serialVersionUID = 1L;
	
	public ConsoleCanvas() {
      addFocusListener(this);
      addKeyListener(this);
   }

   public final String readLine() {  // wait for user to enter a line of input;
                                     // Line can only contain characters in the range
                                     // ' ' to '~'.
      return doReadLine();
   }

   public final void addChar(char ch) {  // output PRINTABLE character to console
      putChar(ch);
   }

   public final void addCR() {  // add a CR to the console
      putCR();
   }

   public synchronized void clear() {  // clear console and return cursor to row 0, column 0.
      if (OSC == null)
         return;
      currentRow = 0;
      currentCol = 0;
      OSCGraphics.setColor(Color.white);
      OSCGraphics.fillRect(4,4,getSize().width-8,getSize().height-8);
      OSCGraphics.setColor(Color.black);
      repaint();
      try { Thread.sleep(25); }
      catch (InterruptedException e) { }
   }
   
 
   // focus and key event handlers; not meant to be called excpet by system

   public void keyPressed(KeyEvent evt) {
      doKey(evt.getKeyChar());
   }
   
   public void keyReleased(KeyEvent evt) { }
   public void keyTyped(KeyEvent evt) { }

   public void focusGained(FocusEvent evt) {
      doFocus(true);
   }

   public void focusLost(FocusEvent evt) {
      doFocus(false);
   }
   
   public boolean isFocusTraversable() {
        // Allows the user to move the focus to the canvas
        // by pressing the tab key.
      return true;
   }

   // Mouse listener methods -- here just to make sure that the canvas
   // gets the focuse when the user clicks on it.  These are meant to
   // be called only by the system.

   public void mousePressed(MouseEvent evt) {
      requestFocus();
   }
   
   public void mouseReleased(MouseEvent evt) { }
   public void mouseClicked(MouseEvent evt) { }
   public void mouseEntered(MouseEvent evt) { }
   public void mouseExited(MouseEvent evt) { }


   // implementation section: protected variables and methods.

   protected StringBuffer typeAhead = new StringBuffer();
                 // Characters typed by user but not yet processed;
                 // User can "type ahead" the charcters typed until
                 // they are needed to satisfy a readLine.

   protected final int maxLineLength = 256;
                 // No lines longer than this are returned by readLine();
                 // The system effectively inserts a CR after 256 chars
                 // of input without a carriage return.

   protected int rows, columns;  // rows and columns of chars in the console
   protected int currentRow, currentCol;  // current curson position



   protected Font font;      // Font used in console (Courier); All font
                             //   data is set up in the doSetup() method.
   protected int lineHeight; // height of one line of text in the console
   protected int baseOffset; // distance from top of a line to its baseline
   protected int charWidth;  // width of a character (constant, since a monospaced font is used)
   protected int leading;    // space between lines
   protected int topOffset;  // distance from top of console to top of text
   protected int leftOffset; // distance from left of console to first char on line

   protected Image OSC;   // off-screen backup for console display (except cursor)
   protected Graphics OSCGraphics;  // graphics context for OSC

   protected boolean hasFocus = false;  // true if this canvas has the input focus
   protected boolean cursorIsVisible = false;  // true if cursor is currently visible


   private int pos = 0;  // exists only for sharing by next two methods
   public synchronized void clearTypeAhead() {
      // clears any unprocessed user typing.  This is meant only to
      // be called by ConsolePanel, when a program being run by
      // console Applet ends.  But just to play it safe, pos is
      // set to -1 as a signal to doReadLine that it should return.
      typeAhead.setLength(0);
      pos = -1;
      notify();
   }


   protected synchronized String doReadLine() {  // reads a line of input, up to next CR
      if (OSC == null) {  // If this routine is called before the console has
                          // completely opened, we shouldn't procede; give the
                          // window a chance to open, so that paint() can call doSetup().
         try { wait(5000); }  // notify() should be set by doSetup()
         catch (InterruptedException e) {}
      }
      if (OSC == null)  // If nothing has happened for 5 seconds, we are probably in
                        //    trouble, but when the heck, try calling doSetup and proceding anyway.
         doSetup();
      if (!hasFocus)  // Make sure canvas has input focus
         requestFocus();
      StringBuffer lineBuffer = new StringBuffer();  // buffer for constructing line from user
      pos = 0;
      while (true) {  // Read and process chars from the typeAhead buffer until a CR is found.
         while (pos >= typeAhead.length()) {  // If the typeAhead buffer is empty, wait for user to type something
            cursorBlink();
            try { wait(500); }
            catch (InterruptedException e) { }
         }
         if (pos == -1) // means clearTypeAhead was called;
            return "";  // this is an abnormal return that should not happen
         if (cursorIsVisible)
            cursorBlink();
         if (typeAhead.charAt(pos) == '\r' || typeAhead.charAt(pos) == '\n') {
            putCR();
            pos++;
            break;
         }
         if (typeAhead.charAt(pos) == 8 || typeAhead.charAt(pos) == 127) {
            if (lineBuffer.length() > 0) {
               lineBuffer.setLength(lineBuffer.length() - 1);
               eraseChar();
            }
            pos++;
         }
         else if (typeAhead.charAt(pos) >= ' ' && typeAhead.charAt(pos) < 127) {
            putChar(typeAhead.charAt(pos));
            lineBuffer.append(typeAhead.charAt(pos));
            pos++;
         }
         else
            pos++;
         if (lineBuffer.length() == maxLineLength) {
            putCR();
            pos = typeAhead.length();
            break;
         }
      }
      if (pos >= typeAhead.length())  // delete all processed chars from typeAhead
         typeAhead.setLength(0);
      else {
         int len = typeAhead.length();
         for (int i = pos; i < len; i++)
            typeAhead.setCharAt(i - pos, typeAhead.charAt(i));
         typeAhead.setLength(len - pos);
      }
      return lineBuffer.toString();   // return the string that was entered
   }

   protected synchronized void doKey(char ch) {  // process key pressed by user
      typeAhead.append(ch);
      notify();
   }

   private void putCursor(Graphics g) {  // draw the cursor
      g.drawLine(leftOffset + currentCol*charWidth + 1, topOffset + (currentRow*lineHeight),
                 leftOffset + currentCol*charWidth + 1, topOffset + (currentRow*lineHeight + baseOffset));
   }

   protected synchronized void putChar(char ch) { // draw ch at cursor position and move cursor
      if (OSC == null) {  // If this routine is called before the console has
                          // completely opened, we shouldn't procede; give the
                          // window a chance to open, so that paint() can call doSetup().
         try { wait(5000); }  // notify() should be set by doSetup()
         catch (InterruptedException e) {}
      }
      if (OSC == null)  // If nothing has happened for 5 seconds, we are probably in
                        //    trouble, but when the heck, try calling doSetup and proceding anyway.
         doSetup();
      if (currentCol >= columns)
         putCR();
      currentCol++;
      Graphics g = getGraphics();
      g.setColor(Color.black);
      g.setFont(font);
      char[] fudge = new char[1];
      fudge[0] = ch;
      g.drawChars(fudge, 0, 1, leftOffset + (currentCol-1)*charWidth, 
                              topOffset + currentRow*lineHeight + baseOffset); 
      g.dispose();
      OSCGraphics.drawChars(fudge, 0, 1, leftOffset + (currentCol-1)*charWidth, 
                              topOffset + currentRow*lineHeight + baseOffset);
   }

   protected void eraseChar() {  // erase char before cursor position and move cursor
      if (currentCol == 0 && currentRow == 0)
         return;
      currentCol--;
      if (currentCol < 0) {
         currentRow--;
         currentCol = columns - 1;
      }
      Graphics g = getGraphics();
      g.setColor(Color.white);
      g.fillRect(leftOffset + (currentCol*charWidth), topOffset + (currentRow*lineHeight),
                                  charWidth, lineHeight - 1);
      g.dispose();
      OSCGraphics.setColor(Color.white);
      OSCGraphics.fillRect(leftOffset + (currentCol*charWidth), topOffset + (currentRow*lineHeight),
                                  charWidth, lineHeight - 1);
      OSCGraphics.setColor(Color.black);
   }

   protected synchronized void putCR() {  // move cursor to start of next line, scrolling window if necessary
      if (OSC == null) {  // If this routine is called before the console has
                          // completely opened, we shouldn't procede; give the
                          // window a chance to open, so that paint() can call doSetup().
         try { wait(5000); }  // notify() should be set by doSetup()
         catch (InterruptedException e) {}
      }
      if (OSC == null)  // If nothing has happened for 5 seconds, we are probably in
                        //    trouble, but when the heck, try calling doSetup and proceding anyway.
         doSetup();
      currentCol = 0;
      currentRow++;
      if (currentRow < rows)
         return;
      OSCGraphics.copyArea(leftOffset, topOffset+lineHeight,
                             columns*charWidth, (rows-1)*lineHeight - leading ,0, -lineHeight);
      OSCGraphics.setColor(Color.white);
      OSCGraphics.fillRect(leftOffset,topOffset + (rows-1)*lineHeight, columns*charWidth, lineHeight - leading);
      OSCGraphics.setColor(Color.black);
      currentRow = rows - 1;
      Graphics g = getGraphics();
      paint(g);
      g.dispose();
      try { Thread.sleep(20); }
      catch (InterruptedException e) { }
   }

   protected void cursorBlink() {  // toggle visibility of cursor (but don't show it if focus has been lost)
      if (cursorIsVisible) {
         Graphics g = getGraphics();
         g.setColor(Color.white);
         putCursor(g);
         cursorIsVisible = false;
         g.dispose();
      }
      else if (hasFocus) {
         Graphics g = getGraphics();
         g.setColor(Color.black);
         putCursor(g);
         cursorIsVisible = true;
         g.dispose();
      }
   }

   protected synchronized void doFocus(boolean focus) {  // react to gain or loss of focus
      if (OSC == null)
         doSetup();      
      hasFocus = focus;
      if (hasFocus)    // the rest of the routine draws or erases border around canvas
         OSCGraphics.setColor(Color.cyan);
      else
         OSCGraphics.setColor(Color.white);
      int w = getSize().width;
      int h = getSize().height;
      for (int i = 0; i < 3; i++)
         OSCGraphics.drawRect(i,i,w-2*i,h-2*i);
      OSCGraphics.drawLine(0,h-3,w,h-3);
      OSCGraphics.drawLine(w-3,0,w-3,h);
      OSCGraphics.setColor(Color.black);
      repaint();
      try { Thread.sleep(50); }
      catch (InterruptedException e) { }
      notify();
   }

   protected void doSetup() {  // get font parameters and create OSC
      int w = getSize().width;
      int h = getSize().height;
      font = new Font("Courier",Font.PLAIN,getFont().getSize());
      FontMetrics fm = getFontMetrics(font);
      lineHeight = fm.getHeight();
      leading = fm.getLeading();
      baseOffset = fm.getAscent();
      charWidth = fm.charWidth('W');
      columns = (w - 12) / charWidth;
      rows = (h - 12 + leading) / lineHeight;
      leftOffset = (w - columns*charWidth) / 2;
      topOffset = (h + leading - rows*lineHeight) / 2;
      OSC = createImage(w,h);
      OSCGraphics = OSC.getGraphics();
      OSCGraphics.setFont(font);
      OSCGraphics.setColor(Color.white);
      OSCGraphics.fillRect(0,0,w,h);
      OSCGraphics.setColor(Color.black);
      notify();
   }

   public void update(Graphics g) {
      paint(g);
   }

   public synchronized void paint(Graphics g) {
      if (OSC == null)
         doSetup();
      g.drawImage(OSC,0,0,this);
   }


}