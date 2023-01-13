package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

import static java.util.logging.Level.*;

/**
 * Abstract class for URL management across GPHUD
 */
public class URLs {
	/**
	 * Purges a URL from the system - useful if it 404'ed which means the SL remote end no longer exists
	 * <p>
	 * Purges from Characters, Objects and Regions.
	 *
	 * @param url The URL to purge.
	 */
	public static void purgeURL(final String url) {
		try {
			for (final ResultsRow row: GPHUD.getDB()
			                                .dq("select characterid,regionid from characters where url=?",url)) {
				try {
					final int charid=row.getInt("characterid");
					final Char ch=Char.get(charid);
					final State st=State.getNonSpatial(ch);
					GPHUD.getDB().d("update eventvisits set endtime=UNIX_TIMESTAMP() where characterid=?",charid);
					final Integer regionid=row.getIntNullable("regionid");
					if (regionid!=null) {
						final int howmany=GPHUD.getDB()
						                       .dqiNotNull(
								                       "select count(*) from visits visits where endtime is null and characterid=? and regionid=?",
								                       charid,
								                       regionid);
						if (howmany>0) {
							st.logger()
							  .info("HUD disconnected (404) from avatar "+st.getAvatar().getName()+" as character "+
							        st.getCharacter().getName()+", not reported as region leaver.");
						}
						GPHUD.getDB()
						     .d("update visits set endtime=UNIX_TIMESTAMP() where characterid=? and regionid=? and endtime is null",
						        charid,
						        regionid);
					}
				} catch (@Nonnull final Exception e) {
					GPHUD.getLogger("Character").log(WARNING,"Exception during per character purgeURL",e);
				}
			}
			GPHUD.getDB()
			     .d("update characters set playedby=null, url=null, urlfirst=null, urllast=null, authnode=null,zoneid=null,regionid=null where url=?",
			        url);
			GPHUD.getDB().d("update objects set url=null where url=?",url);
		} catch (@Nonnull final DBException ex) {
			GPHUD.getLogger().log(SEVERE,"Failed to purge URL from characters",ex);
		}
		try {
			for (final ResultsRow row: GPHUD.getDB().dq("select regionid,instanceid from regions where url=?",url)) {
				GPHUD.getLogger()
				     .log(INFO,
				          "Disconnecting URL for region "+Region.get(row.getInt("regionid"),true).getName()+
				          " from instance "+Instance.get(row.getInt("instanceid")).getName());
				GPHUD.getDB().d("update regions set url=null,authnode=null where regionid=?",row.getInt("regionid"));
			}
		} catch (@Nonnull final DBException ex) {
			GPHUD.getLogger().log(SEVERE,"Failed to purge URL from regions",ex);
		}
	}
}
