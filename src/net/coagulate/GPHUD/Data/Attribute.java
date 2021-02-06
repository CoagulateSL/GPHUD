package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.Modules.Experience.GenericXP;
import net.coagulate.GPHUD.Modules.Experience.QuotaedXP;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.*;

/**
 * Contains the data related to an attribute defined for an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Attribute extends TableRow {

	protected Attribute(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An Attribute representation
	 */
	@Nonnull
	public static Attribute get(final int id) {
		return (Attribute) factoryPut("Attribute",id,Attribute::new);
	}

	/**
	 * Find an attribute in an instance ; NOTE this only finds database attributes.
	 *
	 * @param instance Instance to look attribute up in.
	 * @param name     Name of attribute to locate
	 *
	 * @return Region object for that region
	 *
	 * @throws UserInputLookupFailureException if the attribute doesn't resolve
	 */
	@Nonnull
	public static Attribute find(@Nonnull final Instance instance,
	                             @Nonnull final String name) {
		try {
			final int id=db().dqiNotNull("select attributeid from attributes where name like ? and instanceid=?",name,instance.getId());
			return get(id);
		}
		catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find attribute '"+name+"' in instance '"+instance+"'",e);
		}
	}

	/**
	 * Find an attribute that is a group by 'type'.
	 *
	 * @param instance  Instance to look in
	 * @param groupType Group type (subtype) to look for
	 *
	 * @return The matching Attribute
	 *
	 * @throws UserInputLookupFailureException if there is no matching attribute
	 */
	@Nonnull
	public static Attribute findGroup(@Nonnull final Instance instance,
	                                  @Nonnull final String groupType) {
		try {
			final int id=db().dqiNotNull("select attributeid from attributes where instanceid=? and attributetype='GROUP' and grouptype=?",instance.getId(),groupType);
			return get(id);
		}
		catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find an attribute representing a group of type "+groupType,e);
		}
	}

	/**
	 * Get the attributes for the instance in this state.
	 *
	 * @param st State, Infers instance
	 *
	 * @return Set of attribute for this instance
	 */
	@Nonnull
	public static Set<Attribute> getAttributes(@Nonnull final State st) { return getAttributes(st.getInstance()); }

	/**
	 * Get the attributes for the instance.
	 *
	 * @param instance The instance to query
	 *
	 * @return Set of attribute for this instance
	 */
	@Nonnull
	public static Set<Attribute> getAttributes(@Nonnull final Instance instance) {
		return attributeSetCache.get(instance, ()-> {
			final Set<Attribute> set = new TreeSet<>();
			for (final ResultsRow r : db().dq("select attributeid from attributes where instanceid=?", instance.getId())) {
				set.add(Attribute.get(r.getInt()));
			}
			return set;
		});
	}
	private static final Cache<Set<Attribute>> attributeSetCache=Cache.getCache("GPHUD/attributeSet", CacheConfig.OPERATIONAL_CONFIG);

	/**
	 * Find attribute by name
	 *
	 * @param st   State
	 * @param name Attribute name
	 *
	 * @return Attribute or null
	 */
	@Nullable
	public static Attribute findNullable(@Nonnull final State st,
	                                     final String name) {
		final int id=new Attribute(-1).resolveToID(st,name,true);
		if (id==0) { return null; }
		return get(id);
	}

	/**
	 * Convert a text type to an attribute type
	 *
	 * @param type String form of the type
	 *
	 * @return The ATTRIBUTETYPE that corresponds
	 */
	@Nonnull
	public static ATTRIBUTETYPE fromString(@Nonnull final String type) {
		if ("text".equalsIgnoreCase(type)) { return TEXT; }
		if ("integer".equalsIgnoreCase(type)) { return INTEGER; }
		if ("group".equalsIgnoreCase(type)) { return GROUP; }
		if ("pool".equalsIgnoreCase(type)) { return POOL; }
		if ("float".equalsIgnoreCase(type)) { return FLOAT; }
		if ("color".equalsIgnoreCase(type)) { return COLOR; }
		if ("experience".equalsIgnoreCase(type)) { return EXPERIENCE; }
		if ("currency".equalsIgnoreCase(type)) { return CURRENCY; }
		if ("set".equalsIgnoreCase(type)) { return SET; }
		if ("inventory".equalsIgnoreCase(type)) { return INVENTORY; }
		throw new SystemImplementationException("Unhandled type "+type+" to convert to ATTRIBUTETYPE");
	}

	/**
	 * Create a new attribute
	 *
	 * @param instance          Instance to create in
	 * @param name              Name of attribute
	 * @param selfModify        unpriviledged user self-modify
	 * @param attributetype     "type" of attribute (defined at module level)
	 * @param groupType         subtype of attribute (see module)
	 * @param usesAbilityPoints can be increased by ability points (costs against ability points)
	 * @param required          value must be supplied
	 * @param defaultValue      default value (where not required attribute)
	 */
	public static void create(@Nonnull final Instance instance,
	                          @Nonnull final String name,
	                          final boolean selfModify,
	                          @Nonnull final ATTRIBUTETYPE attributetype,
	                          @Nullable final String groupType,
	                          final boolean usesAbilityPoints,
	                          final boolean required,
	                          @Nullable String defaultValue) {
		if ("".equals(defaultValue)) { defaultValue=null; }
		db().d("insert into attributes(instanceid,name,selfmodify,attributetype,grouptype,usesabilitypoints,required,defaultvalue) values(?,?,?,?,?,?,?,?)",
		       instance.getId(),
		       name,
		       selfModify,
		       toString(attributetype),
		       groupType,
		       usesAbilityPoints,
		       required,
		       defaultValue
		      );
		attributeSetCache.purge(instance);
	}

	/**
	 * Create a new attribute
	 *
	 * @param st                State Instance to create in
	 * @param name              Name of attribute
	 * @param selfModify        unpriviledged user self-modify
	 * @param attributetype     "type" of attribute (defined at module level)
	 * @param groupType         subtype of attribute (see module)
	 * @param usesAbilityPoints can be increased by ability points (costs against ability points)
	 * @param required          value must be supplied
	 * @param defaultValue      default value (where not required attribute)
	 */
	public static void create(@Nonnull final State st,
	                          @Nonnull final String name,
	                          final boolean selfModify,
	                          @Nonnull final ATTRIBUTETYPE attributetype,
	                          @Nullable final String groupType,
	                          final boolean usesAbilityPoints,
	                          final boolean required,
	                          @Nullable final String defaultValue) {
		create(st.getInstance(),name,selfModify,attributetype,groupType,usesAbilityPoints,required,defaultValue);
		attributeSetCache.purge(st.getInstance());
	}

	/**
	 * Convert an ATTRIBUTETYPE back into a string
	 *
	 * @param type ATTRIBUTETYPE
	 *
	 * @return String form
	 */
	@Nonnull
	public static String toString(@Nonnull final ATTRIBUTETYPE type) {
		switch (type) {
			case TEXT:
				return "text";
			case FLOAT:
				return "float";
			case INTEGER:
				return "integer";
			case GROUP:
				return "group";
			case POOL:
				return "pool";
			case COLOR:
				return "color";
			case EXPERIENCE:
				return "experience";
			case CURRENCY:
				return "currency";
			case SET:
				return "set";
			case INVENTORY:
				return "inventory";
		}
		throw new SystemImplementationException("Unhandled attributetype to string mapping for "+type);
	}

	// ----- Internal Statics -----
	// ---------- INSTANCE ----------

	/**
	 * Gets the instance associated with this attribute.
	 *
	 * @return The Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		return instanceCache.get(this, ()-> Instance.get(getInt("instanceid")));
	}
	private static final Cache<Instance> instanceCache=Cache.getCache("GPHUD/attributeInstance",CacheConfig.PERMANENT_CONFIG);

	@Nonnull
	@Override
	public String getTableName() {
		return "attributes";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "attributeid";
	}

	public void validate(@Nonnull final State st) {
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Attribute / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "attributes"; }

	@Nullable
	@Override
	public String getKVTable() { return null; }

	@Nullable
	@Override
	public String getKVIdField() { return null; }

	/**
	 * Get this attributes ATTRIBUTETYPE
	 *
	 * @return The ATTRIBUTETYPE
	 */
	@Nonnull
	public ATTRIBUTETYPE getType() {
		return attributeTypeCache.get(this, ()->
			fromString(getString("attributetype"))
		);
	}
	private static final Cache<ATTRIBUTETYPE> attributeTypeCache=Cache.getCache("GPHUD/AttributeType",CacheConfig.OPERATIONAL_CONFIG);

	/**
	 * Get this attribute's subtype, used by groups to define attribute mappings and exclusions.
	 *
	 * @return The sub type of the attribute, may be null.
	 */
	@Nullable
	public String getSubType() {
		return subTypeCache.get(this, ()->getStringNullable("grouptype"));
	}
	private static final Cache<String> subTypeCache=Cache.getCache("GPHUD/AttributeSubType",CacheConfig.OPERATIONAL_CONFIG);

	/**
	 * Returns if this attribute uses ability points.
	 *
	 * @return True if it does
	 */
	public boolean usesAbilityPoints() {
		return usesAbilityPointsCache.get(this,()->getBool("usesabilitypoints"));
	}
	private static final Cache<Boolean> usesAbilityPointsCache=Cache.getCache("GPHUD/AttributeAbilityPoints",CacheConfig.PERMANENT_CONFIG);

	/**
	 * Return if this attribute is mandatory.
	 *
	 * @return true if this attribute is required
	 */
	public boolean getRequired() {
		return requiredCache.get(this,()->getBool("required"));
	}
	private static final Cache<Boolean> requiredCache=Cache.getCache("gphud/attributeRequired",CacheConfig.OPERATIONAL_CONFIG);

	/**
	 * Sets the required flag.
	 *
	 * @param required New required flag state.
	 */
	public void setRequired(final boolean required) {
		set("required",required);requiredCache.set(this,required);
	}

	/**
	 * Returns the default value
	 *
	 * @return the default value which may be null
	 */
	@Nullable
	public String getDefaultValue() { return getStringNullable("defaultvalue"); }

	/**
	 * Set the default value for this attribute.
	 *
	 * @param defaultValue New default value
	 */
	public void setDefaultValue(@Nullable final String defaultValue) {
		set("defaultvalue",defaultValue);
	}

	/**
	 * Get the self modify flag.
	 *
	 * @return boolean true if can self modify
	 */
	public boolean getSelfModify() { return getBool("selfmodify"); }

	/**
	 * Set the self modify flag.
	 *
	 * @param selfModify Character can self modify the attribute
	 */
	public void setSelfModify(final Boolean selfModify) {
		set("selfmodify",selfModify);
	}

	/**
	 * Gets the character's current final value for an attribute.
	 *
	 * KVs are passed through the usual getKV mechanism
	 * POOLs and EXPERIENCE are summed pools
	 *
	 * @param st State, infers character
	 *
	 * @return The current value for the character, technically nullable
	 */
	@Nullable
	public String getCharacterValue(@Nonnull final State st) {
		//System.out.println("Attr:"+getName()+" is "+getType()+" of "+getClass().getSimpleName());
		if (isKV()) { return st.getKV("Characters."+getName()).value(); }
		final ATTRIBUTETYPE attributetype=getType();
		if (attributetype==GROUP) {
			if (getSubType()==null) { return null; }
			final CharacterGroup cg=CharacterGroup.getGroup(st,getSubType());
			if (cg==null) { return null; }
			return cg.getName();
		}
		if (attributetype==EXPERIENCE) {
			final GenericXP xp=new GenericXP(getName());
			return CharacterPool.sumPool(st,(xp.getPool(st)))+"";
		}
		if (attributetype==POOL && QuotaedXP.class.isAssignableFrom(getClass())) {
			final QuotaedXP xp=(QuotaedXP) this;
			return CharacterPool.sumPool(st,(xp.getPool(st)))+"";
		}
		if (attributetype==CURRENCY) {
			final Currency currency=Currency.findNullable(st,getName());
			if (currency==null) { return "NotDefined?"; }
			return currency.shortSum(st);
		}
		if (attributetype==POOL) { return "POOL"; }
		if (attributetype==SET) {
			final CharacterSet set=new CharacterSet(st.getCharacter(),this);
			return set.countElements()+" elements, "+set.countTotal()+" total qty";
		}
		if (attributetype==INVENTORY) {
			final Inventory set=new Inventory(st.getCharacter(),this);
			return set.countElements()+" items, "+set.countTotal()+" total qty";
		}
		throw new SystemImplementationException("Unhandled non KV type "+getType());
	}

	/**
	 * Get additional information about the value this attribute has for a given character
	 *
	 * KV get the computed path
	 * POOL and EXPERIENCE return quotaed information, if quotaed
	 * GROUP return nothing
	 *
	 * @param st State infers character
	 *
	 * @return A description of the value
	 */
	@Nonnull
	public String getCharacterValueDescription(@Nonnull final State st) {
		if (isKV()) { return st.getKV("Characters."+getName()).path(); }
		final ATTRIBUTETYPE attributetype=getType();
		if (attributetype==EXPERIENCE) {
			final GenericXP xp=new GenericXP(getName());
			return ("<i>(In last "+xp.periodRoughly(st)+" : "+xp.periodAwarded(st)+")</i>")+(", <i>Next available:"+xp.nextFree(st)+"</i>");
		}
		if (attributetype==POOL && QuotaedXP.class.isAssignableFrom(getClass())) {
			final QuotaedXP xp=(QuotaedXP) this;
			return ("<i>(In last "+xp.periodRoughly(st)+" : "+xp.periodAwarded(st)+")</i>")+(", <i>Next available:"+xp.nextFree(st)+"</i>");
		}
		if (attributetype==POOL) { return "POOL"; }
		if (attributetype==CURRENCY) {
			final Currency currency=Currency.findNullable(st,getName());
			if (currency==null) { return "NotDefined?"; }
			return currency.longSum(st);
		}
		if (attributetype==GROUP) { return ""; }
		if (attributetype==SET) {
			final CharacterSet set=new CharacterSet(st.getCharacter(),this);
			return set.textList();
		}
		if (attributetype==INVENTORY) {
			final Inventory set=new Inventory(st.getCharacter(),this);
			return set.textList();
		}
		throw new SystemImplementationException("Unhandled type "+getType());
	}

	/**
	 * If this attribute is represented as a KV.
	 * Group memberships (faction, race) and POOLs including EXPERIENCE are NOT a KV
	 *
	 * @return True if this attribute generates a KV.
	 */
	public boolean isKV() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER || def==FLOAT || def==TEXT || def==COLOR) { return true; }
		if (def==POOL || def==GROUP || def==EXPERIENCE || def==CURRENCY || def==SET || def==INVENTORY) { return false; }
		throw new SystemImplementationException("Unknown attribute type "+def+" in attribute "+this);
	}

	/**
	 * Gets the KV type that models this attribute.
	 *
	 * @return the KVTYPE
	 *
	 * @throws SystemException If the attribute is not of a KV represented attribute.
	 */
	@Nonnull
	public KV.KVTYPE getKVType() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER) { return KV.KVTYPE.INTEGER; }
		if (def==FLOAT) { return KV.KVTYPE.FLOAT; }
		if (def==TEXT) { return KV.KVTYPE.TEXT; }
		if (def==COLOR) { return KV.KVTYPE.COLOR; }
		throw new SystemImplementationException("Non KV attribute type "+def+" in attribute "+this);
	}

	/**
	 * Gets the default KV value for this attribute.
	 *
	 * @return The default value
	 *
	 * @throws SystemException if this attribute type is not KV backed
	 */
	@Nonnull
	public String getKVDefaultValue() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER) { return "0"; }
		if (def==FLOAT) { return "0"; }
		if (def==TEXT) { return ""; }
		if (def==COLOR) { return "<1,1,1>"; }
		throw new SystemImplementationException("Unhandled KV attribute type "+def+" in attribute "+this);
	}

	/**
	 * Gets the KV hierarchy type for this attribute.
	 *
	 * @return the appropriate KVHIERARCHY
	 *
	 * @throws SystemException If this attribute is not backed by a KV type.
	 */
	@Nonnull
	public KV.KVHIERARCHY getKVHierarchy() {
		final ATTRIBUTETYPE def=getType();
		if (def==INTEGER) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def==FLOAT) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def==TEXT) { return KV.KVHIERARCHY.DELEGATING; }
		if (def==COLOR) { return KV.KVHIERARCHY.DELEGATING; }
		throw new SystemImplementationException("Unhandled attribute type "+def+" in attribute "+this);
	}

	/**
	 * Set the uses abilitypoints flag.
	 *
	 * @param usesAbilityPoints Flags new value
	 */
	public void setUsesAbilityPoints(final Boolean usesAbilityPoints) {
		set("usesabilitypoints",usesAbilityPoints);
		usesAbilityPointsCache.set(this,usesAbilityPoints);
	}

	/**
	 * Deletes this attribute, and its data.
	 */
	public void delete(final State st) {
		// delete data
		Instance instance=getInstance();
		if (instance!=st.getInstance()) { throw new SystemConsistencyException("State instance / attribute instance mismatch during DELETE of all things"); }
		final ATTRIBUTETYPE type=getType();
		if (type==TEXT || type==FLOAT || type==INTEGER || type==COLOR) { getInstance().wipeKV("Characters."+getName()); }
		if (type==CURRENCY) {
			final Currency c=Currency.findNullable(st,getName());
			if (c!=null) { c.delete(st); }
		}
		d("delete from attributes where attributeid=?",getId());
		attributeSetCache.purge(this);
		attributeTypeCache.purge(this);
		usesAbilityPointsCache.purge(this);
	}

	public boolean readOnly() {
		return false;
	}

	public enum ATTRIBUTETYPE {
		TEXT,
		FLOAT,
		INTEGER,
		GROUP,
		POOL,
		COLOR,
		EXPERIENCE,
		CURRENCY,
		SET,
		INVENTORY
	}

	public boolean templatable() {
		return templatableCache.get(this,()->getBool("templatable"));
	}

	public void templatable(final State st,
	                        final boolean newValue) {
		if (newValue==templatable()) { return; }
		Audit.audit(false,st,OPERATOR.AVATAR,null,null,"Set",getName()+"/Templatable",""+templatable(),""+newValue,"Set templatable to "+newValue);
		set("templatable",newValue);
		templatableCache.set(this,newValue);
	}
	private static final Cache<Boolean> templatableCache=Cache.getCache("gphud/attributeTemplatable",CacheConfig.OPERATIONAL_CONFIG);
}
