/**
 * 
 */
package terminal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import terminal.Terminal.CardThread;
import terminal.Terminal.CloseEventListener;

/**
 * The terminal class provides an interface for reading out the loyalty card system
 * @author Geert Smelt
 * @author Robin Oostrum
 *
 */
@SuppressWarnings("unused")
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
     * I have no idea how to know what our AID is. I have manually generated the AID.
     * The AID is needed to verify the card holds the correct applet before attempting to connect to it.
     */
    static final byte[] APPLET_AID = {(byte) 0x11, (byte) 0x86, (byte) 0x86, (byte) 0x81, (byte) 0x35};
    /*
     * The Command APDU is fixed. It always has the same prefix, followed by the AID of the applet.
     */
    static final CommandAPDU SELECT_APDU = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, APPLET_AID);

    CardChannel channel;
    JTextField display;
    JPanel keypad;
	
    /**
     * The constructor creates the GUI and starts the Card Thread for interaction with the card
	 * @param parent
	 */
	public Terminal(JFrame parent) {
		buildGUI(parent);
        setEnabled(false);
        (new CardThread()).start();
	}
	
	/**
	 * Copy and paste of the calculator applet, needs to be completely rewritten
	 * @param parent
	 */
	void buildGUI(JFrame parent) {
        setLayout(new BorderLayout());
        display = new JTextField(DISPLAY_WIDTH);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        display.setFont(FONT);
        display.setBackground(Color.darkGray);
        display.setForeground(Color.green);
        add(display, BorderLayout.NORTH);
        keypad = new JPanel(new GridLayout(5, 5));
        key(null);
        key(null);
        key(null);
        key(null);
        key("C");
        key("7");
        key("8");
        key("9");
        key(":");
        key("ST");
        key("4");
        key("5");
        key("6");
        key("x");
        key("RM");
        key("1");
        key("2");
        key("3");
        key("-");
        key("M+");
        key("0");
        key(null);
        key(null);
        key("+");
        key("=");
        add(keypad, BorderLayout.CENTER);
        parent.addWindowListener(new CloseEventListener());
    }
	
	/**
	 * Create a button with text on it
	 * @param txt The text to be put on the button
	 */
	void key(String txt) {
        if (txt == null) {
            keypad.add(new JLabel());
        } else {
            JButton button = new JButton(txt);
            button.addActionListener(this);
            keypad.add(button);
        }
    }

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO What to do when we notice an event?

	}
	/**
	 * Copy and paste of another applet's interface
	 * @author Geert Smelt
	 *
	 */
	class CloseEventListener extends WindowAdapter {
        public void windowClosing(WindowEvent we) {
            System.exit(0);
        }
    }
	
	/**
	 * Send a keystroke to the card
	 * @param ins
	 * @return the response from the card, as a ResponseAPDU
	 */
    public ResponseAPDU sendKey(byte ins) {
        CommandAPDU apdu = new CommandAPDU(0, ins, 0, 0, 5);
        try {
			return channel.transmit(apdu);
		} catch (CardException e) {
			return null;
		}
    }

    /**
     * Getter for the preferred window size
     */
    public Dimension getPreferredSize() {
        return PREFERRED_SIZE;
    }
	
    /**
     * The CardThread is used to set up a connection with the card and interact with it
     * @author Geert Smelt
     * @author Robin Oostrum
     */
	class CardThread extends Thread {
        public void run() {
        	// TODO Connect the terminal to the card
        }    
	}

	/**
	 * Creates a new window allowing the operations of the terminal
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame(TITLE);
        Container c = frame.getContentPane();
		Terminal term = new Terminal(frame);
		c.add(term);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
	}

}
