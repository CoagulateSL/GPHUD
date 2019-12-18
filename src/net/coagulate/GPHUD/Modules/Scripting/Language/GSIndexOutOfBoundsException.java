package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSIndexOutOfBoundsException extends GSInvalidExpressionException {
	private static final long serialVersionUID=1L;

	public GSIndexOutOfBoundsException(final String reason) {
		super(reason);
	}

	public GSIndexOutOfBoundsException(final String reason,
	                                   final Throwable cause)
	{
		super(reason,cause);
	}
}
