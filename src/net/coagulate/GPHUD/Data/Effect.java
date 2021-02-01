package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

/**
 * An effect - a sort of temporary group membership, or a "buff" if you like.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Effect extends TableRow {

	private Effect() {super();}

	protected Effect(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An effect representation
	 */
	@Nonnull
	public static Effect get(final int id) {
		return (Effect) factoryPut("Effect",id,Effect::new);
	}

	/**
	 * Search for an effect by name
	 *
	 * @param st   State
	 * @param name Name of effect
	 *
	 * @return The effect
	 *
	 * @throws UserInputLookupFailureException if the effect does not exist
	 */
	@Nonnull
	public static Effect get(@Nonnull final State st,
	                         @Nonnull final String name) {
		final Effect effect=getNullable(st,name);
		if (effect==null) { throw new UserInputLookupFailureException("There is no effect named "+name); }
		return effect;
	}

	/**
	 * Search for an effect by name
	 *
	 * @param st   State
	 * @param name Name of effect
	 *
	 * @return The effect or null if not found
	 */
	@Nullable
	public static Effect getNullable(@Nonnull final State st,
	                                 @Nonnull final String name) {
		return find(st.getInstance(),name);
	}

	/**
	 * Get all effects for this instance
	 *
	 * @param instance Instance
	 *
	 * @return a Set of Effect objects
	 */
	public static Set<Effect> getAll(final Instance instance) {
		final Set<Effect> effects=new TreeSet<>();
		for (final ResultsRow effect: db().dq("select id from effects where instanceid=?",instance.getId())) {
			effects.add(Effect.get(effect.getInt()));
		}
		return effects;
	}

	/**
	 * Create an effect.
	 *
	 * @param st   State
	 * @param name Name of effect
	 *
	 * @throws UserInputDuplicateValueException If the effect already exists
	 */
	public static void create(@Nonnull final State st,
	                          @Nonnull final String name) {
		if (Effect.getNullable(st,name)!=null) { throw new UserInputDuplicateValueException("There is already an effect named "+name); }
		db().d("insert into effects(instanceid,name) values(?,?)",st.getInstance().getId(),name);
	}

	/**
	 * Find an effect by name
	 *
	 * @param instance Instance we're searching
	 * @param name     Name of effect
	 *
	 * @return Effect object
	 */
	@Nullable
	public static Effect find(@Nonnull final Instance instance,
	                          final String name) {
		final Results matches=db().dq("select id from effects where instanceid=? and name like ?",instance.getId(),name);
		if (matches.empty()) { return null; }
		if (matches.size()>1) { throw new TooMuchDataException("Name "+name+" in instance "+instance.getId()+" matched "+matches.size()+" results"); }
		return Effect.get(matches.iterator().next().getInt());
	}

	/**
	 * Purge expired effects.
	 *
	 * @param st        State
	 * @param character Character to review
	 */
	public static void expirationCheck(@Nonnull final State st,
	                                   @Nonnull final Char character) {
		if (st.expirationChecked) { return; }
		for (final ResultsRow row: db().dq("select effectid from effectsapplications where characterid=? and expires<?",character.getId(),UnixTime.getUnixTime())) {
			final int effectid=row.getInt();
			final Effect effect=get(effectid);
			effect.validate(st);
			effect.unapply(st,character,true);
		}
		st.expirationChecked =true; // run this basically once per request
	}

	/**
	 * Get effects for a character
	 *
	 * Procs expiration
	 *
	 * @param st        State
	 * @param character Character to get effects for
	 *
	 * @return A Set of Effect objects describing the characters active effects
	 */
	@Nonnull
	public static Set<Effect> get(@Nonnull final State st,
	                              @Nonnull final Char character) {
		return effectCache.get(character,()-> {
			expirationCheck(st, character);
			final Set<Effect> set = new HashSet<>();
			for (final ResultsRow row : db().dq("select effectid from effectsapplications where characterid=? and expires>=?", character.getId(), UnixTime.getUnixTime())) {
				set.add(get(row.getInt()));
			}
			return set;
		});
	}
	private static final Cache<Set<Effect>> effectCache=Cache.getCache("GPHUD/EffectsActive",CacheConfig.MINIMAL);

	/**
	 * Add conveyances for effects to the JSON object.
	 *
	 * @param st        The state
	 * @param character The character to convey
	 * @param json      The json state to add to
	 */
	public static void conveyEffects(@Nonnull final State st,
	                                 @Nonnull final Char character,
	                                 @Nonnull final JSONObject json) {
		expirationCheck(st,character);
		final Set<Effect> effects=get(st,character);
		final Map<Integer,Effect> byduration=new TreeMap<>();
		for (final Effect effect: effects) {
			int remain=effect.remains(character);
			while (byduration.containsKey(remain)) {
				remain++;
			}
			String include=st.getKV(effect,"Effects.ShowEffect");
			if (include==null || include.isEmpty()) { include="true"; }
			if ("true".equalsIgnoreCase(include)) { byduration.put(remain,effect); }
		}
		int i=1;
		for (final Entry<Integer,Effect> entry: byduration.entrySet()) {
			if (i<4) {
				final Effect effect=entry.getValue();
				json.put("effect"+i,(entry.getKey()+UnixTime.getUnixTime())+"");
				String texture=st.getKV(effect,"Effects.EffectIcon");
				if (texture==null || texture.isEmpty()) { texture="b39860d0-8c5c-5d51-9dbf-3ef55dafe8a4"; }
				json.put("effect"+i+"t",texture);
				i++;
			}
		}
		if (i==1) {
			json.put("effect1","0");
			json.put("effect1t", Config.getCoagulateSquareLogo());
		}
		character.appendConveyance(new State(character),json);
	}

	// ----- Internal Statics -----
	static void wipeKV(@Nonnull final Instance instance,
	                   final String key) {
		final Effect i=new Effect();
		final String kvtable=i.getKVTable();
		final String maintable=i.getTableName();
		final String kvidcolumn=i.getKVIdField();
		final String maintableidcolumn=i.getIdColumn();
		db().d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+kvidcolumn+"="+maintable+"."+maintableidcolumn+" and "+maintable+".instanceid=?",
		       key,
		       instance.getId()
		      );
	}

	// ---------- INSTANCE ----------

	/**
	 * Unapply an effect from a character.
	 *
	 * @param st        The state
	 * @param character The character to remove the effect from
	 * @param audit     If true, audit this as an expiration, otherwise don't audit
	 *
	 * @return true if an effect was removed, otherwise false.
	 */
	public boolean unapply(@Nonnull final State st,
	                       @Nonnull final Char character,
	                       final boolean audit) {
		if (dqinn("select count(*) from effectsapplications where characterid=? and effectid=?",character.getId(),getId())==0) { return false; }
		validate(st);
		character.validate(st);
		if (audit) {
			Audit.audit(true,st,User.getSystem(),null,character.getOwner(),character,"Effect","Expire",getName(),"","Effect "+getName()+" expired from character");
		}
		d("delete from effectsapplications where characterid=? and effectid=?",character.getId(),getId());
		final String applykv=st.getKV(this,"Effects.RemoveMessage");
		final JSONObject message=new JSONObject();
		if (applykv!=null && (!applykv.isEmpty())) {
			JSONResponse.message(message,applykv,character.getProtocol());
		}
		conveyEffects(st,character,message);
		new Transmission(character,message).start();
		return true;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "effects";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "id";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Effect / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "configuration/Effects";
	}

	@Nonnull
	@Override
	public String getKVTable() {
		return "effectskvstore";
	}

	@Nonnull
	@Override
	public String getKVIdField() {
		return "effectid";
	}

	protected int getNameCacheTime() { return 60*60; } // events may become renamable, cache 60 seconds

	/**
	 * Get the instnce for this Effect.
	 *
	 * @return The instance
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	/**
	 * Deletes the effect
	 *
	 * @param st State
	 */
	public void delete(@Nonnull final State st) {
		validate(st);
		final String name=getName();
		d("delete from effects where id=?",getId());
		Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Delete","Effect",name,null,"Deleted Effect "+name);
	}

	/**
	 * Apply an effect to a character
	 *
	 * @param st             State
	 * @param administrative Applied as admin (AVATAR) or st.CHAR?
	 * @param target         Target character
	 * @param seconds        Number of seconds to apply
	 *
	 * @return True if the effect was applied, false if it was skipped due to an existing effect being of longer duration.  Exceptions on input errors.
	 */
	public boolean apply(@Nonnull final State st,
	                     final boolean administrative,
	                     @Nonnull final Char target,
	                     final int seconds) {
		// validate everything
		target.validate(st);
		validate(st);
		if (seconds<=0) { throw new UserInputInvalidChoiceException("Number of seconds to apply effect must be greater than zero"); }
		final int expires=UnixTime.getUnixTime()+seconds;
		// any existing?
		if (dqinn("select count(*) from effectsapplications where effectid=? and characterid=? and expires>=?",getId(),target.getId(),expires)>0) {
			// already has a same or longer lasting effect
			return false;
		}
		d("delete from effectsapplications where effectid=? and characterid=?",getId(),target.getId());
		d("insert into effectsapplications(effectid,characterid,expires) values(?,?,?)",getId(),target.getId(),expires);
		Audit.OPERATOR operator=Audit.OPERATOR.CHARACTER;
		if (administrative) { operator=Audit.OPERATOR.AVATAR; }
		Audit.audit(true,st,operator,target.getOwner(),target,"Add","Effect","",getName(),"Applied effect "+getName()+" for "+UnixTime.duration(seconds));
		final String applykv=st.getKV(this,"Effects.ApplyMessage");
		final JSONObject message=new JSONObject();
		if (applykv!=null && (!applykv.isEmpty())) {
			JSONResponse.message(message,applykv,target.getProtocol());
		}
		conveyEffects(st,target,message);
		new Transmission(target,message).start();
		return true;
	}

	/**
	 * Have an entity act to remove an Effect.
	 *
	 * @param st             State
	 * @param target         Target character
	 * @param administrative Wether the action is by the Avatar (admin operation) or Character (effect dispell)
	 *
	 * @return true if an effect was removed, otherwise false.
	 */
	public boolean remove(@Nonnull final State st,
	                      @Nonnull final Char target,
	                      final boolean administrative) {
		final boolean didanything=unapply(st,target,false);
		if (!didanything) { return false; }
		if (administrative) {
			Audit.audit(true,st,Audit.OPERATOR.AVATAR,target.getOwner(),target,"Removed","Effect",getName(),"","Administratively removed effect "+getName());
		}
		else {
			Audit.audit(true,st,OPERATOR.CHARACTER,target.getOwner(),target,"Removed","Effect",getName(),"","Removed effect "+getName());
		}
		return true;
	}


	/**
	 * Gets the duration remaining on this effect on a character.
	 *
	 * @param character Character to query
	 *
	 * @return Number of seconds left on the Effect, or -1 if not in effect
	 */
	public int remains(@Nonnull final Char character) {
		final Results set=dq("select expires from effectsapplications where effectid=? and characterid=?",getId(),character.getId());
		if (set.size()==0) { return -1; }
		if (set.size()>1) { throw new SystemConsistencyException(getName()+" has "+set.size()+" applications to "+character); }
		final int expires=set.iterator().next().getInt();
		return expires-UnixTime.getUnixTime();
	}

	/**
	 * Get a human readable duration for the effect's time remaining.
	 *
	 * @param character Character to get duration for
	 *
	 * @return the duration left on the effect, as a String, or "--" if the effect isn't applied
	 */
	public String humanRemains(@Nonnull final Char character) {
		final int remains=remains(character);
		if (remains==-1) { return "--"; }
		return UnixTime.duration(remains,true).trim();
	}
}

