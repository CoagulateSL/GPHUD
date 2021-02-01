package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static net.coagulate.Core.Tools.UnixTime.duration;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

public class CharacterPool {

	// ---------- STATICS ----------
	/**
	 * Count the number of entries in a Pool
	 *
	 * @param character The character to count the pool of
	 * @param pool      Pool
	 *
	 * @return Count of the entries
	 */
	public static int poolEntries(@Nonnull final Char character,
	                          @Nonnull final Pool pool) {
		final Integer count=db().dqi("select count(*) from characterpools where characterid=? and poolname like ?",character.getId(),pool.fullName());
		if (count==null) { return 0; }
		return count;
	}

	/**
	 * Count the number of entries in a Pool
	 *
	 * @param st   State inferring character
	 * @param pool Pool
	 *
	 * @return Count of the entries
	 */
	public static int poolEntries(@Nonnull final State st,
	                          @Nonnull final Pool pool) {
		return poolEntries(st.getCharacter(),pool);
	}

	/**
	 * Sum all the entries in a Pool
	 *
	 * @param character The character to sum the pool of
	 * @param pool      Pool
	 *
	 * @return Sum of the entries
	 */
	public static int sumPool(@Nonnull final Char character,
	                          @Nonnull final Pool pool) {
		return character.poolSumCache.get(pool,()-> {
			final Integer sum = db().dqi("select sum(adjustment) from characterpools where characterid=? and poolname like ?", character.getId(), pool.fullName());
			if (sum == null) {
				return 0;
			}
			return sum;
		});
	}

	/**
	 * Sum all the entries in a Pool
	 *
	 * @param st   State inferring character
	 * @param pool Pool
	 *
	 * @return Sum of the entries
	 */
	public static int sumPool(@Nonnull final State st,
	                          @Nonnull final Pool pool) {
		return sumPool(st.getCharacter(),pool);
	}


	/**
	 * Add an adjustment to a pool from a character.
	 *
	 * @param st          State of the grantor (requires character)
	 * @param target      Recipient of the pool change
	 * @param pool        Pool
	 * @param adjustment  Ammount to grant
	 * @param description Audit logged description
	 */
	public static void addPool(@Nonnull final State st,
	                           @Nonnull final Char target,
	                           @Nonnull final Pool pool,
	                           final int adjustment,
	                           @Nonnull final String description) {
		db().d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)",
		       target.getId(),
		       pool.fullName(),
		       adjustment,
		       st.getCharacterNullable()==null?null:st.getCharacter().getId(),
		       st.getAvatarNullable()==null?null:st.getAvatar().getId(),
		       description,
		       getUnixTime()
		      );
		target.poolSumCache.purge(pool);
	}

	/**
	 * Add an adjustment to a pool, as an administrator (Avatar).
	 *
	 * @param st          State of the grantor (requires avatar)
	 * @param target      Recipient of the pool change
	 * @param pool        Pool
	 * @param adjustment  Ammount to grant
	 * @param description Audit logged description
	 */
	public static void addPoolAdmin(@Nonnull final State st,
	                                @Nonnull final Char target,
	                                @Nonnull final Pool pool,
	                                final int adjustment,
	                                @Nonnull final String description) {
		db().d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)",
		       target.getId(),
		       pool.fullName(),
		       adjustment,
		       null,
		       st.getAvatar().getId(),
		       description,
		       getUnixTime()
		      );
		target.poolSumCache.purge(pool);
	}

	/**
	 * Add an adjustment to a pool on behalf of SYSTEM.
	 *
	 * @param st          State (not used)
	 * @param target      Recipient of the pool change
	 * @param pool        Pool
	 * @param adjustment  Ammount to adjust
	 * @param description Logged reason for the change
	 */
	public static void addPoolSystem(final State st,
	                                 @Nonnull final Char target,
	                                 @Nonnull final Pool pool,
	                                 final int adjustment,
	                                 @Nonnull final String description) {
		db().d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)",
		       target.getId(),
		       pool.fullName(),
		       adjustment,
		       null,
		       User.getSystem().getId(),
		       description,
		       getUnixTime()
		      );
		target.poolSumCache.purge(pool);
	}

	/**
	 * Sum a pool since a given time
	 *
	 * @param character The character to sum the pool of
	 * @param pool      Pool
	 * @param since     Unix Time to count points since
	 *
	 * @return Number of points in the given period.
	 */
	public static int sumPoolSince(@Nonnull final Char character,
	                               @Nonnull final Pool pool,
	                               final int since) {
		final Integer sum=db().dqi("select sum(adjustment) from characterpools where characterid=? and poolname like ? and timedate>=?",
		                           character.getId(),
		                           pool.fullName(),
		                           since
		                          );
		if (sum==null) { return 0; }
		return sum;
	}

	/**
	 * Sum a pool since a given number of days
	 *
	 * @param character Character to sum
	 * @param pool      Pool
	 * @param days      Number of days ago to start counting from.
	 *
	 * @return Number of points in the pool in the selected time range.
	 */
	public static int sumPoolDays(@Nonnull final Char character,
	                              @Nonnull final Pool pool,
	                              final float days) {
		final int seconds=(int) (days*60.0*60.0*24.0);
		return sumPoolSince(character,pool,getUnixTime()-seconds);
	}

	/**
	 * Calculate the next free point time string for a pool.
	 *
	 * @param character Character to find next free point time for
	 * @param pool      Pool
	 * @param maxxp     Maximum ammount of XP earnable in a period
	 * @param days      Period (days)
	 *
	 * @return Explanation of when the next point is available.
	 */
	@Nonnull
	public static String poolNextFree(@Nonnull final Char character,
	                                  @Nonnull final Pool pool,
	                                  final int maxxp,
	                                  final float days) {
		if (maxxp==0) { return "NEVER"; }
		final int now=getUnixTime();
		final int nextfree=poolNextFreeAt(character,pool,maxxp,days);
		if (now >= nextfree) { return "NOW"; }

		final int duration=nextfree-now;
		return "in "+duration(duration,false);
	}

	/**
	 * Calculate the next free point time string for a pool.
	 *
	 * @param state Infers character
	 * @param pool  Pool
	 * @param maxxp Maximum ammount of XP earnable in a period
	 * @param days  Period (days)
	 *
	 * @return Explanation of when the next point is available.
	 */
	@Nonnull
	public static String poolNextFree(@Nonnull final State state,
	                                  @Nonnull final Pool pool,
	                                  final int maxxp,
	                                  final float days) {
		return poolNextFree(state.getCharacter(),pool,maxxp,days);
	}

	/**
	 * Calculate the date-time of the next free point for a pool.
	 *
	 * @param character Character
	 * @param pool      Pool
	 * @param maxxp     Maximum ammount of XP in a period
	 * @param days      Period in days
	 *
	 * @return Date-time of the point of next free (may be in the past, in which case available NOW).
	 */
	public static int poolNextFreeAt(@Nonnull final Char character,
	                                 @Nonnull final Pool pool,
	                                 final int maxxp,
	                                 final float days) {
		final boolean debug=false;
		final int now=getUnixTime();
		final int since=(int) (now-(days*60*60*24));
		final Results res=db().dq("select adjustment,timedate from characterpools where characterid=? and poolname=? and timedate>?",character.getId(),pool.fullName(),since);
		int awarded=0;
		final Map<Integer,Integer> when=new TreeMap<>(); // map time stamps to award.
		for (final ResultsRow r: res) {
			final int ammount=r.getInt("adjustment");
			int at=r.getInt("timedate");
			awarded+=ammount;
			while (when.containsKey(at)) { at++; }
			when.put(at,ammount);
		}
		int overshoot=awarded-maxxp;
		if (overshoot<0) { return now; }
		final int datefilled=0;
		for (final Map.Entry<Integer,Integer> entry: when.entrySet()) {
			final int ammount=entry.getValue();
			overshoot-=ammount;
			if (overshoot<0) {
				return (int) (entry.getKey()+(days*60*60*24));
			}
		}
		return now;
	}

	/**
	 * Get all the pools this character has.
	 *
	 * @param st State infers Character
	 *
	 * @return List of Pools
	 */
	@Nonnull
	public static Set<Pool> getPools(@Nonnull final State st,
	                                 @Nonnull final Char ch) {
		final Set<Pool> pools=new TreeSet<>();
		final Results results=db().dq("select distinct poolname from characterpools where characterid=?",ch.getId());
		for (final ResultsRow r: results) {
			final String name=r.getString();
			if (st.hasModule(name)) {
				final Pool p=Modules.getPoolNullable(st,name);
				if (p!=null) { pools.add(p); }
			}
		}
		return pools;
	}

	// ----- Internal Statics -----
	private static DBConnection db() { return GPHUD.getDB(); }

    public static void delete(String fullName) {
		GPHUD.getDB().d("delete from characterpools where poolname like ?",fullName);
    }

    // ---------- INSTANCE ----------

	/**
	 * Calculate the date-time of the next free point for a pool.
	 *
	 * @param state Infers character
	 * @param pool  Pool
	 * @param maxxp Maximum ammount of XP in a period
	 * @param days  Period in days
	 *
	 * @return Date-time of the point of next free (may be in the past, in which case available NOW).
	 */
	public int poolNextFreeAt(@Nonnull final State state,
	                          @Nonnull final Pool pool,
	                          final int maxxp,
	                          final float days) {
		return poolNextFreeAt(state.getCharacter(),pool,maxxp,days);
	}
}
