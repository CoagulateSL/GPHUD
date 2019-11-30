package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements a row of elements in a table layout.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Row implements Renderable {
	final List<Cell> row = new ArrayList<>();
	String bgcolor = "";
	String alignment = "";

	public Row() {}

	public Row(Cell c) { add(c); }

	public Row(String c) { add(c); }

	public Row add(Cell c) {
		row.add(c);
		return this;
	}

	public Row add(String s) {
		row.add(new Cell(new Text(s)));
		return this;
	}

	public Row add(Renderable r) {
		row.add(new Cell(r));
		return this;
	}

	public boolean isHeader() { return false; }

	@Override
	public String asText(State st) {
		StringBuilder s = new StringBuilder();
		for (Cell c : row) {
			if (s.length() > 0) { s.append(" : "); }
			s.append(c.asText(st));
		}
		return s.toString();
	}

	@Override
	public String asHtml(State st, boolean rich) {
		StringBuilder s = new StringBuilder("<tr");
		if (!bgcolor.isEmpty()) { s.append(" bgcolor=").append(bgcolor); }
		if (!alignment.isEmpty()) { s.append(" align=").append(alignment); }
		s.append(">");
		for (Cell c : row) {
			c.header = isHeader();
			s.append(c.asHtml(st, rich));
		}
		return s + "</tr>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return new HashSet<>(row);
	}

	public void add(Integer ownerid) {
		add("" + ownerid);
	}

	public void add(boolean online) {
		add(Boolean.toString(online));
	}

	public void setbgcolor(String setbgcolor) {
		bgcolor = setbgcolor;
	}

	public void align(String alignment) {
		this.alignment = alignment;
	}

}
