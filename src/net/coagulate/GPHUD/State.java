package net.coagulate.GPHUD;

import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Data.Objects;
import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.SL.Data.User;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
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

	// map of post values read in the user interface
	@Nullable
	public SafeMap postmap;
	@Nullable
	public String callbackurl;
	@Nonnull
	public Sources source = Sources.NONE;
	@Nullable
	public HttpRequest req;
	@Nullable
	public HttpResponse resp;
	@Nullable
	public HttpContext context;
	// system interface puts input here
	@Nullable
	private JSONObject json;
	// system interface sets to raw json string
	@Nullable
	public final String jsoncommand = null;
	@Nullable
	public InetAddress address;

	@Nullable
	public Header[] headers;
	//requested host
	@Nullable
	public String host;
	// web interface stores an error here for rendering
	@Nullable
	public Exception exception;
	// web interface stores the logged in userid if applicable
	@Nullable
	public String username;
	// system interface sets this if we're "runas" someone other than the owner
	public boolean issuid;
	// web interface logged in user ID, may be null if they cookie in as an avatar :)
	// avatar, from web interface, or second life
	@Nullable
	public User avatar;
	// web interface cookie, used to logout things
	@Nullable
	public Cookies cookie;
	@Nullable
	public String cookiestring;
	@Nullable
	private Form form;
	// system interface puts the object originating the request here
	@Nullable
	private User sourceowner;
	@Nullable
	public String sourcename;
	@Nullable
	private User sourcedeveloper;
	@Nullable
	public Region sourceregion;
	@Nullable
	public String sourcelocation;

	// used by the HUD interface to stash things briefly
	public String command;
	@Nullable
	public Zone zone;
	// used by the HUD interface to stash things briefly
	public boolean sendshow;
	@Nullable
	public Integer roll;
	@Nullable
	public GSVM vm;
	@Nonnull
	public Map<Integer,Set<String>> permissionsGroupCache=new HashMap<>();
	@Nullable
	public String objectkey;
	@Nullable
	public Objects object;

	public void flushPermissionsGroupCache() { permissionsGroupCache=new HashMap<>(); }
	@Nullable
	Set<String> permissionscache;
	@Nullable
	Set<Attribute> attributes;
	@Nullable
	private String uri;
	@Nullable
	private State target;
	@Nullable
	private String regionname;
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
	private Boolean instanceowner;
	private final Map<TableRow, Map<String, String>> kvmaps = new HashMap<>();

	public State() {}


	public State(@Nullable final HttpRequest req, @Nullable final HttpResponse resp, @Nullable final HttpContext context) {
		this.req = req;
		this.resp = resp;
		this.context = context;
	}

	public State(@Nonnull final Char c) {
		character = c;
		avatar = c.getPlayedBy();
		instance = c.getInstance();
		region = c.getRegion();
		zone = c.getZone();
	}

	public State(@Nullable final Instance i, @Nullable final Region r, @Nullable final Zone z, @Nonnull final Char c) {
		instance = i;
		region = r;
		zone = z;
		character = c;
		avatar = c.getPlayedBy();
	}

	@Nonnull
	public static State getNonSpatial(@Nonnull final Char c) {
		final State ret = new State();
		ret.setInstance(c.getInstance());
		ret.setAvatar(c.getOwner());
		ret.setCharacter(c);
		return ret;
	}

	// system interface sets to "runas" - defaults to object owner but can be overridden.  THIS IS "WHO WE ARE RUNNING AS"
	// web interface sets this to the "logged in CHARACTER" object
	// system interface sets to instance

	public boolean hasModule(@Nonnull final String module) throws UserException, SystemException {
		if (Modules.get(null, module).isEnabled(this)) { return true; }
		return false;
	}

	@Nonnull
	public Set<String> getCharacterGroupTypes() {
		final boolean debug = false;
		final Set<String> types = new TreeSet<>();
		types.add("");
		for (final Attribute a : getAttributes()) {
			if (a.getType() == Attribute.ATTRIBUTETYPE.GROUP) {
				final String grouptype = a.getSubType();
				if (grouptype != null && !grouptype.isEmpty()) { types.add(grouptype); }
			}
		}
		return types;
	}

	@Nullable
	public Attribute getAttributeOrException(@Nonnull final String name) {
		final Attribute a = getAttribute(name);
		if (a == null) { throw new UserException("No such character attribute '" + name + "'"); }
		return a;
	}

	@Nullable
	public Attribute getAttribute(@Nonnull final String name) {
		final Set<Attribute> map = getAttributes();
		for (final Attribute a : map) {
			if (name.equalsIgnoreCase(a.getName())) { return a; }
		}
		return null;
	}

	@Nullable
	public Attribute getAttribute(@Nonnull final CharacterGroup group) {
		final boolean debug = false;
		final String keyword = group.getType();
		for (final Attribute attr : getAttributes()) {
			if (attr.getType()== Attribute.ATTRIBUTETYPE.GROUP) {
				final String type = attr.getSubType();
				if (type.equals(keyword)) {
					return attr;
				}
			}
		}
		return null;
	}

	public Logger logger() {
		String subspace = getInstanceString();
		if (avatar != null) { subspace += "." + avatar.getName(); }
		if (character != null) { subspace += "." + character.getNameSafe(); }
		return GPHUD.getLogger(subspace);
	}

	// requested uri
	@Nonnull
	public String getFullURL() {
		if (uri==null) { throw new SystemException("Attempted to get URI but it's null?"); }
		return uri;
	}

	@Nonnull
	public String getDebasedURL() {
		if (uri == null) { throw new SystemException("Attempted to get URI but it's null?"); }
		if (uri.startsWith("/GPHUD/")) { return uri.substring(6); }
		return uri;
	}

	@Nonnull
	public String getDebasedNoQueryURL() {
		String ret = getDebasedURL();
		//System.out.println("Pre parsing:"+ret);
		if (ret.contains("?")) { ret = ret.substring(0, ret.indexOf("?")); }
		//System.out.println("Post parsing:"+ret);
		return ret;
	}

	public void setURL(final String url) { uri = url; }

	@Nullable
	public State getTargetNullable() { return target; }

	@Nonnull
	public State getTarget() {
		if (target==null) { throw new UserException("There is no selected target"); }
		return target;
	}

	public void setTarget(@Nonnull final Char c) {
		target = new State(instance, region, zone, c);
	}

	@Nonnull
	public KVValue getTargetKV(final String kvname) { return target.getKV(kvname); }

	/**
	 * Get the region name - this is EITHER the name of the Region object (see getRegion()) or a temporary string.
	 * The temporary string is set by setRegionName and is only used if the Region object is null (getRegion() errors, getRegionNullable() nulls).
	 * The temporary string is used during instance creation / region registration when there is no valid Region object at this point, but we need the data to create from.
	 *
	 * @return Region name (Region.getName() usually)
	 */
	@Nullable
	public String getRegionName() {
		if (region != null) { return region.getName(); }
		// this fallback is used as a stub when we're registering a region and nothing more.
		return regionname;
	}

	public void setRegionName(final String regionname) {
		this.regionname = regionname;
	}

	/**
	 * Get the region this connection is using.
	 *
	 * @return Region object
	 */
	@Nonnull
	public Region getRegion() {
		final Region r = getRegionNullable();
		if (r == null) { throw new UserException("No region has been selected"); }
		return r;
	}

	public void setRegion(@Nonnull final Region region) {
		region.validate(this);
		this.region = region;
	}

	@Nullable
	public Region getRegionNullable() {
		if (region != null) { region.validate(this); }
		return region;
	}

	@Nonnull
	public Char getCharacter() {
		final Char c = getCharacterNullable();
		if (c == null) { throw new UserException("No character is selected"); }
		return c;
	}

	public void setCharacter(@Nullable final Char character) {
		if (character != null) { character.validate(this); }
		this.character = character;
		if (this.character != null && avatar == null) { avatar = character.getPlayedBy(); }
	}

	@Nullable
	public Char getCharacterNullable() {
		if (character != null) { character.validate(this); }
		return character;
	}

	@Nullable
	public User getAvatarNullable() { return avatar; }

	@Nonnull
	public User getAvatar() {
		if (avatar==null) { throw new UserException("There is no logged in avatar"); }
		return avatar;
	}

	public void setAvatar(@Nullable final User avatar) {
		this.avatar = avatar;
	}

	/**
	 * Return the Instance object associated with this connection.
	 *
	 * @return Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		final Instance i = getInstanceNullable();
		if (i == null) { throw new UserException("No instance has been selected"); }
		return i;
	}

	public void setInstance(@Nullable final Instance instance) {
		if (instance != null) { instance.validate(this); }
		this.instance = instance;
	}

	@Nullable
	public Instance getInstanceNullable() {
		if (instance != null) { instance.validate(this); }
		return instance;
	}

	@Nonnull
	public String getInstanceString() {
		if (instance == null) { return "<null>"; }
		return instance.toString();
	}

	@Nullable
	public String getInstanceAndRegionString() {
		return getInstanceString() + " @ " + getRegionName();
	}

	@Nonnull
	public String getOwnerString() {
		if (getSourceowner() == null) { return "<null>"; }
		return getSourceowner().toString();
	}

	@Nullable
	public String getURIString() {
		if (uri == null) { return "<null>"; }
		return uri;
	}

	@Nonnull
	public String getIdString() {
		String response = "";
		if (character != null) {
			response += character.toString();
			if (avatar != null) { response += "/Avatar:" + avatar + ""; }
			return response;
		}
		if (avatar != null) { return avatar.toString(); }
		return "NOID#?";
	}

	public void flushSuperUser() { superuser = null; }

	private void populateSuperUser() { if (superuser == null && avatar != null) { superuser = avatar.isSuperAdmin(); } }

	public boolean isSuperUser() {
		populateSuperUser();
		if (superuser == null) { return false; }
		return superuser;
	}

	public void flushPermissionsCache() { permissionscache = null; }

	private void preparePermissionsCache() {
		if (permissionscache == null && avatar != null && instance != null) {
			permissionscache = new HashSet<>();
			permissionscache.addAll(Permissions.getPermissions(instance, avatar));
		}
	}

	/**
	 * Checks, and caches, if a user has a permission.
	 * Note this assumes superuser is always allowed, as is instance owner.
	 * DO NOT USE THIS TO PROTECT SUPERUSER ONLY OPERATIONS IN ANY WAY, INSTANCE OWNERS CAN ALWAYS DO EVERYTHING THE PERMISSION SYSTEM ALLOWS.
	 *
	 * @param permission Permission string to check
	 * @return true/false
	 */
	public boolean hasPermission(@Nullable final String permission) {
		if (permission == null || permission.isEmpty()) { return true; }
//        Modules.validatePermission(permission);
		if (isSuperUser()) { return true; }
		if (isInstanceOwner()) { return true; }
		preparePermissionsCache();
		if (permissionscache == null) { return false; }
		for (final String check : permissionscache) { if (check.equalsIgnoreCase(permission)) { return true; } }
		return false;
	}

	/**
	 * Checks for a permission.
	 * Writes an error to the FORM if not present
	 *
	 * @param permission name of permission
	 * @return True/false
	 */
	public boolean hasPermissionOrAnnotateForm(final String permission) {
		final boolean haspermission = hasPermission(permission);
		if (haspermission) { return haspermission; }
		form().add(new TextError("Insufficient permissions: You require " + permission));
		return haspermission;
	}

	@Nullable
	public Set<String> getPermissions() {
		preparePermissionsCache();
		return permissionscache;
	}

	public void flushInstanceOwner() { instanceowner = null; }

	private void prepareInstanceOwner() {
		if (instanceowner == null && instance != null && avatar != null) {
			instanceowner = instance.getOwner() == avatar;
		}
	}

	public boolean isInstanceOwner() {
		prepareInstanceOwner();
		if (instanceowner == null) { return false; }
		return instanceowner;
	}

	public boolean deniedPermission(@Nullable final String permission) throws UserException {
		if (permission == null || permission.isEmpty()) { return true; }
		if (hasPermission(permission)) { return false; }
		form().add(new TextError("Permission Denied!  You require permission " + permission + " to access this content"));
		return true;
	}

	public void assertPermission(@Nullable final String permission) throws SystemException, UserException {
		if (permission == null || permission.isEmpty()) { return; }
		if (hasPermission(permission)) { return; }
		throw new SystemException("ALERT! Permission assertion failed on permission " + permission);
	}

	private Map<String, String> getKVMap(@Nullable final TableRow dbo) {
		if (dbo == null) { throw new SystemException("Can not get KV map for null object"); }
		if (!kvmaps.containsKey(dbo)) {
			kvmaps.put(dbo, dbo.loadKVs());
		}
		return kvmaps.get(dbo);
	}

	private boolean kvDefined(@Nullable final TableRow o, @Nonnull final KV kv) {
		if (o == null) { throw new SystemException("Can not check kv definition on a null object"); }
		final Map<String, String> kvmap = getKVMap(o);
		if (kvmap.containsKey(kv.fullname().toLowerCase())) { return true; }
		return false;
	}

	@Nonnull
	public KV getKVDefinition(final String kvname) {
		return Modules.getKVDefinition(this, kvname);
	}

	@Nonnull
	public List<TableRow> getTargetList(@Nonnull final KV kv) {
		final boolean debug = false;
		final KV.KVSCOPE scope = kv.scope();
		// create a ordered list of all the relevant objects, wether valued or not
		final List<TableRow> check = new ArrayList<>();
		// in DELEGATING order
		if (scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.SERVER || scope == KV.KVSCOPE.SPATIAL || scope == KV.KVSCOPE.INSTANCE || scope == KV.KVSCOPE.NONSPATIAL) {
			if (instance != null) { check.add(instance); }
		}
		if (scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.SERVER || scope == KV.KVSCOPE.SPATIAL) {
			if (region != null) { check.add(region); }
		}
		if (scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.SPATIAL || scope == KV.KVSCOPE.ZONE) {
			if (zone != null) { check.add(zone); }
		}
		// events in ID order
		if (scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.SPATIAL || scope == KV.KVSCOPE.EVENT) {
			final Map<Integer, Event> eventmap = new TreeMap<>();
			for (final Event e : instance.getActiveEvents()) {
				eventmap.put(e.getId(), e);
			}
			check.addAll(eventmap.values());
		}
		// charactergroups in ID order
		if (scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.NONSPATIAL) {
			if (character != null) {
				final Map<Integer, CharacterGroup> map = new TreeMap<>();
				for (final CharacterGroup c : character.getGroups()) {
					map.put(c.getId(), c);
				}
				check.addAll(map.values());
			}
		}
		//character
		if (scope == KV.KVSCOPE.CHARACTER || scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.NONSPATIAL) {
			if (character != null) { check.add(character); }
		}
		return check;
	}

	// tells us which is the target from where we would derive a value.
	@Nullable
	public TableRow determineTarget(@Nonnull final KV kv) {
		final boolean debug = false;
		final List<TableRow> targets = getTargetList(kv);
		TableRow ret = null;
		switch (kv.hierarchy()) {
			case NONE:
				if (targets.size() > 1) {
					throw new SystemException("NONE hierarchy type returned " + targets.size() + " results... unable to compute :P");
				}
				if (targets.size() == 1) {
					ret = targets.get(0); // "the" element
				}
				break;
			case AUTHORITATIVE:
				// from highest to lowest, first value we find takes precedence.
				for (final TableRow dbo : targets) {
					if (ret == null) { // already found? do nothing
						if (kvDefined(dbo, kv) && getKV(dbo, kv.fullname()) != null) { ret = dbo; }
					}
				}
				break;
			case DELEGATING:
				// rather the inverse logic, just take the 'lowest' match
				for (final TableRow dbo : targets) {
					if (kvDefined(dbo, kv) && getKV(dbo, kv.fullname()) != null) { ret = dbo; }
				}
				break;
			case CUMULATIVE:
				throw new SystemException("Can not determineTarget() a CUMULATIVE set, you should getTargetList(KV) it instead and sum it.  or just use getKV()");
			default:
				throw new SystemException("Unknown hierarchy type " + kv.hierarchy());
		}
		return ret;
	}

	@Nonnull
	public KVValue getKV(@Nullable final String kvname) {
		try {
			final StringBuilder path = new StringBuilder();
			final boolean debug = false;
			if (kvname == null) {
				final SystemException ex=new SystemException("Get KV on null K");
				logger().log(Level.WARNING,"Get KV on null K",ex);
				throw ex;
			}
			final KV kv = getKVDefinition(kvname);
			if (kv.hierarchy() == KV.KVHIERARCHY.CUMULATIVE) {
				float sum = 0;
				boolean triggered = false;
				final List<TableRow> list = getTargetList(kv);
				if (!list.isEmpty()) {
					for (final TableRow dbo : getTargetList(kv)) {
						final String raw = getKV(dbo, kvname);
						if (raw != null && !raw.isEmpty()) {
							sum = sum + Float.parseFloat(raw);
							triggered = true;
							if (kv.type() == KV.KVTYPE.INTEGER) {
								path.append(" +").append(Integer.parseInt(raw)).append(" (").append(dbo.getNameSafe()).append(")");
							} else { path.append(" +").append(Float.parseFloat(raw)).append(" (").append(dbo.getNameSafe()).append(")"); }
						}
					}
					if (triggered) {
						if (kv.type() == KV.KVTYPE.INTEGER) { return new KVValue(((int) sum) + "", path.toString()); }
						return new KVValue(sum + "", path.toString());
					}
				}
				return new KVValue(templateDefault(kv), "Template Default");
			} else {
				final TableRow target = determineTarget(kv);
				if (target == null) {
					return new KVValue(templateDefault(kv), "No Target Template Default");
				}
				final String value = getKV(target, kvname);
				if (value == null) {
					return new KVValue(templateDefault(kv), "Null Value Template Default");
				}
				return new KVValue(getKV(target, kvname), "Direct value from " + target.getClass().getSimpleName() + " " + target.getNameSafe());
			}
		} catch (final RuntimeException re) {
			throw new UserException("Failed to evaluate KV " + kvname + ": " + re.getLocalizedMessage(), re);
		}
	}

	public String templateDefault(@Nonnull final KV kv) {
		final String s = kv.defaultvalue();
		if (!kv.template()) { return s; }
		boolean evaluate = false;
		boolean isint = false;
		if (kv.type() == KV.KVTYPE.FLOAT) { evaluate = true; }
		if (kv.type() == KV.KVTYPE.INTEGER) {
			evaluate = true;
			isint = true;
		}
		return Templater.template(this, s, evaluate, isint);
	}

	public String getRawKV(@Nullable final TableRow target, @Nonnull final String kvname) {
		if (target == null) { throw new SystemException("Can not get kv " + kvname + " for null target"); }
		final KV kv = getKVDefinition(kvname);
		if (kv == null) { throw new UserException("Failed to resolve " + kvname + " to a valid KV entity"); }
		return getKVMap(target).get(kvname.toLowerCase());
	}

	public String getKV(@Nonnull final TableRow target, @Nonnull final String kvname) {
		final boolean debug = false;
		final String s = getRawKV(target, kvname);
		final KV kv = getKVDefinition(kvname);
		if (!kv.template()) { return s; }
		boolean evaluate = false;
		boolean isint = false;
		if (kv.type() == KV.KVTYPE.FLOAT) { evaluate = true; }
		if (kv.type() == KV.KVTYPE.INTEGER) {
			evaluate = true;
			isint = true;
		}
		String out;
		try { out = Templater.template(this, s, evaluate, isint); } catch (final Exception e) {
			throw new UserException("Failed loading KV " + kvname + " for " + target.getTableName() + " " + target.getNameSafe() + " : " + e.getLocalizedMessage(), e);
		}
		if (kv.type()== COLOR) {
			while (out.startsWith("<<")) { out=out.replaceFirst("<<","<"); }
			while (out.endsWith(">>")) { out=out.replaceFirst(">>",">"); }
		}
		return out;
	}

	public void purgeCache(final TableRow dbo) { kvmaps.remove(dbo); }

	public void setKV(@Nonnull final TableRow dbo, @Nonnull final String key, @Nullable String value) {
		if (value != null && !value.isEmpty()) {
			final KV definition = getKVDefinition(key);
			if (!definition.template()) { // these are hard to verify :P
				switch (definition.type()) {
					case TEXT: // no checking here :P
						break;
					case INTEGER: // check it parses into an int
						try { Integer.parseInt(value); } catch (final NumberFormatException e) {
							throw new UserException(key + " must be a whole number, you entered '" + value + "' (" + e.getLocalizedMessage() + ")");
						}
						break;
					case FLOAT:
						try { Float.parseFloat(value); } catch (final NumberFormatException e) {
							throw new UserException(key + " must be a number, you entered '" + value + "' (" + e.getLocalizedMessage() + ")");
						}
						break;
					case UUID:
						if (!Validators.uuid(value)) {
							throw new UserException(key + " must be a UUID , you entered '" + value + "'");
						}
						break;
					case BOOLEAN:
						value = value.toLowerCase();
						if (!("true".equals(value) || "false".equals(value))) {
							throw new UserException(key + " must be true/false");
						}
						break;
					case COMMAND:
						try { Modules.getCommandNullable(this, value); } catch (final SystemException e) {
							throw new UserException(key + " must be an internal command, you entered '" + value + "' and it gave a weird error");
						} catch (final UserException f) {
							throw new UserException(key + " must be an internal command, you entered '" + value + "' (" + f.getLocalizedMessage() + ")");
						}
						break;
					case COLOR:
						if (!Validators.color(value)) {
							throw new UserException(key + " must be a COLOR (in LSL format, e.g. '< 1 , 0.5 , 1 >', all numbers in range 0.0-1.0, you entered '" + value + "')");
						}
						// does it have lsl surrounds?
						break;
					default:
						throw new SystemException("No validator defined for KV type " + definition.type() + " in " + key);
				}
			}
			if (definition.type() == COLOR) {
				if (!value.contains("<") || !value.contains(">")) { value = "<" + value + ">"; }
			}
		}
		purgeCache(dbo);
		dbo.setKV(this, key, value);
		purgeCache(dbo);
		// push to all, unless we're modifying ourselves, then we'll be picked up on the outbound.
		instance.pushConveyances();
	}

	@Nonnull
	public State simulate(@Nullable Char c) {
		final State simulated = new State();
		simulated.setInstance(getInstance());
		if (c == null) { c = getCharacterNullable(); }
		simulated.setCharacter(c);
		final Set<Region> possibleregions = instance.getRegions(false);
		final Region simulatedregion = new ArrayList<>(possibleregions).get((int) (Math.floor(Math.random() * possibleregions.size())));
		simulated.setRegion(simulatedregion);
		final Set<Zone> possiblezones = simulatedregion.getZones();
		if (!possiblezones.isEmpty()) {
			simulated.zone = new ArrayList<>(possiblezones).get((int) (Math.floor(Math.random() * possiblezones.size())));
		}
		return simulated;
	}

	public void purgeAttributeCache() { attributes = null; }

	/**
	 * Get attributes for an instance.
	 *
	 * @return Set of ALL character attributes, not just writable ones in the DB...
	 */
	@Nonnull
	public Set<Attribute> getAttributes() {
		if (attributes != null) { return attributes; }
		attributes = new TreeSet<>();
		final Set<Attribute> db = Attribute.getAttributes(this);
		attributes.addAll(db);
		for (final Module module : Modules.getModules()) {
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
		final StringBuilder ret = new StringBuilder();
		if (instance != null) {
			if (ret.length() > 0) { ret.append(", "); }
			ret.append("Instance:").append(instance.getName());
		}
		if (region != null) {
			if (ret.length() > 0) { ret.append(", "); }
			ret.append("Region:").append(region.getName());
		}
		if (zone != null) {
			if (ret.length() > 0) { ret.append(", "); }
			ret.append("Zone:").append(zone.getName());
		}
		if (character != null) {
			for (final CharacterGroup c : character.getGroups()) {
				if (ret.length() > 0) { ret.append(", "); }
				ret.append("Group:").append(c.getName());
			}
			if (ret.length() > 0) { ret.append(", "); }
			ret.append("Char:").append(character.getName());
		}
		return ret.toString();
	}

	@Nonnull
	@Override
	protected String dumpAdditionalStateToHtml() {
		if (vm!=null) {
			try {
				return vm.dumpStateToHtml();
			} catch (final Throwable e) { return "Exceptioned: " + e; }
		}
		return "";
	}

	public void fleshOut() {
		// attempt to figure out some kinda completion for avatar/instance/char
		if (avatar==null && instance==null && character==null) { return; } //meh, nothing to work with
		if (avatar!=null && instance!=null && character!=null) { return; } //heh, the opposite, nothing to do
		// well not sure there's a general solution so
		if (avatar==null) {
			if (character==null) {
				if (instance==null) {
					// NO avatar, NO character, NO instance
					// pointless
				} else {
					// NO avatar, NO character, YES instance
					// unworkable combo
				}
			} else {
				if (instance==null) {
					// NO avatar, YES character, NO instance
					instance=character.getInstance();
				} else {
					// NO avatar, YES character, YES instance
					character.validate(this);
				}
				avatar=character.getOwner();
				updateCookie();
			}
		} else {
			if (character==null) {
				if (instance==null) {
					// YES avatar, NO character, NO instance
					character=Char.getMostRecent(avatar); instance=character.getInstance();
				} else {
					// YES avatar, NO character, YES instance
					character=Char.getMostRecent(avatar, instance);
				}
				updateCookie();
			} else {
				if (instance==null) {
					// YES avatar, YES character, NO instance
					instance=character.getInstance(); updateCookie();
				} else {
					// YES avatar, YES character, YES instance
				}
			}
		}

	}

	private void updateCookie() {
		if (cookie!=null) {
			if (cookie.getCharacter()!=character) { cookie.setCharacter(character); }
			if (cookie.getAvatar()!=avatar) { cookie.setAvatar(avatar); }
			if (cookie.getInstance()!=instance) { cookie.setInstance(instance); }
		}
	}

	public Form form() {
		if (form==null) { throw new SystemException("Getting form but no form defined?"); }
		return form();
	}

	@Nonnull
	public User getSourcedeveloper() {
		if (sourcedeveloper==null) { throw new SystemException("There is no source developer!"); }
		return sourcedeveloper;
	}

	public void setSourcedeveloper(@Nullable final User sourcedeveloper) {
		this.sourcedeveloper = sourcedeveloper;
	}

	public void setForm(@Nonnull final Form form) {
		this.form = form;
	}

	@Nonnull
	public JSONObject json() {
		if (json==null) { throw new SystemException("JSON is null when retrieved"); }
		return json;
	}

	public void setJson(@Nullable final JSONObject json) {
		this.json = json;
	}

	@Nonnull
	public User getSourceowner() {
		if (sourceowner==null) { throw new SystemException("There is no source owner?"); }
		return sourceowner;
	}

	public void setSourceowner(@Nullable final User sourceowner) {
		this.sourceowner = sourceowner;
	}


	public enum Sources {NONE, SYSTEM, USER, CONSOLE, SCRIPTING, OBJECT}
}
