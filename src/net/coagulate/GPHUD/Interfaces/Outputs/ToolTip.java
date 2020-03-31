package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Iain Price
 */
public class ToolTip implements Renderable {

	private final String element;
	private final Renderable tooltip;

	public ToolTip(final String element,
	               final String tooltip) {
		this.element=element;
		this.tooltip=new Text(tooltip);
	}

	public ToolTip(final String element,
	               final Renderable tooltip) {
		this.element=element;
		this.tooltip=tooltip;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String asText(final State st) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		return "<div class=\"tooltip\">"+element+"<span class=\"tooltiptext\">"+tooltip.asHtml(st,rich)+"</span></div>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return new HashSet<>();
	}

}
