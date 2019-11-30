package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements a tabular layout.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Table implements Renderable {
	final List<Row> table = new ArrayList<>();
	boolean border = false;
	Row openrow = null;
	private boolean nowrap = false;

	public void border(boolean border) { this.border = border; }

	public Table openRow() {
		if (openrow != null) { closeRow(); }
		openrow = new Row();
		add(openrow);
		return this;
	}

	public Table closeRow() {
		openrow = null;
		return this;
	}

	public Table add(Row r) {
		table.add(r);
		openrow = r;
		return this;
	}

	public Table add(String s) {
		add(new Text(s));
		return this;
	}

	public Table add(Boolean b) { return add(b.toString()); }

	public Table add(Renderable e) {
		add(new Cell(e));
		return this;
	}

	public Table add(Cell e) {
		if (openrow == null) { openRow(); }
		openrow.add(e);
		return this;
	}

	@Override
	public String asText(State st) {
		StringBuilder res = new StringBuilder();
		for (Row r : table) {
			if (res.length() > 0) { res.append("\n"); }
			res.append(r.asText(st));
		}
		return res.toString();
	}

	@Override
	public String asHtml(State st, boolean rich) {
		StringBuilder s = new StringBuilder();
		s.append("<table");
		if (border) { s.append(" border=1"); }
		if (nowrap) { s.append(" style=\"white-space: nowrap;\""); }
		s.append(">");
		for (Row r : table) { s.append(r.asHtml(st, rich)); }
		s.append("</table>");
		return s.toString();
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return new HashSet<>(table);
	}

	public void addNoNull(Renderable addable) {
		if (addable == null) { add(""); } else { add(addable); }
	}

	public void addNoNull(String addable) {
		if (addable == null) { add(""); } else { add(addable); }
	}

	public int rowCount() {
		return table.size();
	}

	public void nowrap() { nowrap = true; }

	public void setBGColor(String bgcolor){ openrow.setbgcolor(bgcolor);}
}
