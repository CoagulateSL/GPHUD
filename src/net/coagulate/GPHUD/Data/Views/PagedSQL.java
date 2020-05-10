package net.coagulate.GPHUD.Data.Views;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.NameCache;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.Interfaces.Outputs.ToolTip;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

/**
 * To be subclassed and overridden
 */

public abstract class PagedSQL implements Renderable {

	public String searchtext="";
	protected int page=0;
	protected int pagesize=50;
	String prefix;
	SafeMap parameters;
	Instance instance;

	public PagedSQL(@Nonnull final State state,
	                @Nonnull final String prefix,
	                @Nonnull final SafeMap parameters) {
		this.instance=state.getInstance();
		this.prefix=prefix;
		this.parameters=parameters;
	}

	// ----- Internal Statics -----
	@Nonnull
	protected static String cleanse(@Nullable final String s) {
		if (s==null) { return ""; }
		return s;
	}

	protected static String formatavatar(@Nonnull final NameCache cache,
	                                     @Nullable final Integer avatarid) {
		if (avatarid!=null) { return cache.lookup(User.get(avatarid)); }
		return "";
	}

	protected static String formatchar(@Nonnull final NameCache cache,
	                                   @Nullable final Integer charid) {
		if (charid!=null) { return cache.lookup(Char.get(charid)); }
		return "";
	}

	protected static String formatregion(@Nonnull final NameCache cache,
	                                     @Nullable final Integer charid) {
		if (charid!=null) { return cache.lookup(Region.get(charid,true)); }
		return "";
	}

	protected static String trimlocation(String s) {
		final String olds=s;
		s=s.replaceAll("\\(","");
		s=s.replaceAll("\\)","");
		s=s.replaceAll(" ","");
		final String[] xyz=s.split(",");
		if (xyz.length!=3) { return olds; }
		try {
			final float x=Float.parseFloat(xyz[0]);
			final float y=Float.parseFloat(xyz[1]);
			final float z=Float.parseFloat(xyz[2]);
			return ((int) x)+","+((int) y)+","+((int) z);
		}
		catch (@Nonnull final NumberFormatException e) { return olds; }
	}

	@Nonnull
	protected static Renderable notate(@Nonnull final String s,
	                                   final int size) {
		if (s.length()>size) {
			return new ToolTip(s.substring(0,size),s);
		}
		return new Text(s);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String asText(State st) {
		return "Paged table not supported in text mode";
	}

	@Nonnull
	@Override
	public String asHtml(State st,
	                     boolean rich) {
		if (parameters.containsKey(prefix+"-page-default")) { page=Integer.parseInt(parameters.get(prefix+"-page-default")); }
		if (parameters.containsKey(prefix+"-page")) { page=Integer.parseInt(parameters.get(prefix+"-page")); }
		if (parameters.containsKey(prefix+"-search")) { searchtext=parameters.get(prefix+"-search"); }
		String r="<table width=100%>";
		// header row
		r+=getPageRow();
		r+="<tr>";
		for (String header: getHeaders()) {
			r+="<th align=center>"+header+"</th>";
		}
		r+="</tr>";
		Results results=runQuery();
		for (ResultsRow row: results) {
			r+="<tr>";
			r+=formatRow(st,row);
			r+="</tr>";
		}
		r+="</table>";
		return r;
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

	// ----- Internal Instance -----

	/**
	 * Get the SQL query, the base of it, no order, limit parameters
	 *
	 * @return
	 */
	@Nonnull
	protected abstract String getSQL();

	/**
	 * Get the SQL to get the number of rows
	 */
	@Nonnull
	protected abstract String getCountSQL();

	/**
	 * Get the table headers, a map of column name (from DB) to human readable thing
	 *
	 * @return
	 */
	@Nonnull
	protected abstract List<String> getHeaders();

	protected abstract String formatRow(@Nonnull final State state,
	                                    @Nonnull final ResultsRow row);

	private String getPageRow() {
		String r="";
		int rowcount=getRowCount();
		int pagecount=(int) Math.ceil(((double) rowcount)/pagesize);
		r+="<tr><td align=center colspan=99999>";
		String encodedsearchtext="";
		try { encodedsearchtext=URLEncoder.encode(searchtext,"UTF-8"); }
		catch (UnsupportedEncodingException e) { encodedsearchtext="Encoder failure"; }
		r+="<button type=submit name="+prefix+"-submit value="+prefix+"-submit>Search:</button><input type=text name="+prefix+"-search value=\""+encodedsearchtext+"\">";
		r+="&nbsp;&nbsp;&nbsp;";
		r+="<input type=hidden name="+prefix+"-page-default value="+page+">";
		r+="<button "+(page==0?"disabled":"")+" type=submit name="+prefix+"-page value="+(page-1)+">&lt;&lt;</button>";
		r+="&nbsp;&nbsp;&nbsp;";
		r+="Page: "+(page+1)+"/"+(pagecount);
		r+="&nbsp;&nbsp;&nbsp;";
		r+="<button "+((page+1) >= pagecount?"disabled":"")+" type=submit name="+prefix+"-page value="+(page+1)+">&gt;&gt;</button>";
		r+="</td></tr>";
		return r;
	}

	private int getRowCount() {
		System.out.println(getCountSQL());
		return GPHUD.getDB().dqinn(getCountSQL());
	}

	private final Results runQuery() {
		String sql=getSQL();
		sql+=" limit "+(page*pagesize)+","+((page+1)*pagesize);
		return GPHUD.getDB().dq(sql);
	}

}
