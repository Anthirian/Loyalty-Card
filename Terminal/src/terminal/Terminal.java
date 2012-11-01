/**
 * 
 */
package terminal;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * The terminal class provides an interface for reading out the loyalty card system
 * @author Geert Smelt
 * @author Robin Oostrum
 *
 */
public class Terminal extends JPanel implements ActionListener {

	/**
	 * Set some default values
	 */
	private static final long serialVersionUID = 1L;
	static final String TITLE = "Loyalty Card Terminal";
    static final Font FONT = new Font("Monospaced", Font.BOLD, 24);
    static final Dimension PREFERRED_SIZE = new Dimension(300, 300);
    
    static final int DISPLAY_WIDTH = 20;
    static final String MSG_ERROR = "    -- error --     ";
    static final String MSG_DISABLED = " -- insert card --  ";
    static final String MSG_INVALID = " -- invalid card -- ";
    
    /*
     * I have no idea how to know what our AID is. It seems to be auto-generated once the applet is uploaded to the card.
     * The AID is needed to verify the card holds the correct applet before attempting to connect to it.
     */
    static final byte[] CALC_APPLET_AID = { (byte) 0x3B, (byte) 0x29, (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x63, (byte) 0x01 };
    
    /*
     * The Command APDU is fixed. It always has the same prefix, followed by the AID of the applet.
     */
    static final CommandAPDU SELECT_APDU = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, CALC_APPLET_AID);

	/**
	 * @param parent
	 */
	public Terminal(JFrame parent) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param layout
	 */
	public Terminal(LayoutManager layout) {
		super(layout);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param isDoubleBuffered
	 */
	/*
	public Terminal(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		// TODO Auto-generated constructor stub
	}
	*/
	/**
	 * @param layout
	 * @param isDoubleBuffered
	 */
	/*
	public Terminal(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		// TODO Auto-generated constructor stub
	}
	*/

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JFrame frame = new JFrame(TITLE);
        Container c = frame.getContentPane();
		Terminal term = new Terminal(frame);

	}

}
