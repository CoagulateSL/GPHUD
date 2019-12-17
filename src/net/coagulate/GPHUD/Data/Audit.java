package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NullInteger;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static java.util.logging.Level.WARNING;
import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Non instantiable class with static methods for auditing things.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Audit {

	@Nonnull
	public static Results getAudit(@Nullable final Instance instance, @Nullable final User avatar, @Nullable final Char character) {
		final List<Object> parameters = new ArrayList<>();
		String sql = "select * from audit where 1=1 ";
		if (instance != null) {
			sql += "and instanceid=? ";
			parameters.add(instance.getId());
		}
		if (avatar != null) {
			sql += "and (sourceavatarid=? or destavatarid=?) ";
			parameters.add(avatar.getId());
			parameters.add(avatar.getId());
		}
		if (character != null) {
			sql += "and (sourcecharacterid=? or destcharacterid=?) ";
			parameters.add(character.getId());
			parameters.add(character.getId());
		}
		sql += " order by timedate desc limit 0,500";
		final Object[] objectarray = new Object[0];
		return GPHUD.getDB().dq(sql, parameters.toArray(objectarray));
	}

	public static void audit(@Nonnull final State st, final OPERATOR op, final User targetavatar, final Char targetcharacter, final String changetype, final String changeditem, final String oldvalue, final String newvalue, final String note) {
		audit(true, st, op, targetavatar, targetcharacter, changetype, changeditem, oldvalue, newvalue, note);
	}

	public static void audit(final boolean log, @Nonnull final State st, final OPERATOR op, @Nullable final User targetavatar, @Nullable final Char targetcharacter, @Nullable final String changetype, @Nullable final String changeditem, @Nullable final String oldvalue, @Nullable final String newvalue, final String note) {
		final User avatar = st.getAvatarNullable();
		Char character = st.getCharacterNullable();
		if (op == OPERATOR.AVATAR) { character = null; }
		final Instance stinstance = st.getInstanceNullable();
		if (log) {
			//String instance = "NoInstance";
			//if (stinstance != null) { instance = stinstance.getName(); }
			String actor = "";
			if (avatar != null) {
				actor += "A:" + avatar.getName();
			}
			if (character != null) {
				if (!actor.isEmpty()) { actor += " "; }
				actor += "C:" + character.getName() + "#" + character.getId();
			}
			String facility = "";
			if (changetype != null) { facility += changetype; }
			if (changeditem != null) { facility += "/" + changeditem; }
			String target = "";
			if (targetavatar != null) {
				target += "A:" + targetavatar.getName();
			}
			if (targetcharacter != null) {
				if (!target.isEmpty()) { target += " "; }
				target += "C:" + targetcharacter.getName() + "#" + targetcharacter.getId();
			}
			String message = "Change from '";
			if (oldvalue == null) { message += "<null>"; } else { message += oldvalue; }
			message += "' to '";
			if (newvalue == null) { message += "<null>"; } else { message += newvalue; }
			message += "' on " + target + " : " + note;
			st.logger().info("<" + actor + "> in " + facility + " - " + message);
		}
		try {
			GPHUD.getDB().d("insert into audit(timedate," +
							"instanceid," +
							"sourceavatarid," +
							"sourcecharacterid," +
							"destavatarid," +
							"destcharacterid," +
							"changetype," +
							"changeditem," +
							"oldvalue," +
							"newvalue," +
							"notes," +
							"sourcename," +
							"sourceowner," +
							"sourcedeveloper," +
							"sourceregion," +
							"sourcelocation) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					getUnixTime(),
					getId(stinstance),
					getId(avatar),
					getId(character),
					getId(targetavatar),
					getId(targetcharacter),
					changetype,
					changeditem,
					oldvalue,
					newvalue,
					note,
					st.getSourcenameNullable(),
					getId(st.getSourceownerNullable()),
					getId(st.getSourcedeveloperNullable()),
					getId(st.sourceregion),
					st.sourcelocation);
		} catch (@Nonnull final DBException ex) {
			st.logger().log(WARNING, "Audit logging failure", ex);
		}
	}

	private static Object getId(@Nullable final TableRow r) {
		if (r == null) { return new NullInteger(); }
		return r.getId();
	}

	private static Object getId(@Nullable final User r) {
		if (r == null) { return new NullInteger(); }
		return r.getId();
	}

	@Nonnull
	public static Table formatAudit(@Nonnull final Results rows, final String timezone) {
		Table table = new Table();
		table.nowrap();
		table.border(false);
		final NameCache cache = new NameCache();
		final net.coagulate.GPHUD.Interfaces.Outputs.Row headers = new HeaderRow();
		String tzheader = timezone;
		final String[] tzparts = tzheader.split("/");
		if (tzparts.length == 2) { tzheader = tzparts[1]; }
		headers.add(tzheader).add("").add("Source").add("").add("Target").add("Change").add("Old Value").add("").add("New Value").add("Notes");
		table.add(headers);
		String olddate = "";
		for (final ResultsRow r : rows) {
			final String[] datetime = fromUnixTime(r.getStringNullable("timedate"), timezone).split(" ");
			if (!olddate.equals(datetime[0])) {
				final net.coagulate.GPHUD.Interfaces.Outputs.Row t = new net.coagulate.GPHUD.Interfaces.Outputs.Row();
				t.align("center");
				table.add(t);
				t.add(new Cell("<table width=100%><tr width=100%><td width=50%><hr></td><td><span style=\"display: inline-block; white-space: nowrap;\">" + datetime[0] + "</span></td><td width=50%><hr></td></tr></table>", 99999));
				olddate = datetime[0];
			}
			final net.coagulate.GPHUD.Interfaces.Outputs.Row t = new net.coagulate.GPHUD.Interfaces.Outputs.Row();
			table.add(t);
			t.add(datetime[1]);

			final String sourcename = cleanse(r.getStringNullable("sourcename"));
			final String sourceowner = formatavatar(cache, r.getIntNullable("sourceowner"));
			final String sourcedev = formatavatar(cache, r.getIntNullable("sourcedeveloper"));
			final String sourceregion = formatregion(cache, r.getIntNullable("sourceregion"));
			final String sourceloc = trimlocation(cleanse(r.getStringNullable("sourcelocation")));

			if (!(sourcename.isEmpty() && sourceowner.isEmpty() && sourcedev.isEmpty() && sourceregion.isEmpty() && sourceloc.isEmpty())) {
				final Table internal = new Table();
				internal.nowrap();
				internal.add(new Cell("Source name:").th()).add(sourcename).closeRow();
				internal.add(new Cell("Source owner:").th()).add(sourceowner).closeRow();
				internal.add(new Cell("Source developer:").th()).add(sourcedev).closeRow();
				internal.add(new Cell("Source region:").th()).add(sourceregion).closeRow();
				internal.add(new Cell("Source location:").th()).add(sourceloc).closeRow();
				t.add(new ToolTip("[Via]", internal));
			} else { t.add(""); }

			final String srcav = formatavatar(cache, r.getIntNullable("sourceavatarid"));
			final String srcch = formatchar(cache, r.getIntNullable("sourcecharacterid"));
			final String dstav = formatavatar(cache, r.getIntNullable("destavatarid"));
			final String dstch = formatchar(cache, r.getIntNullable("destcharacterid"));
			t.add(new Cell(srcav + (srcav.isEmpty() || srcch.isEmpty() ? "" : "/") + srcch).align("right"));
			// if we have nothing on one side
			if ((srcav.isEmpty() && srcch.isEmpty()) || (dstav.isEmpty() && dstch.isEmpty())) {
				t.add("");
			} else {
				t.add("&rarr;");
			}
			t.add(dstav + (dstav.isEmpty() || dstch.isEmpty() ? "" : "/") + dstch);
			final String changetype = cleanse(r.getStringNullable("changetype"));
			final String changeitem = cleanse(r.getStringNullable("changeditem"));
			t.add(changetype + (changetype.isEmpty() || changeitem.isEmpty() ? "" : " - ") + changeitem);

			final String oldvaluestr = cleanse(r.getStringNullable("oldvalue"));
			final String newvaluestr = cleanse(r.getStringNullable("newvalue"));
			final Renderable oldvalue = notate(oldvaluestr, 10);
			final Renderable newvalue = notate(newvaluestr, 10);
			final Renderable notes = new Text(cleanse(r.getStringNullable("notes")));
			t.add(new Cell(oldvalue).align("right"));
			if (oldvaluestr.isEmpty() && newvaluestr.isEmpty()) { t.add(""); } else { t.add("&rarr;"); }
			t.add(newvalue);
			t.add(notes);
            /*
            net.coagulate.GPHUD.Interfaces.Outputs.Row t2=new net.coagulate.GPHUD.Interfaces.Outputs.Row();
            t2.setbgcolor("#f0f0f0");
            t2.add(new Cell(cleanse(r.getString("oldvalue")),3));
            t2.add(new Cell(cleanse(r.getString("newvalue")),2));
            t2.add(new Cell(cleanse(r.getString("notes")),7));
            table.add(t2);
            */
		}
		if (table.rowCount() == 1) {
			table = new Table();
			table.add("No audit events");
		}
		return table;
	}

	@Nonnull
	private static Renderable notate(@Nonnull final String s, final int size) {
		if (s.length() > size) {
			return new ToolTip(s.substring(0, size), s);
		}
		return new Text(s);
	}

	@Nonnull
	private static String cleanse(@Nullable final String s) {
		if (s == null) { return ""; }
		return s;
	}

	private static String formatavatar(@Nonnull final NameCache cache, @Nullable final Integer avatarid) {
		if (avatarid != null) { return cache.lookup(User.get(avatarid)); }
		return "";
	}

	private static String formatchar(@Nonnull final NameCache cache, @Nullable final Integer charid) {
		if (charid != null) { return cache.lookup(Char.get(charid)); }
		return "";
	}

	private static String formatregion(@Nonnull final NameCache cache, @Nullable final Integer charid) {
		if (charid != null) { return cache.lookup(Region.get(charid,true)); }
		return "";
	}

	private static String trimlocation(String s) {
		final String olds = s;
		s = s.replaceAll("\\(", "");
		s = s.replaceAll("\\)", "");
		s = s.replaceAll(" ", "");
		final String[] xyz = s.split(",");
		if (xyz.length != 3) { return olds; }
		try {
			final float x = Float.parseFloat(xyz[0]);
			final float y = Float.parseFloat(xyz[1]);
			final float z = Float.parseFloat(xyz[2]);
			return ((int) x) + "," + ((int) y) + "," + ((int) z);
		} catch (@Nonnull final NumberFormatException e) { return olds; }
	}

	public enum OPERATOR {AVATAR, CHARACTER}
}
