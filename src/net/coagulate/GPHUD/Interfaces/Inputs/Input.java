package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

/**
 * For elements that read input (in HTML).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Input implements Renderable {
	String value = "";

	public abstract String getName();

	private String getValue() { return value; }

	public Input setValue(String value) { this.value = value; return this; }

	@Override
	public String asText(State st) {
		throw new SystemException("Textual output does not support Input elements");
	}

}
