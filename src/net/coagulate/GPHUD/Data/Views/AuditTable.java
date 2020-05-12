package net.coagulate.GPHUD.Data.Views;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.NameCache;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;

public class AuditTable extends PagedSQL {

	final NameCache cache=new NameCache();
	final String timezone;
	Char forchar;
	String olddate="";
	String cachedwhere;

	/**
	 * Defines an audit table view.
	 */
	public AuditTable(final State state,
	                  final String prefix,
	                  final SafeMap parameters) {
		super(state,prefix,parameters);
		timezone=state.getAvatar().getTimeZone();
	}

	public AuditTable(final State state,
	                  final String prefix,
	                  final SafeMap parameters,
	                  final Char forchar) {
		super(state,prefix,parameters);
		timezone=state.getAvatar().getTimeZone();
		this.forchar=forchar;
	}

	// ----- Internal Instance -----
	@Override
	protected int getRowCount() {
		return db().dqinn("select count(*) from audit where instanceid="+instance.getId()+" "+getAdditionalWhere()+" order by timedate desc "+getSQLLimit());
	}

	@Nonnull
	@Override
	protected Results runQuery() {
		return db().dq("select * from audit where instanceid="+instance.getId()+" "+getAdditionalWhere()+" order by timedate desc "+getSQLLimit());
	}

	@Override
	@Nonnull
	protected List<String> getHeaders() {
		final List<String> headers=new ArrayList<>();
		headers.add(timezone);
		headers.add("");
		headers.add("Source");
		headers.add("");
		headers.add("Target");
		headers.add("Change");
		headers.add("Old Value");
		headers.add("");
		headers.add("New Value");
		headers.add("Notes");
		return headers;
	}

	@Override
	@Nonnull
	protected String formatRow(@Nonnull final State state,
	                           @Nonnull final ResultsRow row) {
		final String timezone=state.getAvatar().getTimeZone();
		String ret="";
		final String[] datetime=fromUnixTime(row.getString("timedate"),timezone).split(" ");
		if (!olddate.equals(datetime[0])) {
			ret+="<td colspan=9999><table width=100%><tr width=100%><td width=50%><hr></td><td><span style=\"display: inline-block; white-space: nowrap;\">"+datetime[0]+"</span"+"></td><td width=50%><hr></td></tr></table></td></tr><tr>";
			olddate=datetime[0];
		}
		ret+="<td>"+datetime[1]+"</td>";

		final String sourcename=cleanse(row.getStringNullable("sourcename"));
		final String sourceowner=formatavatar(cache,row.getIntNullable("sourceowner"));
		final String sourcedev=formatavatar(cache,row.getIntNullable("sourcedeveloper"));
		final String sourceregion=formatregion(cache,row.getIntNullable("sourceregion"));
		final String sourceloc=trimlocation(cleanse(row.getStringNullable("sourcelocation")));

		ret+="<td>";
		if (!(sourcename.isEmpty() && sourceowner.isEmpty() && sourcedev.isEmpty() && sourceregion.isEmpty() && sourceloc.isEmpty())) {
			final Table internal=new Table();
			internal.nowrap();
			internal.add(new Cell("Source name:").th()).add(sourcename).closeRow();
			internal.add(new Cell("Source owner:").th()).add(sourceowner).closeRow();
			internal.add(new Cell("Source developer:").th()).add(sourcedev).closeRow();
			internal.add(new Cell("Source region:").th()).add(sourceregion).closeRow();
			internal.add(new Cell("Source location:").th()).add(sourceloc).closeRow();
			ret+=(new ToolTip("[Via]",internal)).asHtml(state,true);
		}
		ret+="</td>";

		final String srcav=formatavatar(cache,row.getIntNullable("sourceavatarid"));
		final String srcch=formatchar(cache,row.getIntNullable("sourcecharacterid"));
		final String dstav=formatavatar(cache,row.getIntNullable("destavatarid"));
		final String dstch=formatchar(cache,row.getIntNullable("destcharacterid"));
		ret+="<td align=right>";
		//t.add(new Cell(srcav+(srcav.isEmpty() || srcch.isEmpty()?"":"/")+srcch).align("right"));
		ret+=(srcav+(srcav.isEmpty() || srcch.isEmpty()?"":"/")+srcch);
		ret+="</td>";
		// if we have nothing on one side
		ret+="<td>";
		if (!((srcav.isEmpty() && srcch.isEmpty()) || (dstav.isEmpty() && dstch.isEmpty()))) {
			ret+="&rarr;";
		}
		ret+="</td>";
		ret+="<td>";
		ret+=dstav+(dstav.isEmpty() || dstch.isEmpty()?"":"/")+dstch;
		ret+="</td>";
		final String changetype=cleanse(row.getStringNullable("changetype"));
		final String changeitem=cleanse(row.getStringNullable("changeditem"));
		ret+="<td>";
		ret+=changetype+(changetype.isEmpty() || changeitem.isEmpty()?"":" - ")+changeitem;
		ret+="</td>";

		final String oldvaluestr=cleanse(row.getStringNullable("oldvalue"));
		final String newvaluestr=cleanse(row.getStringNullable("newvalue"));
		final Renderable oldvalue=notate(oldvaluestr,10);
		final Renderable newvalue=notate(newvaluestr,10);
		final Renderable notes=new Text(cleanse(row.getStringNullable("notes")));
		ret+="<td align=right>";
		ret+=oldvalue.asHtml(state,true);
		ret+="</td>";
		ret+="<td>";
		if (!(oldvaluestr.isEmpty() && newvaluestr.isEmpty())) { ret+="&rarr;"; }
		ret+="</td>";
		ret+="<td>";
		ret+=newvalue.asHtml(state,true);
		ret+="</td>";
		ret+="<td>";
		ret+=notes.asHtml(state,true);
		ret+="</td>";
		return ret;
	}

	private String getAdditionalWhere() {
		if (cachedwhere!=null) { return cachedwhere; }
		cachedwhere="";
		if (!searchtext.isEmpty()) {
			final User avatar=User.findUsernameNullable(searchtext,false);
			final Char character=Char.findNullable(instance,searchtext);
			cachedwhere=" and ( 1=2 ";
			if (avatar!=null) {
				cachedwhere+=" or sourceavatarid="+avatar.getId()+" or destavatarid="+avatar.getId()+" ";
			}
			if (character!=null) {
				cachedwhere+=" or sourcecharacterid="+character.getId()+" or destcharacterid="+character.getId()+" ";
			}
			cachedwhere+=" ) ";
		}
		if (forchar!=null) {
			cachedwhere+=" and ( sourcecharacterid="+forchar.getId()+" or destcharacterid="+forchar.getId()+" ) ";
		}
		return cachedwhere;
	}

	/*
	@Nonnull
	public static Results getAudit(@Nullable final Instance instance,
	                               @Nullable final User avatar,
	                               @Nullable final Char character) {
		final List<Object> parameters=new ArrayList<>();
		String sql="select * from audit where 1=1 ";
		if (instance!=null) {
			sql+="and instanceid=? ";
			parameters.add(instance.getId());
		}
		if (avatar!=null) {
			sql+="and (sourceavatarid=? or destavatarid=?) ";
			parameters.add(avatar.getId());
			parameters.add(avatar.getId());
		}
		if (character!=null) {
			sql+="and (sourcecharacterid=? or destcharacterid=?) ";
			parameters.add(character.getId());
			parameters.add(character.getId());
		}
		sql+=" order by timedate desc limit 0,500";
		final Object[] objectarray=new Object[0];
		return db().dq(sql,parameters.toArray(objectarray));
	}

	@Nonnull
	public static Table formatAudit(@Nonnull final Results rows,
	                                final String timezone) {
		Table table=new Table();
		table.nowrap();
		table.border(false);
		final net.coagulate.GPHUD.Interfaces.Outputs.Row headers=new HeaderRow();
		String tzheader=timezone;
		final String[] tzparts=tzheader.split("/");
		if (tzparts.length==2) { tzheader=tzparts[1]; }
		headers.add(tzheader).add("").add("Source").add("").add("Target").add("Change").add("Old Value").add("").add("New Value").add("Notes");
		table.add(headers);
		String olddate="";
		for (final ResultsRow r: rows) {


		}
		if (table.rowCount()==1) {
			table=new Table();
			table.add("No audit events");
		}
		return table;
	}
	*/
}
