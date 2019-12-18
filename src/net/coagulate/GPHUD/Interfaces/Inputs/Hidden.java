package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Hidden input element.
 *
 * @author iain
 */
public class Hidden extends Input {

	final String name;

	public Hidden(final String name,
	              final String value)
	{
		this.name=name;
		this.value=value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich)
	{
		return "<input type=hidden name=\""+getName()+"\" value=\""+value+"\">";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

}
