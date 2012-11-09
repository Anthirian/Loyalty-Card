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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

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

import rsa.RSAKeyGen;
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

	// Set some default and/or final values.
	private static final long serialVersionUID = 1L;
	
	static final String TITLE = "Loyalty Card Terminal";
    static final Font FONT = new Font("Monospaced", Font.BOLD, 24);
    static final Dimension PREFERRED_SIZE = new Dimension(750, 750);
    static final int DISPLAY_WIDTH = 20;
    
    static final String MSG_DISABLED = "-- Please insert card --";
    
    private static final byte CLA_CRYPTO = (byte) 0xCC;
	private static final byte INS_SET_PUB_MODULUS = (byte) 0x02;
	private static final byte INS_SET_PRIV_MODULUS = (byte) 0x12;
	private static final byte INS_SET_PRIV_EXP = (byte) 0x22;
	private static final byte INS_SET_PUB_EXP = (byte) 0x32;
	private static final byte INS_ISSUE = (byte) 0x40;
	private static final byte INS_ENCRYPT = (byte) 0xE0;
	private static final byte INS_DECRYPT = (byte) 0xD0;
    
    /*
     * I have no idea how to know what our AID is. I have manually generated the AID.
     * The AID is needed to verify the card holds the correct applet before attempting to connect to it.
     */
    static final byte[] APPLET_AID = {(byte) 0x11, (byte) 0x86, (byte) 0x86, (byte) 0x81, (byte) 0x35, (byte) 0x24};
    // The SELECT APDU is fixed. It always has the same prefix, followed by the AID of the applet.
    static final CommandAPDU SELECT_APDU = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, APPLET_AID);
    static final byte[] empty_data = {};
    static final CommandAPDU CONSULT_BALANCE_APDU = new CommandAPDU((byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, empty_data);
    // TODO create a real APDU for consulting balance

    CardChannel channel;
    JTextField display;
    Display scherm;
    JPanel keypad, numpad;
    
    /** Public Key of the Terminal. Used for encryption. */
    RSAPublicKey pkT;
    /** Secret Key of the Card. Used for decryption. */
    RSAPrivateKey skT;
    
    /**
     * The constructor creates the GUI and starts the Card Thread for interaction with the card
	 * @param parent
	 */
	public Terminal(JFrame parent) {
		createGUI(parent);
        setEnabled(false);
        generateKeypair();
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
	
	/**
	 * Finalizes the initialization of the loyalty card.
	 * 
	 * @throws CardException
	 *             if something goes wrong.
	 */
	void issue() throws CardException {
		try {
			CommandAPDU capdu = new CommandAPDU(CLA_CRYPTO, INS_ISSUE,
					(byte) 0, (byte) 0);
			sendCommandAPDU(capdu);
		} catch (Exception e) {
			throw new CardException(e.getMessage());
		}
	}

	ResponseAPDU sendCommandAPDU(CommandAPDU capdu)	throws CardException {
		// TODO Add some logging of the APDUs sent and received
		ResponseAPDU rapdu = channel.transmit(capdu);		
		return rapdu;
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
		}
	}
	
	/**
	 * Generate a keypair to use for mutual authentication between the card and terminal.
	 */
	public void generateKeypair() {
		// TODO Check if this can be changed to the way the Card handles crypto instead. 
		// It is not useful to use two different algorithms to do the same thing twice. 
		RSAKeyGen kg = new RSAKeyGen();
		pkT = kg.getPublicKey();
		skT = kg.getPrivateKey();
	}
	
	/**
	 * Send a keystroke to the card
	 * @param ins The instruction byte to send
	 * @return the response from the card, as a ResponseAPDU
	 */
    public ResponseAPDU sendKey(byte ins) {
    	// I don't know why Le is hardcoded to 5
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
    	//char[] characters = text.toCharArray();
		//g.drawChars(characters, 0, characters.length, 0, 0);
		//scherm.repaint();
    	
    	System.out.println(text);
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
			char[] data = MSG_DISABLED.toCharArray();
			g.setColor(new Color(150, 200, 0));
			int windowHeight = (int) getPreferredSize().getHeight();
			int windowWidth = (int) getPreferredSize().getWidth();
			int rectHeight = 150;
			int rectWidth = 300;
			int vOffset = ((windowHeight - rectHeight) / 2);
			int hOffset = ((windowWidth - rectWidth) / 2);
			g.fillRoundRect(hOffset, vOffset, rectWidth, rectHeight, 20, 20);
			
			g.setColor(new Color(0, 10, 0));
			g.drawChars(data, 0, data.length, hOffset + 10 + rectWidth / 4, vOffset + 10 + rectHeight / 2);
			// The window title is 10 pixels high
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
        	try {
            	connect();
        	} catch (InterruptedException ie) {
        		System.out.println("Sleep operation was interrupted during connecting: " + ie.getMessage());
        	} catch (Exception e) {
                setEnabled(false);
                System.out.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }

		/**
		 * Attempts to connect to any card reader that has a card present.
		 * @throws InterruptedException If a connection attempt fails and the intermittent sleep time has been interrupted.
		 */
		private void connect() throws InterruptedException {
			TerminalFactory tf = TerminalFactory.getDefault();
			CardTerminals ct = tf.terminals();
			List<CardTerminal> cs;
			
			while (true) {
				try {
					cs = ct.list(CardTerminals.State.CARD_PRESENT);
			    	if (cs.isEmpty()) {
			    		System.err.println("None of the terminals have cards present");
			    		sleep(2000);
			    		continue;
			    	}
					for(CardTerminal c : cs) {
						if (c.isCardPresent()) {
							try {
								// Try connecting using any protocol available (*)
								Card card = c.connect("*");
								try {
									channel = card.getBasicChannel();
									
									ResponseAPDU resp = channel.transmit(SELECT_APDU);
									int status = resp.getSW();
									
									if (status == 0x6999) {
										throw new Exception("Applet selection failed");
									} else if (status == 0x9000) {
										displayMessage("Connection established!");
										setEnabled(true);
									}
			                        
			                        // TODO Do the actual work here
			                        
			                        
			                        // Wait for the card to be removed
			                        while (c.isCardPresent());
			                        
			                        setEnabled(false);
			                        break;
			                        
								} catch (CardException ce) {
									System.err.println("The operation failed: " + ce.getMessage());
								} catch (IllegalStateException ise) {
									System.err.println("Channel has been closed or the corresponding Card has been disconnected: " + ise.getMessage());
								} catch (IllegalArgumentException iae) {
									System.err.println("The APDU encodes a MANAGE CHANNEL command: " + iae.getMessage());
								} catch (NullPointerException npe) {
									System.err.println("Command is null: " + npe.getMessage());
								} catch (Exception e) {
									System.err.println(e.getMessage());
									sleep(2000);
									continue;
								}
							} catch (CardException ce) {
								System.err.println("Couldn't connect to card: " + ce.getMessage());
								sleep(2000);    	    						
								continue;
							}
						} else {
							System.err.println("No card present in reader " + c.getName());
							sleep(2000);
							continue;
						}
					}
				} catch (CardException ce) {
					System.err.println("Card status problem: " + ce.getMessage());
				}
			}
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
