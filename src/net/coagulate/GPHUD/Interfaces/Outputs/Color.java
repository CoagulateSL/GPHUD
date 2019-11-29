package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.HashSet;
import java.util.Set;

/**
 * @author iain
 */
public class Color implements Renderable {
	final Renderable content;
	final String color;

	public Color(String color, Renderable content) {
		this.content = content;
		this.color = color;
	}

	public Color(String color, String content) {
		this.content = new Text(content);
		this.color = color;
	}

	@Override
	public String asText(State st) {
		return content.asText(st);
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return "<font color=\"" + color + "\">" + content.asHtml(st, rich) + "</font>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		Set<Renderable> r = new HashSet<>();
		r.add(content);
		return r;
	}


}
