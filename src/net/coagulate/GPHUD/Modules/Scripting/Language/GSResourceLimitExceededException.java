package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSResourceLimitExceededException extends GSException {
    private static final long serialVersionUID = 1L;

    public GSResourceLimitExceededException(final String message) {
        super(message);
    }

    public GSResourceLimitExceededException(final String message,
                                            final Throwable cause) {
        super(message, cause);
    }

    // ---------- INSTANCE ----------
    @Nonnull
    public String toString() {
        return "{GS Resource Limit Exceeded Exception}: " + getLocalizedMessage();
    }
}
