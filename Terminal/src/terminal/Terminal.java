/**
 * 
 */
package terminal;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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
import javax.swing.JTextArea;
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
    static final Dimension PREFERRED_SIZE = new Dimension(750, 750);
    
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
    static final byte[] empty_data = {};
    static final CommandAPDU CONSULT_BALANCE_APDU = new CommandAPDU((byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, empty_data);
    // TODO create a real APDU for consulting balance

    CardChannel channel;
    JTextField display;
    Display scherm;
    JPanel keypad, numpad;
    
    /**
     * The constructor creates the GUI and starts the Card Thread for interaction with the card
	 * @param parent
	 */
	public Terminal(JFrame parent) {
		createGUI(parent);
        setEnabled(false);
        (new CardThread()).start();
	}
	
	/**
	 * Copy and paste of the calculator applet, needs to be completely rewritten
	 * At the moment this servers only for quick reference and is not used anymore
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
        key(null); key(null); key(null); key(null); key("C"); 
        key("7");  key("8");  key("9");  key(":");  key("ST");
        key("4");  key("5");  key("6");  key("x");  key("RM");
        key("1");  key("2");  key("3");  key("-");  key("M+");
        key("0");  key(null); key(null); key("+");  key("=");
        add(keypad, BorderLayout.CENTER);
        parent.addWindowListener(new CloseEventListener());
    }
	
	/**
	 * First attempt at creating the terminal's GUI
	 * Currently features only a Display and a Keypad
	 * @param parent
	 */
	private void createGUI(JFrame parent) {
		setLayout(new BorderLayout());
		add(createCanvas(), BorderLayout.NORTH);
		add(createKeypad(), BorderLayout.CENTER);
		parent.addWindowListener(new CloseEventListener());
	}
	
	/**
	 * Create a Display to use for notifying the user of new information
	 * @return a Display that can be added to the GUI
	 */
	private Display createCanvas() {
		scherm = new Display();
		scherm.setBackground(new Color(100, 100, 155));
		scherm.setSize(375,375);
		return scherm;
	}
	
	/**
	 * Create a keypad to use in verifying the user's PIN
	 * @return a JPanel that can be added to the GUI
	 */
	private JPanel createKeypad() {
		numpad = new JPanel(new GridLayout(4, 3));
		key("7");  key("8"); key("9");
		key("4");  key("5"); key("6");
		key("1");  key("2"); key("3");
		key(null); key("0"); key(null);
		return numpad;
	}
	
	/**
	 * Create a button with text on it
	 * @param txt The text to be put on the button
	 */
	void key(String txt) {
        if (txt == null) {
            numpad.add(new JLabel());
        } else {
            JButton button = new JButton(txt);
            button.addActionListener(this);
            numpad.add(button);
        }
    }

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		// TODO What to do when we notice an event?
		Object source = ae.getSource();
		if (source instanceof JButton) {
			System.out.printf("\nYou pressed button %s!", ((JButton) source).getText());
			/* 
			 * This doesn't seem to work yet. 
			 * Ideally we would like to print the text 
			 * above in the Display instead of in the Console
			 */
			scherm.repaint();
		}
	}
	
	/**
	 * Send a keystroke to the card
	 * @param ins the key that was pressed
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
    
    //***********************//
    //  Getters and setters  //
    //***********************//
    
    /**
     * Getter for the preferred window size
     */
    public Dimension getPreferredSize() {
        return PREFERRED_SIZE;
    }
    
    /**
     * Displays any text on the display of the terminal
     * @param text The text that should be displayed on screen, as a String
     */
    private void displayMessage(String text) {
    	// TODO This has yet to be made compatible with the new Display canvas
    	char[] characters = text.toCharArray();
		//g.drawChars(characters, 0, characters.length, 0, 0);
		scherm.repaint();
    }    
    
    //********************************************//
    // Subclasses to be used by the terminal only //
    //********************************************//
    
    /**
	 * A Canvas that is intended to function as a display for important information
	 * @author Geert Smelt
	 * @author Robin Oostrum
	 */
	public class Display extends Canvas {
		private static final long serialVersionUID = 1L;
		
		public void paint(Graphics g) {
			// TODO What to do when the display is (re)painted?
		}
	}
    
    /**
	 * A simple event listener class to add to the Terminal's GUI
	 * @author Geert Smelt
	 * @author Robin Oostrum
	 */
	class CloseEventListener extends WindowAdapter {
		/**
		 * Override the default window close event to perform a clean exit
		 */
        public void windowClosing(WindowEvent we) {
            System.exit(0);
        }
        // TODO Add more event listeners?
    }
    
    /**
     * The CardThread class is used to set up a connection with the card and interact with it
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
