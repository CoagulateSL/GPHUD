package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.Core.Exceptions.User.UserInputStateException;

public class UserInventoryException extends UserInputStateException {

    private static final long serialVersionUID = 1L;

    public UserInventoryException(final String message) {
        super(message);
    }

    public UserInventoryException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UserInventoryException(final String reason, final Throwable cause, final boolean suppresslogging) {
        super(reason, cause, suppresslogging);
    }
}
