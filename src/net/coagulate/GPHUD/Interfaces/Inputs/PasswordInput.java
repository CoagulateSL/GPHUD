package net.coagulate.GPHUD.Interfaces.Inputs;

import javax.annotation.Nonnull;

/**
 * Implements a masked text input box.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PasswordInput extends TextInput {

	public PasswordInput(String name) {
		super(name);
	}

	public PasswordInput(String name, String value) { super(name, value); }

	@Nonnull
	@Override
	public String getType() { return "password"; }

}
