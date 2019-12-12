package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSUnknownIdentifier extends GSException {
	private static final long serialVersionUID=1L;
	public GSUnknownIdentifier(String reason) {
		super(reason);
	}

	public GSUnknownIdentifier(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Unknown Identifier Exception}: "+getLocalizedMessage(); }
}
