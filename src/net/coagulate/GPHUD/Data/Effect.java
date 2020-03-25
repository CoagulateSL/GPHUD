package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * An effect - a sort of temporary group membership, or a "buff" if you like.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Effect extends TableRow {

	private Effect(){super();}
	protected Effect(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An effect representation
	 */
	@Nonnull
	public static Effect get(final int id) {
		return (Effect) factoryPut("Effect",id,new Effect(id));
	}

	@Nonnull
	public static Effect get(@Nonnull final State st,@Nonnull final String name) {
		Effect effect=getNullable(st,name);
		if (effect==null) { throw new UserInputLookupFailureException("There is no effect named "+name); }
		return effect;
	}

	@Nullable
	public static Effect getNullable(@Nonnull final State st,@Nonnull final String name) {
		return find(st.getInstance(),name);
	}

	public static Set<Effect> getAll(Instance instance) {
		Set<Effect> effects=new TreeSet<>();
		for (ResultsRow effect: GPHUD.getDB().dq("select id from effects where instanceid=?",instance.getId())) {
			effects.add(Effect.get(effect.getInt()));
		}
		return effects;
	}

	public static void create(@Nonnull final State st,
	                          @Nonnull final String name) {
		if (Effect.getNullable(st,name)!=null) { throw new UserInputDuplicateValueException("There is already an effect named "+name); }
		GPHUD.getDB().d("insert into effects(instanceid,name) values(?,?)",st.getInstance().getId(),name);
	}

	static void wipeKV(@Nonnull final Instance instance,
				       final String key) {
		final Effect i=new Effect();
		final String kvtable=i.getKVTable();
		final String maintable=i.getTableName();
		final String kvidcolumn=i.getKVIdField();
		final String maintableidcolumn=i.getIdField();
		GPHUD.getDB()
		     .d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+kvidcolumn+"="+maintable+"."+maintableidcolumn+" and "+maintable+
				        ".instanceid=?",
		        key,
		        instance.getId()
		);
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
		Results matches=GPHUD.getDB().dq("select id from effects where instanceid=? and name like ?",instance.getId(),name);
		if (matches.empty()) { return null; }
		if (matches.size()>1) { throw new TooMuchDataException("Name "+name+" in instance "+instance.getId()+" matched "+matches.size()+" results"); }
		return Effect.get(matches.iterator().next().getInt());
	}

	public static Set<Effect> get(State st,Char character) {
		for (ResultsRow row:GPHUD.getDB().dq("select effectid from effectsapplications where characterid=? and expires<?",character.getId(),UnixTime.getUnixTime())) {
			int effectid=row.getInt();
			Effect effect=get(effectid);
			effect.validate(st);
			effect.expire(st,character);
		}
		Set<Effect> set=new HashSet<>();
		for (ResultsRow row:GPHUD.getDB().dq("select effectid from effectsapplications where characterid=? and expires>=?",character.getId(),UnixTime.getUnixTime())) {
			set.add(get(row.getInt()));
		}
		return set;
	}

	public void expire(State st,Char character) {
		if (dqinn("select count(*) from effectsapplications where characterid=? and effectid=?",character.getId(),getId())==0) { return; }
		validate(st);
		character.validate(st);
		Audit.audit(true,st,User.getSystem(),null,character.getOwner(),character,"Effect","Expire",getName(),"","Effect "+getName()+" expired from character");
		d("delete from effectsapplications where characterid=? and effectid=?",character.getId(),getId());
		String applykv=st.getKV(this,"Effects.RemoveMessage");
		if (applykv!=null && (!applykv.isEmpty())) {
			JSONObject message=new JSONObject();
			message.put("message",applykv);
			new Transmission(character,message).start();
		}
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "effects";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "id";
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
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
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


	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Effect / State Instance mismatch");
		}
	}

	protected int getNameCacheTime() { return 60; } // events may become renamable, cache 60 seconds

	// perhaps flush the caches (to do) when this happens...
	public void delete(State st) {
		validate(st);
		String name=getName();
		d("delete from effects where id=?",getId());
		Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Delete","Effect",name,null,"Deleted Effect "+name);
	}

	/** Apply an effect to a character
	 *  @param st State
	 * @param administrative Applied as admin (AVATAR) or st.CHAR?
	 * @param target Target character
	 * @param seconds Number of seconds to apply
	 * @return True if the effect was applied, false if it was skipped due to an existing buff being of longer duration.  Exceptions on input errors.
	 */
	public boolean apply(State st,
	                     boolean administrative,
	                     Char target,
	                     int seconds) {
		// validate everything
		target.validate(st);
		validate(st);
		if (seconds<=0) { throw new UserInputInvalidChoiceException("Number of seconds to apply effect must be greater than zero"); }
		int expires=UnixTime.getUnixTime()+seconds;
		// any existing?
		if (dqinn("select count(*) from effectsapplications where effectid=? and characterid=? and expires>=?",getId(),target.getId(),expires)>0) {
			// already has a same or longer lasting buff
			return false;
		}
		d("delete from effectsapplications where effectid=? and characterid=?",getId(),target.getId());
		d("insert into effectsapplications(effectid,characterid,expires) values(?,?,?)",getId(),target.getId(),expires);
		Audit.OPERATOR operator=Audit.OPERATOR.CHARACTER;
		if (administrative) { operator=Audit.OPERATOR.AVATAR; }
		Audit.audit(true,st,operator,target.getOwner(),target,"Add","Effect","",getName(),"Applied effect "+getName()+" for "+UnixTime.duration(seconds));
		String applykv=st.getKV(this,"Effects.ApplyMessage");
		if (applykv!=null && (!applykv.isEmpty())) {
			JSONObject message=new JSONObject();
			message.put("message",applykv);
			new Transmission(target,message).start();
		}
		return true;
	}
}

