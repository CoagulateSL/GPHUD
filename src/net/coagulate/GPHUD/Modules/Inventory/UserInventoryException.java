package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.Core.Exceptions.User.UserInputStateException;

public class UserInventoryException extends UserInputStateException {

    private static final long serialVersionUID = 1L;
    public UserInventoryException(String reason) {
        super(reason);
    }

    public UserInventoryException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public UserInventoryException(String message, Throwable exception, boolean suppress) {
        super(message, exception, suppress);
    }
}
