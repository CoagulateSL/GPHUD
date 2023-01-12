package net.coagulate.GPHUD.Interfaces.Inputs;

import javax.annotation.Nonnull;

/**
 * Implements a masked text input box.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PasswordInput extends TextInput {
	
	public PasswordInput(final String name) {
		super(name);
	}
	
	public PasswordInput(final String name,final String value) {
		super(name,value);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getType() {
		return "password";
	}
	
}
