package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	final List<Cell> row=new ArrayList<>();
	String bgcolor="";
	String alignment="";

	public Row() {}

	public Row(final Cell c) { add(c); }

	public Row(final String c) { add(c); }

	// ---------- INSTANCE ----------
	@Nonnull
	public Row add(final Cell c) {
		row.add(c);
		return this;
	}

	@Nonnull
	public Row add(final String s) {
		row.add(new Cell(new Text(s)));
		return this;
	}

	@Nonnull
	public Row add(final Renderable r) {
		row.add(new Cell(r));
		return this;
	}

	public boolean isHeader() { return false; }

	@Nonnull
	@Override
	public String asText(final State st) {
		final StringBuilder s=new StringBuilder();
		for (final Cell c: row) {
			if (!s.isEmpty()) {
				s.append(" : ");
			}
			s.append(c.asText(st));
		}
		return s.toString();
	}

	@Nonnull
    @Override
    public String asHtml(final State st,
                         final boolean rich) {
        return asHtml(st, rich, 0);
    }

    public String asHtml(final State st, final boolean rich, final int rownum) {
        final StringBuilder s = new StringBuilder("<tr");
        if (!id.isBlank()) {
            s.append(" id=\"").append(id).append("\" ");
        }
        if (!bgcolor.isEmpty()) {
            s.append(" bgcolor=").append(bgcolor);
        } else {
            s.append(" bgcolor=#").append((rownum % 2) == 1 ? "f0f0f0" : "ffffff");
        }
        if (!alignment.isEmpty()) {
            s.append(" align=").append(alignment);
        }
        s.append(">");
        for (final Cell c : row) {
            c.header = isHeader();
            s.append(c.asHtml(st, rich));
        }
		return s+"</tr>";
	}

	private boolean reset;
	public Row resetNumbering() { reset=true; return this; }
	public boolean isResetNumbering() { return reset; }

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return new HashSet<>(row);
	}

	public void add(final Integer ownerid) {
		add(""+ownerid);
	}

	public void add(final boolean online) {
		add(Boolean.toString(online));
	}

	public void setbgcolor(final String setbgcolor) {
		bgcolor=setbgcolor;
	}

	public void align(final String alignment) {
		this.alignment=alignment;
	}
	private String id="";

    public void id(final String rowID) {
        this.id = rowID;
    }
}
