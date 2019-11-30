package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.coagulate.Core.Tools.UnixTime.duration;
import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;

/**
 * Just used to store data about characters for the "view all" page, because interrogating everything one cell at a time would be painful.
 * See Instance.getCharacterSummary()
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CharacterSummary implements Comparable<CharacterSummary> {
	public boolean retired = false;
	int id = 0;
	String name = "";
	Integer ownerid = 0;
	String ownername = "";
	Integer lastactive = 0;
	boolean online = false;
	Integer totalvisits = 0;
	Integer recentvisits = 0;
	Integer totalxp = 0;
	List<String> groupheaders = new ArrayList<>();
	final Map<String, String> groups = new HashMap<>();

	static String sortLink(String current, String link) {
		if (link.equals(current)) { return "<a href=\"?sort=-" + link + "\">" + link + "</a> &darr;"; }
		if (("-" + link).equals(current)) { return "<a href=\"?sort=" + link + "\">" + link + "</a> &uarr;"; }
		return "<a href=\"?sort=" + link + "\">" + link + "</a>";
	}

	public Row headers(State st) throws UserException, SystemException {
		String uri = st.getDebasedURL().replaceAll("%20", " ");
		uri = uri.replaceFirst(".*?sort=", "");
		Row r = new HeaderRow();
		r.add(new Cell(sortLink(uri, "Name")).th());
		r.add(new Cell(sortLink(uri, "Owner")).th());
		r.add(new Cell(sortLink(uri, "Last Active")).th());
		r.add(new Cell(sortLink(uri, "Online")).th());
		r.add(new Cell(sortLink(uri, "Total Visit Time")).th());
		r.add(new Cell(sortLink(uri, "Visit Time (Last " + Experience.getCycleLabel(st) + ")")).th());
		for (String header : groupheaders) {
			r.add(new Cell(sortLink(uri, header)).th());
		}
		if (st.hasModule("Experience")) {
			r.add(new Cell(sortLink(uri, "Total XP")).th());
			r.add(new Cell(sortLink(uri, "Level")).th());
		}
		return r;
	}

	public Row asRow(State st) throws UserException, SystemException {
		Row r = new Row();
		if (retired) { r.setbgcolor("#ffe0e0"); }
		r.add(Char.getLink(name, "characters", id));
		r.add(User.getGPHUDLink(ownername, ownerid));
		String tz = st.avatar().getTimeZone();
		r.add(fromUnixTime(lastactive, tz) + " " + tz);
		r.add(online);
		r.add(duration(totalvisits));
		r.add(duration(recentvisits));
		for (String group : groupheaders) {
			String add = groups.get(group);
			if (add == null) { add = ""; }
			r.add(add);
		}
		if (st.hasModule("Experience")) {
			r.add(totalxp);
			r.add(Experience.toLevel(st, totalxp));
		}
		return r;
	}

	void setGroup(String grouptype, String groupname) {
		groups.put(grouptype, groupname);
	}

	@Override
	public int compareTo(@NotNull CharacterSummary o) {
		return Integer.compare(id, o.id);
	}
}
