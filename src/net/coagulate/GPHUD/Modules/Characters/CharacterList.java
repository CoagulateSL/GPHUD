package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.CharacterSummary;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Show all characters within the instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CharacterList {

	// ---------- STATICS ----------
	@URLs(url="/characters/list*",
	      requiresPermission="Characters.ViewAll")
	public static void list(@Nonnull final State st,
	                        final SafeMap parameters) { listInternal(st,parameters,false); }

	@URLs(url="/characters/retiredlist*",
	      requiresPermission="Characters.ViewAll")
	public static void listWithRetired(@Nonnull final State st,
	                                   final SafeMap parameters) { listInternal(st,parameters,true); }

	// ----- Internal Statics -----
	private static void listInternal(@Nonnull final State st,
	                                 final SafeMap parameters,
	                                 final boolean showretired) {

		int page=0;
		String searchtext="";

		if (parameters.containsKey("page-default")) { page=Integer.parseInt(parameters.get("page-default")); }
		if (parameters.containsKey("page")) { page=Integer.parseInt(parameters.get("page")); }
		if (parameters.containsKey("search")) { searchtext=parameters.get("search"); }


		final List<CharacterSummary> list=st.getInstance().getCharacterSummary(st,searchtext,showretired);
		if (list.isEmpty()) {
			st.form().add("No characters found");
			return;
		}

		final int totalrows=list.size();

		final Form f=st.form();
		f.add("<table border=1>");
		f.add(getPageRow(page,totalrows,searchtext));
		f.add(list.get(0).headers(st).asHtml(st,true));

		final int start=page*50;
		int end=(page+1)*50;
		if (end >= list.size()) { end=list.size()-1; }
		for (int row=start;row<=end;row++) {
			final CharacterSummary s=list.get(row);
			f.add(s.asRow(st).asHtml(st,true));
		}
		f.add("</table>");
	}
	private static String getPageRow(final int page,
	                                 final int rowcount,
	                                 final String searchtext) {
		String r="";
		final int pagecount=(int) Math.ceil(((double) rowcount)/50);
		r+="<tr><td align=center colspan=99999>";
		r+="<button type=submit name=submit value=submit>Search:</button><input type=text name=search value=\""+searchtext+"\">";
		r+="&nbsp;&nbsp;&nbsp;";
		r+="<input type=hidden name=page-default value="+page+">";
		r+="<button "+(page==0?"disabled":"")+" type=submit name=page value="+(page-1)+">&lt;&lt;</button>";
		r+="&nbsp;&nbsp;&nbsp;";
		r+="Page: "+(page+1)+"/"+(pagecount);
		r+="&nbsp;&nbsp;&nbsp;";
		r+="<button "+((page+1) >= pagecount?"disabled":"")+" type=submit name=page value="+(page+1)+">&gt;&gt;</button>";
		r+="</td></tr>";
		return r;
	}

}
