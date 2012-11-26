package officeterminal;

/**
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
 */
class BackOfficeException extends Exception {
	private static final long serialVersionUID = -5544225503197643465L;

	public BackOfficeException() {
		super();
	}

	public BackOfficeException(String message) {
		super(message);
	}

	public BackOfficeException(String message, Throwable cause) {
		super(message, cause);
	}

	public BackOfficeException(Throwable cause) {
		super(cause);
	}

	public String getMessage() {
		if (getCause() == null) {
			return super.getMessage();
		}
		return super.getMessage() + "\n" + getCause().toString() + "\n";
	}
}
