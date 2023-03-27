package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NullInteger;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.logging.Level.WARNING;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Non instantiable class with static methods for auditing things.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Audit {
	// ---------- STATICS ----------
	
	public static void audit(@Nonnull final State st,
	                         final OPERATOR op,
	                         final User targetavatar,
	                         final Char targetcharacter,
	                         final String changetype,
	                         final String changeditem,
	                         final String oldvalue,
	                         final String newvalue,
	                         final String note) {
		audit(true,st,op,targetavatar,targetcharacter,changetype,changeditem,oldvalue,newvalue,note);
	}
	
	public static void audit(final boolean log,
	                         @Nonnull final State st,
	                         final OPERATOR op,
	                         @Nullable final User targetavatar,
	                         @Nullable final Char targetcharacter,
	                         @Nullable final String changetype,
	                         @Nullable final String changeditem,
	                         @Nullable final String oldvalue,
	                         @Nullable final String newvalue,
	                         final String note) {
		final User sourceavatar=st.getAvatarNullable();
		Char sourcecharacter=st.getCharacterNullable();
		if (op==OPERATOR.AVATAR) {
			sourcecharacter=null;
		}
		audit(log,
		      st,
		      sourceavatar,
		      sourcecharacter,
		      targetavatar,
		      targetcharacter,
		      changetype,
		      changeditem,
		      oldvalue,
		      newvalue,
		      note);
	}
	
	public static void audit(final boolean log,
	                         @Nonnull final State st,
	                         @Nullable final User sourceavatar,
	                         @Nullable final Char sourcecharacter,
	                         @Nullable final User targetavatar,
	                         @Nullable final Char targetcharacter,
	                         @Nullable final String changetype,
	                         @Nullable final String changeditem,
	                         @Nullable final String oldvalue,
	                         @Nullable final String newvalue,
	                         final String note) {
		
		final Instance stinstance=st.getInstanceNullable();
		if (log) {
			//String instance = "NoInstance";
			//if (stinstance != null) { instance = stinstance.getName(); }
			String actor="";
			if (sourceavatar!=null) {
				actor+="A:"+sourceavatar.getName();
			}
			if (sourcecharacter!=null) {
				if (!actor.isEmpty()) {
					actor+=" ";
				}
				actor+="C:"+sourcecharacter.getName()+"#"+sourcecharacter.getId();
			}
			String facility="";
			if (changetype!=null) {
				facility+=changetype;
			}
			if (changeditem!=null) {
				facility+="/"+changeditem;
			}
			String target="";
			if (targetavatar!=null) {
				target+="A:"+targetavatar.getName();
			}
			if (targetcharacter!=null) {
				if (!target.isEmpty()) {
					target+=" ";
				}
				target+="C:"+targetcharacter.getName()+"#"+targetcharacter.getId();
			}
			String message="Change from '";
			if (oldvalue==null) {
				message+="<null>";
			} else {
				message+=oldvalue;
			}
			message+="' to '";
			if (newvalue==null) {
				message+="<null>";
			} else {
				message+=newvalue;
			}
			message+="' on "+target+" : "+note;
			st.logger().info("<"+actor+"> in "+facility+" - "+message);
		}
		try {
			db().d("insert into audit(timedate,"+"instanceid,"+"sourceavatarid,"+"sourcecharacterid,"+"destavatarid,"+
			       "destcharacterid,"+"changetype,"+"changeditem,"+"oldvalue,"+"newvalue,"+"notes,"+"sourcename,"+
			       "sourceowner,"+"sourcedeveloper,"+"sourceregion,"+"sourcelocation) values(?,?,?,?,?,?,?,?,?,?,?,?,"+
			       "?,?,?,?)",
			       getUnixTime(),
			       getId(stinstance),
			       getId(sourceavatar),
			       getId(sourcecharacter),
			       getId(targetavatar),
			       getId(targetcharacter),
			       changetype,
			       changeditem,
			       oldvalue,
			       newvalue,
			       note,
			       st.getSourceNameNullable(),
			       getId(st.getSourceOwnerNullable()),
			       getId(st.getSourceDeveloperNullable()),
			       getId(st.sourceRegion),
			       st.sourceLocation);
		} catch (@Nonnull final DBException ex) {
			st.logger().log(WARNING,"Audit logging failure",ex);
		}
	}
	
	
	// ----- Internal Statics -----
	private static DBConnection db() {
		return GPHUD.getDB();
	}
	
	private static Object getId(@Nullable final TableRow r) {
		if (r==null) {
			return new NullInteger();
		}
		return r.getId();
	}
	
	private static Object getId(@Nullable final User r) {
		if (r==null) {
			return new NullInteger();
		}
		return r.getId();
	}
	
	public static void truncate() {
		GPHUD.getDB().d("delete from audit where timedate<?",getUnixTime()-(UnixTime.DAY*90));
	}
	
	public enum OPERATOR {
		AVATAR,CHARACTER
	}
}
