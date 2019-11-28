package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSArrayIndexOutOfBoundsException extends GSInvalidExpressionException {
	private static final long serialVersionUID=1L;
	public GSArrayIndexOutOfBoundsException(String reason) {
		super(reason);
	}

	public GSArrayIndexOutOfBoundsException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Array Index Out Of Bounds Exception}: "+getLocalizedMessage(); }

}
