package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Implements a single line text input box.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextArea extends Input {
	final String name;
	@Nullable
	Integer columns;
	@Nullable
	Integer rows;

	public TextArea(final String name) {this.name=name;}

	public TextArea(final String name,
					final String value) {
		this.name=name;
		this.value=value;
	}

	public TextArea(final String name,
					final int rows,
					final int columns) {
		this.name=name;
		this.rows=rows;
		this.columns=columns;
	}

	public TextArea(final String name,
					final String value,
					final int rows,
					final int columns) {
		this.name=name;
		this.value=value;
		this.rows=rows;
		this.columns=columns;
	}

	// ---------- INSTANCE ----------
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		String s="<textarea name=\""+name+"\" ";
		if (rows!=null) { s+="rows="+rows+" "; }
		if (columns!=null) { s+="cols="+columns+" "; }
		s+="autofocus ";
		s+=">";
		s+=value;
		s+="</textarea>";
		return s;
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

}
