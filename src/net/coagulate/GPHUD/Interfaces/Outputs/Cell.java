package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * A cell in a table.
 * Basically an encapsulated Element, that is an element its self.
 *
 * @author iain
 */
public class Cell implements Renderable {

	@Nullable
	Renderable e;
	boolean header;
	int colspan=1;
	String align="";

	public Cell() {}

	public Cell(final String s) { e=new Text(s); }

	public Cell(@Nullable final Renderable e) {
		if (e==null) { throw new SystemImplementationException("Abstract Cell is not renderable."); }
		this.e=e;
	}

	public Cell(final String s,
	            final int colspan)
	{
		e=new Text(s);
		this.colspan=colspan;
	}

	public Cell(@Nullable final Renderable e,
	            final int colspan)
	{
		if (e==null) {
			throw new SystemImplementationException("Abstract Cell is not renderable");
		}
		this.e=e;
		this.colspan=colspan;
	}

	@Nonnull
	Renderable e() {
		if (e==null) { throw new SystemBadValueException("Cell content was null"); }
		return e;
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		if (header) { return "*"+e().asText(st)+"*"; }
		return e().asText(st);
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich)
	{
		String s="";
		if (header) { s+="<th"; } else { s+="<td"; }
		if (colspan>1) { s+=" colspan="+colspan; }
		if (!align.isEmpty()) { s+=" align="+align; }
		s+=">";
		s+=e().asHtml(st,rich);
		s+="</";
		if (header) { s+="th>"; } else { s+="td>"; }
		return s;
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		final Set<Renderable> r=new HashSet<>();
		r.add(e);
		return r;
	}

	@Nonnull
	public Cell th() {
		header=true;
		return this;
	}

	@Nonnull
	public Cell align(final String align) {
		this.align=align;
		return this;
	}

}
