package net.coagulate.GPHUD.Data.Views;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Data.NameCache;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;

public class PoolTable extends PagedSQL {

	final NameCache cache=new NameCache();
	final Char forchar;
	final String timezone;
	String olddate="";
	final String poolname;
	String cachedwhere;

	/**
	 * Defines an audit table view.
	 */


	public PoolTable(final State state,
	                 final String prefix,
	                 final SafeMap parameters,
	                 final Char forchar,
	                 final String poolname) {
		super(state,prefix,parameters);
		timezone=state.getAvatar().getTimeZone();
		this.forchar=forchar;
		this.poolname=poolname;
		if (state.getInstance()!=forchar.getInstance()) { throw new SystemConsistencyException("State/char instance mismatch"); }
	}

	// ----- Internal Instance -----
	@Override
	protected boolean maxWidth() {
		return false;
	}

	@Override
	@Nonnull
	protected List<String> getHeaders() {
		final List<String> headers=new ArrayList<>();
		headers.add(timezone);
		headers.add("Source");
		headers.add("Change");
		headers.add("Description");
		headers.add("Total");
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

		final String srcav=formatavatar(cache,row.getIntNullable("adjustedbyavatar"));
		final String srcch=formatchar(cache,row.getIntNullable("adjustedbycharacter"));
		ret+="<td align=right>";
		ret+=(srcav+(srcav.isEmpty() || srcch.isEmpty()?"":"/")+srcch);
		ret+="</td>";
		final String newvaluestr=""+row.getInt("adjustment");
		final Renderable notes=new Text(cleanse(row.getStringNullable("description")));
		ret+="<td align=right>";
		if (poolname.toLowerCase().startsWith("currency.")) {
			ret+=Currency.find(state,poolname.substring(9)).shortTextForm(row.getInt("adjustment"));
		} else {
			ret+=newvaluestr;
		}
		ret+="</td>";
		ret+="<td>";
		ret+=notes.asHtml(state,true);
		ret+="</td>";
		ret+="<td align=right>";
		if (poolname.toLowerCase().startsWith("currency.")) {
			ret+=Currency.find(state,poolname.substring(9)).shortTextForm(row.getInt("cumsum"));
		} else {
			ret+=row.getInt("cumsum")+"";
		}
		ret+="</td>";
		return ret;
	}

	@Override
	protected int getRowCount() {
		return db().dqiNotNull("select count(*) from characterpools where poolname=? and characterid="+forchar.getId()+" "+getAdditionalWhere()+" order by timedate desc ",
		                  poolname
		                 );
	}

	@Nonnull
	@Override
	protected Results runQuery() {
		//return db().dq("select * from characterpools where poolname=? and characterid="+forchar.getId()+" "+getAdditionalWhere()+" order by timedate desc "+getSQLLimit(),
		//               poolname);
		return db().dq("select * from (select *,@runtot:=@runtot+adjustment as cumsum from ( select * from characterpools where poolname like ? and characterid="+forchar.getId()+" order by timedate asc) t join (select @runtot:=0) r order by timedate) q where 1=1 "+getAdditionalWhere()+" order by timedate desc "+getSQLLimit(),poolname);
	}

	private String getAdditionalWhere() {
		if (cachedwhere!=null) { return cachedwhere; }
		cachedwhere="";
		if (!searchtext.isEmpty()) {
			final User avatar=User.findUsernameNullable(searchtext,false);
			final Char character=Char.findNullable(instance,searchtext);
			cachedwhere=" and ( 1=2 ";
			if (avatar!=null) {
				cachedwhere+=" or adjustedbyavatar="+avatar.getId()+" ";
			}
			if (character!=null) {
				cachedwhere+=" or adjustedbycharacter="+character.getId()+" ";
			}
			cachedwhere+=" ) ";
		}
		return cachedwhere;
	}
}
