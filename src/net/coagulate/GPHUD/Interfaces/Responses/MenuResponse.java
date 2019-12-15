package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provide a series of elements in a Menu like layout...
 *
 * @author iain
 */
public class MenuResponse implements Response {

	@Nullable
	private String header;
	private final List<Renderable> menu = new ArrayList<>();

	public MenuResponse() {}

	public MenuResponse(@Nullable final String title) { header = title; }

	public void add(final Renderable r) { menu.add(r); }

	@Nonnull
	@Override
	public JSONObject asJSON(final State st) {
		throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<A menu response>";
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		final StringBuilder s = new StringBuilder();
		if (header != null && !header.isEmpty()) { s.append(new TextHeader(header).asHtml(st, rich)); }
		for (final Renderable r : menu) {
			if (s.length() > 0) {
				if (rich) { s.append("<br>"); } else { s.append(" | "); }
			}
			s.append(r.asHtml(st, rich));
		}
		return s.toString();
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
