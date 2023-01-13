package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * For elements that read input (in HTML).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Input implements Renderable {
	String value="";
	
	// ---------- INSTANCE ----------
	public abstract String getName();
	
	@Nonnull
	@Override
	public String asText(final State st) {
		throw new SystemImplementationException("Textual output does not support Input elements");
	}
	
	// ----- Internal Instance -----
	private String getValue() {
		return value;
	}
	
	@Nonnull
	public Input setValue(final String value) {
		this.value=value;
		return this;
	}
	
}
