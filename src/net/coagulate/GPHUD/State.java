package net.coagulate.GPHUD;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static net.coagulate.GPHUD.Modules.KV.KVTYPE.COLOR;

/**
 * Used as an alternative to HTTPContext to pass around the state of the request.
 * Note some caches are used here, which can be flushed if necessary.
 * Note these caches exist for the duration of the request ONLY and are private to the request.
 * Caching for any duration is prohibited by the MYSQL ASYNC replication backend GPHUD.getDB().
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class State extends DumpableState {
	public Map<String,String> externals;

	private static final Map<Thread, State> stateMap = new ConcurrentHashMap<>();
	public int protocol; // protocol as specified by remote JSON, if it exists

	public static void maintenance() {
		try {
			for (final Thread entry : stateMap.keySet()) {
				if (!entry.isAlive()) {
					stateMap.remove(entry);
				}
			}
		} catch (final ConcurrentModificationException ignored) {
		}
	}
	public static State get() {
		if (!stateMap.containsKey(Thread.currentThread())) { throw new SystemImplementationException("GPHUD session State is not initialised at this point."); }
		return stateMap.get(Thread.currentThread());
	}
	public void register() {
		if (stateMap.containsKey(Thread.currentThread())) { throw new SystemImplementationException("GPHUD session State is already initialised."); }
		stateMap.put(Thread.currentThread(),this);
	}
	private final Map<TableRow,Map<String,String>> kvMaps =new HashMap<>();
	@Nonnull
	public Sources source=Sources.NONE;
	@Nullable
	public HttpContext context;
	// web interface stores an error here for rendering
	@Nullable
	public Exception exception;
	// web interface stores the logged in userid if applicable
	@Nullable
	public String username;
	// system interface sets this if we're "runas" someone other than the owner
	public boolean isSuid;
	// web interface logged in user ID, may be null if they cookie in as an avatar :)
	// avatar, from web interface, or second life
	@Nullable
	public User avatar;
	@Nullable
	public String cookieString;
	@Nullable
	public Region sourceRegion;
	@Nullable
	public String sourceLocation;
	// used by the HUD interface to stash things briefly
	public String command;
	@Nullable
	public Zone zone;
	@Nullable
	public Integer roll;
	@Nullable
	public GSVM vm;
	@Nonnull
	public Map<Integer,Set<String>> permissionsGroupCache=new HashMap<>();
	@Nullable
	public String objectKey;
	@Nullable
	public Obj object;
	// used by Effect to only run the expiration checker once per player request as any effects intensive stuff will spam calls to the checker
	public boolean expirationChecked;
	// cache the computed main menu template
	public String mainMenuTemplate;
	@Nullable
	Set<String> permissionsCache;
	@Nullable
	Set<Attribute> attributes;
	// map of post values read in the user interface
	@Nullable
	private SafeMap postMap;
	@Nullable
	private String callBackURL;
	@Nullable
	private HttpRequest req;
	// system interface puts input here
	@Nullable
	private JSONObject json;
	// system interface sets to raw json string
	// web interface cookie, used to logout things
	@Nullable
	private Cookie cookie;
	@Nullable
	private Form form;
	// system interface puts the object originating the request here
	@Nullable
	private User sourceOwner;
	@Nullable
	private String sourceName;
	@Nullable
	private User sourceDeveloper;
	@Nullable
	private String uri;
	@Nullable
	private State target;
	@Nullable
	private String regionName;
	@Nullable
	private Region region;
	@Nullable
	private Instance instance;
	// character
	@Nullable
	private Char character;
	@Nullable
	private Boolean superuser;
	@Nullable
	private Boolean instanceOwner;
	private boolean elevated;
	@Nullable
	public SafeMap parameterDebugRaw;
	@Nullable
	public Map<String,Object> parameterDebug;
	@Nullable
	public List<Object> parameterDebugFinal;

	public State() {}

	public State(@Nullable final HttpRequest req,
	             @Nullable final HttpContext context) {
		req(req);
		this.context=context;
	}


	public State(@Nonnull final Char c) {
		character=c;
		avatar=c.getPlayedByNullable();
		instance=c.getInstance();
		region=c.getRegion();
		zone=c.getZone();
	}

	public State(@Nonnull final Instance i) { instance=i; }

	public State(@Nullable final Instance i,
	             @Nullable final Region r,
	             @Nullable final Zone z,
	             @Nonnull final Char c) {
		instance=i;
		region=r;
		zone=z;
		character=c;
		avatar=c.getPlayedByNullable();
	}

	// ---------- STATICS ----------
	@Nonnull
	public static State getNonSpatial(@Nonnull final Char c) {
		final State ret=new State();
		ret.setInstance(c.getInstance());
		ret.setAvatar(c.getOwner());
		ret.setCharacter(c);
		return ret;
	}

	// ---------- INSTANCE ----------
	public void flushPermissionsGroupCache() { permissionsGroupCache=new HashMap<>(); }

	// system interface sets to "runas" - defaults to object owner but can be overridden.  THIS IS "WHO WE ARE RUNNING AS"
	// web interface sets this to the "logged in CHARACTER" object
	// system interface sets to instance

	public boolean hasModule(@Nonnull final String module) {
		return Modules.get(null, module).isEnabled(this);
	}

	@Nonnull
	public Set<String> getCharacterGroupTypes() {
		final Set<String> types=new TreeSet<>();
		types.add("");
		for (final Attribute a: getAttributes()) {
			if (a.getType()==Attribute.ATTRIBUTETYPE.GROUP) {
				final String groupType=a.getSubType();
				if (groupType!=null && !groupType.isEmpty()) { types.add(groupType); }
			}
		}
		return types;
	}

	@Nullable
	public Attribute getAttribute(@Nonnull final String name) {
		final Set<Attribute> map=getAttributes();
		for (final Attribute a: map) {
			if (name.equalsIgnoreCase(a.getName())) { return a; }
		}
		return null;
	}

	@Nullable
	public Attribute getAttribute(@Nonnull final CharacterGroup group) {
		final String keyword=group.getType();
		for (final Attribute attr: getAttributes()) {
			if (attr.getType()==Attribute.ATTRIBUTETYPE.GROUP) {
				final String type=attr.getSubType();
				if (type!=null && type.equals(keyword)) {
					return attr;
				}
			}
		}
		return null;
	}

	public Logger logger() {
		String subspace=getInstanceString();
		if (avatar!=null) { subspace+="."+avatar.getName(); }
		if (character!=null) { subspace+="."+character.getNameSafe(); }
		return GPHUD.getLogger(subspace);
	}

	// requested uri
	@Nonnull
	public String getFullURL() {
		String debased=getDebasedURL();
		if (!debased.startsWith("/")) { debased="/"+debased; }
		return "/GPHUD"+debased;
	}

	@Nonnull
	public String getDebasedURL() {
		if (uri==null) { throw new SystemConsistencyException("Attempted to get URI but it's null?"); }
		if (uri.toUpperCase().startsWith("/GPHUD/")) { return uri.substring(6); }
		return uri;
	}

	@Nonnull
	public String getDebasedNoQueryURL() {
		String ret=getDebasedURL();
		//System.out.println("Pre parsing:"+ret);
		if (ret.contains("?")) { ret=ret.substring(0,ret.indexOf("?")); }
		//System.out.println("Post parsing:"+ret);
		return ret;
	}

	public void setURL(final String url) { uri=url; }

	@Nullable
	public State getTargetNullable() { return target; }

	@Nonnull
	public State getTarget() {
		if (target==null) { throw new UserInputStateException("There is no selected target"); }
		return target;
	}

	public void setTarget(@Nonnull final Char c) {
		target=new State(instance,region,zone,c);
	}

	/**
	 * Get the region name - this is EITHER the name of the Region object (see getRegion()) or a temporary string.
	 * The temporary string is set by setRegionName and is only used if the Region object is null (getRegion() errors, getRegionNullable() nulls).
	 * The temporary string is used during instance creation / region registration when there is no valid Region object at this point, but we need the data to create from.
	 *
	 * @return Region name (Region.getName() usually)
	 */
	@Nullable
	public String getRegionName() {
		if (region!=null) { return region.getName(); }
		// this fallback is used as a stub when we're registering a region and nothing more.
		return regionName;
	}

	public void setRegionName(@Nullable final String regionName) {
		this.regionName =regionName;
	}

	/**
	 * Get the region this connection is using.
	 *
	 * @return Region object
	 */
	@Nonnull
	public Region getRegion() {
		final Region r=getRegionNullable();
		if (r==null) { throw new UserInputStateException("No region has been selected"); }
		return r;
	}

	public void setRegion(@Nonnull final Region region) {
		region.validate(this);
		this.region=region;
		regionName =region.getName();
	}

	@Nullable
	public Region getRegionNullable() {
		if (region!=null) { region.validate(this); }
		return region;
	}

	@Nonnull
	public Char getCharacter() {
		final Char c=getCharacterNullable();
		if (c==null) { throw new UserInputStateException("No character is selected"); }
		return c;
	}

	public void setCharacter(@Nullable final Char character) {
		if (character!=null) { character.validate(this); }
		this.character=character;
		if (this.character!=null && avatar==null) { avatar=character.getPlayedByNullable(); }
	}

	@Nullable
	public Char getCharacterNullable() {
		if (character!=null) { character.validate(this); }
		return character;
	}

	@Nullable
	public User getAvatarNullable() { return avatar; }

	@Nonnull
	public User getAvatar() {
		if (avatar==null) { throw new UserInputStateException("There is no logged in avatar"); }
		return avatar;
	}

	public void setAvatar(@Nullable final User avatar) {
		this.avatar=avatar;
	}

	/**
	 * Return the Instance object associated with this connection.
	 *
	 * @return Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		final Instance i=getInstanceNullable();
		if (i==null) { throw new UserInputStateException("No instance has been selected"); }
		return i;
	}

	public void setInstance(@Nullable final Instance instance) {
		if (instance!=null) { instance.validate(this); }
		this.instance=instance;
	}

	@Nullable
	public Instance getInstanceNullable() {
		if (instance!=null) { instance.validate(this); }
		return instance;
	}

	@Nonnull
	public String getInstanceString() {
		if (instance==null) { return "<null>"; }
		return instance.toString();
	}

	public boolean isSuperUser() {
		populateSuperUser();
		if (superuser==null) { return false; }
		return superuser;
	}

	public void flushPermissionsCache() { permissionsCache =null; }

	/**
	 * Checks, and caches, if a user has a permission.
	 * Note this assumes superuser is always allowed, as is instance owner.
	 * DO NOT USE THIS TO PROTECT SUPERUSER ONLY OPERATIONS IN ANY WAY, INSTANCE OWNERS CAN ALWAYS DO EVERYTHING THE PERMISSION SYSTEM ALLOWS.
	 *
	 * @param permission Permission string to check, can be a comma separated list
	 *
	 * @return true/false
	 */
	public boolean hasPermission(@Nullable final String permission) {
		if (permission==null || permission.isEmpty()) { return true; }
		//        Modules.validatePermission(permission);
		// special case, just in case i do something stupid...
		if ("User.SuperAdmin".equalsIgnoreCase(permission)) {
			return isSuperUser();// not even instance owners or elevated stuff bypasses superadmin powers!
		}
		if (isSuperUser()) {
			return true;
		}
		if (isInstanceOwner()) { return true; }
		if (elevated()) { return true; }
		if (User.getSystem().equals(getAvatarNullable())) { return true; }
		preparePermissionsCache();
		if (permissionsCache ==null) { return false; }
		for (final String checkAgainst:permission.split(",")) {
			for (final String check : permissionsCache) {
				if (check.equalsIgnoreCase(checkAgainst)) {
					return true;
				}
				if (checkAgainst.endsWith(".*")) {
					if (check.startsWith(checkAgainst.substring(0, checkAgainst.length()-2))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks for a permission.
	 * Writes an error to the FORM if not present
	 *
	 * @param permission name of permission
	 *
	 * @return True/false
	 */
	public boolean hasPermissionOrAnnotateForm(final String permission) {
		final boolean hasPermission=hasPermission(permission);
		if (hasPermission) { return true; }
		form().add(new TextError("Insufficient permissions: You require "+permission));
		return false;
	}

	@Nullable
	public Set<String> getPermissions() {
		preparePermissionsCache();
		return permissionsCache;
	}

	/*public void flushInstanceOwner() { instanceOwner=null; }*/

	public boolean isInstanceOwner() {
		prepareInstanceOwner();
		if (instanceOwner ==null) { return false; }
		return instanceOwner;
	}

	@Nonnull
	public KV getKVDefinition(@Nonnull final String kvName) {
		return Modules.getKVDefinition(this,kvName);
	}

	@Nonnull
	public List<TableRow> getTargetList(@Nonnull final KV kv) {
		final KV.KVSCOPE scope=kv.scope();
		// create a ordered list of all the relevant objects, valued or not
		final List<TableRow> check=new ArrayList<>();
		// in DELEGATING order
		if (scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.SERVER || scope==KV.KVSCOPE.SPATIAL || scope==KV.KVSCOPE.INSTANCE || scope==KV.KVSCOPE.NONSPATIAL) {
			if (instance!=null) { check.add(instance); }
		}
		if (scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.SERVER || scope==KV.KVSCOPE.SPATIAL) {
			if (region!=null) { check.add(region); }
		}
		if (scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.SPATIAL || scope==KV.KVSCOPE.ZONE) {
			if (zone!=null) { check.add(zone); }
		}
		// events in ID order
		if (scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.SPATIAL || scope==KV.KVSCOPE.EVENT) {
			final Map<Integer,Event> eventMap=new TreeMap<>();
			Zone zone=null;
			if (character!=null) { zone=character.getZone(); }
			for (final Event e: Event.getActive(this)) {
				//boolean playerInZone=false;
				for (final Zone eventZone : e.getZones()) {
					if (eventZone == zone) {
						eventMap.put(e.getId(), e);
					}
				}
			}
			check.addAll(eventMap.values());
		}
		// characterGroups in ID order
		if (scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.NONSPATIAL) {
			if (character!=null) {
				check.addAll(CharacterGroup.getGroups(character));
			}
		}
		//character
		if (scope==KV.KVSCOPE.CHARACTER || scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.NONSPATIAL) {
			if (character!=null) { check.add(character); }
		}
		//effects
		if (scope==KV.KVSCOPE.EFFECT || scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.NONSPATIAL) {
			if (character!=null) {
				final Map<Integer,Effect> map=new TreeMap<>();
				for (final Effect e: Effect.get(this,character)) {
					map.put(e.getId(),e);
				}
				check.addAll(map.values());
			}
		}
		return check;
	}

	// tells us which is the target from where we would derive a value.
	@Nullable
	public TableRow determineTarget(@Nonnull final KV kv) {
		final List<TableRow> targets=getTargetList(kv);
		TableRow ret=null;
		switch (kv.hierarchy()) {
			case NONE:
				if (targets.size()>1) {
					throw new SystemImplementationException("NONE hierarchy type returned "+targets.size()+" results... unable to compute :P");
				}
				if (targets.size()==1) {
					ret=targets.get(0); // "the" element
				}
				break;
			case AUTHORITATIVE:
				// from highest to lowest, first value we find takes precedence.
				for (final TableRow dbo: targets) {
					if (ret==null) { // already found? do nothing
						if (kvDefined(dbo,kv) && getKV(dbo,kv.fullName())!=null) { ret=dbo; }
					}
				}
				break;
			case DELEGATING:
				// rather the inverse logic, just take the 'lowest' match
				for (final TableRow dbo: targets) {
					if (kvDefined(dbo,kv) && getKV(dbo,kv.fullName())!=null) { ret=dbo; }
				}
				break;
			case CUMULATIVE:
				throw new SystemImplementationException("Can not determineTarget() a CUMULATIVE set, you should getTargetList(KV) it instead and sum it.  or just use getKV"+"()");
			default:
				throw new SystemImplementationException("Unknown hierarchy type "+kv.hierarchy());
		}
		return ret;
	}

	@Nonnull
	public KVValue getKV(@Nonnull final String kvName) {
		try {
			final StringBuilder path=new StringBuilder();
			final KV kv=getKVDefinition(kvName);
			if (kv.hierarchy()==KV.KVHIERARCHY.CUMULATIVE) {
				float sum=0;
				boolean triggered=false;
				final List<TableRow> list=getTargetList(kv);
				if (!list.isEmpty()) {
					for (final TableRow dbo: getTargetList(kv)) {
						final String raw=getKV(dbo,kvName);
						if (raw!=null && !raw.isEmpty()) {
							sum=sum+Float.parseFloat(raw);
							triggered=true;
							if (kv.type()==KV.KVTYPE.INTEGER) {
								path.append(" +").append(Integer.parseInt(raw)).append(" (").append(dbo.getNameSafe()).append(")");
							}
							else {
								path.append(" +").append(Float.parseFloat(raw)).append(" (").append(dbo.getNameSafe()).append(")");
							}
						}
					}
					if (triggered) {
						if (kv.type() == KV.KVTYPE.INTEGER) {
							return new KVValue(String.valueOf((int) sum), path.toString());
						}
						return new KVValue(String.valueOf(sum), path.toString());
					}
				}
				return new KVValue(templateDefault(kv),"Template Default");
			}
			else {
				final TableRow target=determineTarget(kv);
				if (target==null) {
					return new KVValue(templateDefault(kv),"No Target Template Default");
				}
				final String value=getKV(target,kvName);
				if (value==null) {
					return new KVValue(templateDefault(kv),"Null Value Template Default");
				}
				return new KVValue(getKV(target,kvName),"Direct value from "+target.getClass().getSimpleName()+" "+target.getNameSafe());
			}
		}
		catch (@Nonnull final RuntimeException re) {
			throw new UserConfigurationException("Failed to evaluate KV "+kvName+": "+re.getLocalizedMessage(),re);
		}
	}

	public String templateDefault(@Nonnull final KV kv) {
		final String s=kv.defaultValue();
		if (!kv.template()) { return s; }
		boolean evaluate=false;
		boolean isInt=false;
		if (kv.type()==KV.KVTYPE.FLOAT) { evaluate=true; }
		if (kv.type()==KV.KVTYPE.INTEGER) {
			evaluate=true;
			isInt=true;
		}
		return Templater.template(this,s,evaluate,isInt);
	}

	public String getRawKV(@Nonnull final TableRow target,
	                       @Nonnull final String kvName) {
		return getKVMap(target).get(kvName.toLowerCase());
	}

	public String getKV(@Nonnull final TableRow target,
	                    @Nonnull final String kvName) {
		final String s=getRawKV(target,kvName);
		final KV kv=getKVDefinition(kvName);
		if (!kv.template()) { return s; }
		boolean evaluate=false;
		boolean isInt=false;
		if (kv.type()==KV.KVTYPE.FLOAT) { evaluate=true; }
		if (kv.type()==KV.KVTYPE.INTEGER) {
			evaluate=true;
			isInt=true;
		}
		String out;
		try { out=Templater.template(this,s,evaluate,isInt); }
		catch (@Nonnull final Exception e) {
			throw new UserConfigurationException("Failed loading KV "+kvName+" for "+target.getTableName()+" "+target.getNameSafe()+" : "+e.getLocalizedMessage(),e);
		}
		if (kv.type()== COLOR && out!=null) {
			while (out.startsWith("<<")) { out=out.replaceFirst("<<","<"); }
			while (out.endsWith(">>")) { out=out.replaceFirst(">>",">"); }
		}
		return out;
	}

	public void purgeCache(final TableRow dbo) {
		kvMaps.remove(dbo);
	}

	public void setKV(@Nonnull final TableRow dbo,
					  @Nonnull final String key,
					  @Nullable final String value) {
		setKV(dbo, key, value, true);
	}

	public void setKV(@Nonnull final TableRow dbo,
					  @Nonnull final String key,
					  @Nullable String value,
					  final boolean pushUpdate) {
		final KV definition = getKVDefinition(key);
		if (value != null && !value.isEmpty()) {
			if (!definition.template()) { // these are hard to verify :P
				switch (definition.type()) {
					case TEXT: // no checking here :P
						break;
					case INTEGER: // check it parses into an int
						try {
							Integer.parseInt(value);
						} catch (@Nonnull final NumberFormatException e) {
							throw new UserInputValidationParseException(key + " must be a whole number, you entered '" + value + "' (" + e.getLocalizedMessage() + ")", e, true);
						}
						break;
					case FLOAT:
						try { Float.parseFloat(value); }
						catch (@Nonnull final NumberFormatException e) {
							throw new UserInputValidationParseException(key + " must be a number, you entered '" + value + "' (" + e.getLocalizedMessage() + ")", e, true);
						}
						break;
					case UUID:
						if (!Validators.uuid(value)) {
							throw new UserInputValidationParseException(key+" must be a UUID , you entered '"+value+"'",true);
						}
						break;
					case BOOLEAN:
						value=value.toLowerCase();
						if (!("true".equals(value) || "false".equals(value))) {
							throw new UserInputValidationParseException(key+" must be true/false",true);
						}
						break;
					case COMMAND:
						try { Modules.getCommandNullable(this,value); }
						catch (@Nonnull final SystemException e) {
							throw new UserInputValidationParseException(key + " must be an internal command, you entered '" + value + "' and it gave a weird error", e);
						}
						catch (@Nonnull final UserException f) {
							throw new UserInputValidationParseException(key + " must be an internal command, you entered '" + value + "' (" + f.getLocalizedMessage() + ")", f);
						}
						break;
					case COLOR:
						value=ColorNormaliser.normalise(value);
						if (!Validators.color(value)) {
							throw new UserInputValidationParseException(key+" must be a COLOR (in LSL format, HTML format, or a simple word, you "+"entered '"+value+"')",true);
						}
						// does it have lsl surrounds?
						break;
					default:
						throw new SystemImplementationException("No validator defined for KV type "+definition.type()+" in "+key);
				}
			}
			if (definition.type()==COLOR) {
				if (!value.contains("<") || !value.contains(">")) { value="<"+value+">"; }
			}
		}
		purgeCache(dbo);
		dbo.setKV(this,key,value);
		purgeCache(dbo);
		definition.callOnUpdate(this,dbo,value);
		// push to all, unless we're modifying ourselves, then we'll be picked up on the outbound.
		// TO DO - review all this stuff
		if (pushUpdate) {
			if (dbo instanceof Char) {
				((Char)dbo).considerPushingConveyances();
			} else {
				getInstance().pushConveyances();
			}
		}
	}

	@Nonnull
	public State simulate(@Nullable Char c) {
		final State simulated=new State();
		simulated.setInstance(getInstance());
		if (c==null) { c=getCharacterNullable(); }
		simulated.setCharacter(c);
		final Set<Region> possibleRegions=Region.getRegions(this,false);
		final Region simulatedRegion=new ArrayList<>(possibleRegions).get((int) (Math.floor(Math.random()*possibleRegions.size())));
		simulated.setRegion(simulatedRegion);
		final Set<Zone> possibleZones=simulatedRegion.getZones();
		if (!possibleZones.isEmpty()) {
			simulated.zone=new ArrayList<>(possibleZones).get((int) (Math.floor(Math.random()*possibleZones.size())));
		}
		return simulated;
	}

	public void purgeAttributeCache() { attributes=null; }

	/**
	 * Get attributes for an instance.
	 *
	 * @return Set of ALL character attributes, not just writable ones in the DB...
	 */
	@Nonnull
	public Set<Attribute> getAttributes() {
		if (attributes!=null) { return attributes; }
		attributes=new TreeSet<>();
		final Set<Attribute> db=Attribute.getAttributes(this);
		attributes.addAll(db);
		for (final Module module: Modules.getModules()) {
			//System.out.println("checking module "+module.getName());
			if (module.isEnabled(this)) {
				//System.out.println("is enabled true "+module.getName());
				attributes.addAll(module.getAttributes(this));
			}
		}
		return attributes;
	}

	@Nonnull
	public String toString() {
		final StringBuilder ret=new StringBuilder();
		if (instance!=null) {
			if (!ret.isEmpty()) {
				ret.append(", ");
			}
			ret.append("Instance:").append(instance.getName());
		}
		if (region!=null) {
			if (!ret.isEmpty()) {
				ret.append(", ");
			}
			ret.append("Region:").append(region.getName());
		}
		if (zone!=null) {
			if (!ret.isEmpty()) {
				ret.append(", ");
			}
			ret.append("Zone:").append(zone.getName());
		}
		if (character!=null) {
			for (final CharacterGroup c: CharacterGroup.getGroups(character)) {
				if (!ret.isEmpty()) {
					ret.append(", ");
				}
				ret.append("Group:").append(c.getName());
			}
			if (!ret.isEmpty()) {
				ret.append(", ");
			}
			ret.append("Char:").append(character.getName());
		}
		return ret.toString();
	}

	public void fleshOut() {
		// attempt to figure out some kinda completion for avatar/instance/char
		if (avatar==null && instance==null && character==null) { return; } //meh, nothing to work with
		if (avatar!=null && instance!=null && character!=null) { return; } //heh, the opposite, nothing to do
		// well not sure there's a general solution so
		if (avatar==null) {
			if (character==null) {
				//noinspection ConstantConditions,StatementWithEmptyBody
				if (instance==null) {
					// NO avatar, NO character, NO instance
					// pointless
				}
				else {
					// NO avatar, NO character, YES instance
					// unworkable combo
				}
			}
			else {
				if (instance==null) {
					// NO avatar, YES character, NO instance
					instance=character.getInstance();
				}
				else {
					// NO avatar, YES character, YES instance
					character.validate(this);
				}
				avatar=character.getOwner();
				updateCookie();
			}
		}
		else {
			if (character==null) {
				if (instance==null) {
					// YES avatar, NO character, NO instance
					character=Char.getMostRecent(avatar);
					if (character!=null) { instance=character.getInstance(); }
				}
				else {
					// YES avatar, NO character, YES instance
					character=Char.getMostRecent(avatar,instance);
				}
				updateCookie();
			}
			else {
				//noinspection ConstantConditions,StatementWithEmptyBody
				if (instance==null) {
					// YES avatar, YES character, NO instance
					instance=character.getInstance();
					updateCookie();
				}
				else {
					// YES avatar, YES character, YES instance
				}
			}
		}

	}

	@Nonnull
	public Form form() {
		if (form==null) { throw new SystemConsistencyException("Getting form but no form defined?"); }
		return form;
	}

	@Nonnull
	public User getSourceDeveloper() {
		if (sourceDeveloper ==null) { throw new SystemConsistencyException("There is no source developer!"); }
		return sourceDeveloper;
	}

	public void setSourceDeveloper(@Nullable final User sourceDeveloper) {
		this.sourceDeveloper =sourceDeveloper;
	}

	@Nullable
	public User getSourceDeveloperNullable() {
		return sourceDeveloper;
	}

	public void setForm(@Nonnull final Form form) {
		this.form=form;
	}

	@Nonnull
	public JSONObject json() {
		if (json==null) { throw new SystemConsistencyException("JSON is null when retrieved"); }
		return json;
	}

	@Nullable
	public JSONObject jsonNullable() {
		return json;
	}

	public void setJson(@Nullable final JSONObject json) {
		// logger().warning("Explicitly settings STATE JSON to null, even though it defaults to it");
		this.json=json;
	}

	@Nonnull
	public User getSourceOwner() {
		if (sourceOwner ==null) { throw new SystemConsistencyException("There is no source owner?"); }
		return sourceOwner;
	}

	public void setSourceOwner(@Nullable final User sourceOwner) {
		this.sourceOwner =sourceOwner;
	}

	@Nullable
	public User getSourceOwnerNullable() {
		return sourceOwner;
	}

	@Nonnull
	public HttpRequest req() {
		if (req==null) { throw new SystemImplementationException("There is no HTTP Request object"); }
		return req;
	}

	public void req(@Nullable final HttpRequest req) {
		this.req=req;
	}

	@Nullable
	public String getSourceNameNullable() { return sourceName; }

	@Nonnull
	public String getSourceName() {
		if (sourceName ==null) { throw new SystemImplementationException("Source name is null"); }
		return sourceName;
	}

	public void setSourceName(@Nullable final String sourceName) {
		this.sourceName =sourceName;
	}

	@Nonnull
	public String address() {
		return getClientIP();
	}

	@Nonnull
	public Cookie cookie() {
		if (cookie==null) { throw new SystemImplementationException("Cookies are null"); }
		return cookie;
	}

	public void cookie(@Nullable final Cookie cookie) {
		this.cookie=cookie;
	}

	public boolean hasCookie() {
		return cookie != null;
	}

	@Nonnull
	public String callBackURL() {
		if (callBackURL ==null) { throw new SystemImplementationException("Callback URL is null"); }
		return callBackURL;
	}

	@Nullable
	public String callBackURLNullable() {
		return callBackURL;
	}

	public void callBackURL(@Nullable final String callBackURL) {
		this.callBackURL =callBackURL;
	}

	@Nonnull
	public SafeMap postMap() {
		if (postMap ==null) { throw new SystemImplementationException("Post map is null"); }
		return postMap;
	}

	public void postMap(@Nullable final SafeMap postMap) {
		this.postMap =postMap;
	}

	public void elevate(final boolean elevate) { elevated=elevate; }

	public boolean elevated() { return elevated; }

	public Map<String,String> getTemplates() {
		return Templater.getTemplates(this);
	}

	public String getClientIP() {
		if (req==null) { throw new SystemConsistencyException("There is no request object to getClientIP()"); }
		return SL.getClientIP(req,context);
	}

	// ----- Internal Instance -----
	@Nonnull
	@Override
	protected String dumpAdditionalStateToHtml() {
		return "";
	}

	private void populateSuperUser() { if (superuser==null && avatar!=null) { superuser=avatar.isSuperAdmin(); } }

	private void preparePermissionsCache() {
		if (permissionsCache ==null && avatar!=null && instance!=null) {
			permissionsCache =new HashSet<>();
			permissionsCache.addAll(PermissionsGroup.getPermissions(instance,avatar));
		}
	}

	private void prepareInstanceOwner() {
		if (instanceOwner ==null && instance!=null && avatar!=null) {
			instanceOwner =instance.getOwner()==avatar;
		}
	}

	private Map<String,String> getKVMap(@Nonnull final TableRow dbo) {
		if (!kvMaps.containsKey(dbo)) {
			kvMaps.put(dbo,dbo.loadKVs());
		}
		return kvMaps.get(dbo);
	}

	private boolean kvDefined(@Nonnull final TableRow o,
	                          @Nonnull final KV kv) {
		final Map<String,String> kvMap=getKVMap(o);
		return kvMap.containsKey(kv.fullName().toLowerCase());
	}

	private void updateCookie() {
		if (hasCookie()) {
			if (cookie().getCharacter()!=character) { cookie().setCharacter(character); }
			if (cookie().getAvatar()!=avatar) { cookie().setAvatar(getAvatar()); }
			if (cookie().getInstance()!=instance) { cookie().setInstance(instance); }
		}
	}

	@Nonnull
	public Obj getObject() {
		if (object==null) { throw new SystemImplementationException("Object data is undefined"); }
		return object;
	}

	private boolean suppressOutput;

	public void suppressOutput(final boolean template) {
		this.suppressOutput = template;
	}

	public boolean suppressOutput() {
		return suppressOutput;
	}

	public enum Sources {
		NONE,
		SYSTEM,
		USER,
		CONSOLE,
		SCRIPTING,
		OBJECT,
		EXTERNAL
	}


}
