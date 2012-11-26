package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Static functions to make command line interaction easier.
 * 
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
 */
public final class CLI {
	private static BufferedReader in = new BufferedReader(
			new InputStreamReader(System.in));

	public static String prompt(String prompt) {
		String input = "";
		try {
			System.out.print(prompt);
			input = in.readLine();
		} catch (IOException e) {
			System.err.println("IOException during prompt.");
			e.printStackTrace();
			input = "";
		}
		return input;
	}

	public static int promptInt(String prompt) {
		while (true) {
			String correct = "";
			String id = CLI.prompt(prompt);

			CLI.showln("ID is " + id + ".");
			while (!correct.equals("N") && !correct.equals("Y")
					&& !correct.equals("C")) {
				correct = CLI.prompt("Is this correct? (Y)es/(N)o/(C)ancel: ");
			}

			if (correct.equals("C")) {
				return -1;
			} else if (correct.equals("Y")) {
				try {
					return Integer.parseInt(id);
				} catch (NumberFormatException e) {
					CLI.showln("This is not a valid id number.");
				}
			}
		}
	}
	
	public static int checkInt(int input) {
		while (true) {
			String correct = "";
			
			//CLI.showln("ID is " + input + ".");
			while (!correct.equals("N") && !correct.equals("Y")
					&& !correct.equals("C")) {
				correct = CLI.prompt("Is this correct? (Y)es/(N)o/(C)ancel: ");
			}

			if (correct.equals("C")) {
				return -1;
			} else if (correct.equals("Y")) {
				try {
					return input;
				} catch (NumberFormatException e) {
					CLI.showln("This is not a valid id number.");
				}
			}
		}
	}

	public static void show(String message) {
		System.out.print(message);
	}

	public static void showln(String message) {
		System.out.println(message);
	}

}
