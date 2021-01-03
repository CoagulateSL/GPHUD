package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	final List<Row> table=new ArrayList<>();
	HeaderRow headerrow;
	boolean border;
	@Nullable
	Row openrow;
	private boolean nowrap;

	// ---------- INSTANCE ----------
	public Table border(final boolean border) {
		this.border=border;
		return this;
	}

	public Table border() { return border(true); }

	@Nonnull
	public Table openRow() {
		if (openrow!=null) { closeRow(); }
		openrow=new Row();
		add(openrow);
		return this;
	}

	@Nonnull
	public Table closeRow() {
		openrow=null;
		return this;
	}

	@Nonnull
	public Table header(@Nonnull final String s) {
		if (headerrow==null) { headerrow=new HeaderRow(); }
		headerrow.add(s);
		return this;
	}

	@Nonnull
	public Table add(final Row r) {
		table.add(r);
		openrow=r;
		return this;
	}

	@Nonnull
	public Table add(@Nullable final String s) {
		if (s==null) { return add(new Text("")); }
		return add(new Text(s));
	}

	@Nonnull
	public Table add(@Nullable final Integer i) {
		if (i==null) { return add(new Text("")); }
		return add(new Text(i.toString()));
	}

	@Nonnull
	public Table add(@Nonnull final Boolean b) { return add(b.toString()); }

	@Nonnull
	public Table add(final Renderable e) {
		add(new Cell(e));
		return this;
	}

	@Nonnull
	public Table add(final Cell e) {
		if (openrow==null) { openRow(); }
		openrow.add(e);
		return this;
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		final StringBuilder res=new StringBuilder();
		if (headerrow!=null) { res.append(headerrow.asText(st)); }
		for (final Row r: table) {
			if (res.length()>0) { res.append("\n"); }
			res.append(r.asText(st));
		}
		return res.toString();
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		final StringBuilder s=new StringBuilder();
		s.append("<table");
		if (border) { s.append(" border=1"); }
		if (nowrap) { s.append(" style=\"white-space: nowrap;\""); }
		s.append(">");
		if (headerrow!=null) { s.append(headerrow.asHtml(st,rich)); }
		int rownum=0;
		for (final Row r: table) {
			if (r.isResetNumbering()) { rownum=0; }
			s.append(r.asHtml(st,rich,rownum));
			rownum++;
		}
		s.append("</table>");
		return s.toString();
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return new HashSet<>(table);
	}

	public void addNoNull(@Nullable final Renderable addable) {
		if (addable==null) { add(""); }
		else { add(addable); }
	}

	public void addNoNull(@Nullable final String addable) {
		if (addable==null) { add(""); }
		else { add(addable); }
	}

	public int rowCount() {
		return table.size();
	}

	public void nowrap() { nowrap=true; }

	public void setBGColor(final String bgcolor) {
		if (openrow==null) { openRow(); }
		openrow.setbgcolor(bgcolor);
	}

    public Table openRow(String rowID) {
		openRow();
		openrow.id(rowID);
		return this;
    }
}
