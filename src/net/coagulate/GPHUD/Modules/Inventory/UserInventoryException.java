package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.Core.Exceptions.User.UserInputStateException;

public class UserInventoryException extends UserInputStateException {

    private static final long serialVersionUID = 1L;
    public UserInventoryException(final String reason) {
        super(reason);
    }

    public UserInventoryException(final String reason, final Throwable cause) {
        super(reason, cause);
    }

    public UserInventoryException(final String message, final Throwable exception, final boolean suppress) {
        super(message, exception, suppress);
    }
}
