package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSIndexOutOfBoundsException extends GSInvalidExpressionException {
    private static final long serialVersionUID = 1L;

    public GSIndexOutOfBoundsException(final String message) {
        super(message);
    }

    public GSIndexOutOfBoundsException(final String message,
                                       final Throwable cause) {
        super(message, cause);
    }
}
