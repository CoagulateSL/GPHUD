package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
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
	public boolean retired;
	int id;
	@Nonnull
	String name = "";

	int ownerid;
	String ownername = "";
	int lastactive;
	boolean online;
	int totalvisits;
	int recentvisits;
	int totalxp;
	@Nonnull
	List<String> groupheaders = new ArrayList<>();
	final Map<String, String> groups = new HashMap<>();

	@Nonnull
	static String sortLink(final String current, @Nonnull final String link) {
		if (link.equals(current)) { return "<a href=\"?sort=-" + link + "\">" + link + "</a> &darr;"; }
		if (("-" + link).equals(current)) { return "<a href=\"?sort=" + link + "\">" + link + "</a> &uarr;"; }
		return "<a href=\"?sort=" + link + "\">" + link + "</a>";
	}

	@Nonnull
	public Row headers(@Nonnull final State st) {
		String uri = st.getDebasedURL().replaceAll("%20", " ");
		uri = uri.replaceFirst(".*?sort=", "");
		final Row r = new HeaderRow();
		r.add(new Cell(sortLink(uri, "Name")).th());
		r.add(new Cell(sortLink(uri, "Owner")).th());
		r.add(new Cell(sortLink(uri, "Last Active")).th());
		r.add(new Cell(sortLink(uri, "Online")).th());
		r.add(new Cell(sortLink(uri, "Total Visit Time")).th());
		r.add(new Cell(sortLink(uri, "Visit Time (Last " + Experience.getCycleLabel(st) + ")")).th());
		for (final String header : groupheaders) {
			r.add(new Cell(sortLink(uri, header)).th());
		}
		if (st.hasModule("Experience")) {
			r.add(new Cell(sortLink(uri, "Total XP")).th());
			r.add(new Cell(sortLink(uri, "Level")).th());
		}
		return r;
	}

	@Nonnull
	public Row asRow(@Nonnull final State st) {
		final Row r = new Row();
		if (retired) { r.setbgcolor("#ffe0e0"); }
		r.add(Char.getLink(name, "characters", id));
		r.add(User.getGPHUDLink(ownername, ownerid));
		final String tz = st.getAvatar().getTimeZone();
		r.add(fromUnixTime(lastactive, tz) + " " + tz);
		r.add(online);
		r.add(duration(totalvisits));
		r.add(duration(recentvisits));
		for (final String group : groupheaders) {
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

	void setGroup(final String grouptype, final String groupname) {
		groups.put(grouptype, groupname);
	}

	@Override
	public int compareTo(final @NotNull CharacterSummary o) {
		return Integer.compare(id, o.id);
	}
}
