package net.coagulate.GPHUD;

import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
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
	public SafeMap postmap=null;
	public String callbackurl = null;
	public Sources source = Sources.NONE;
	public HttpRequest req = null;
	public HttpResponse resp = null;
	public HttpContext context = null;
	// system interface puts input here
	public JSONObject json = null;
	// system interface sets to raw json string
	public final String jsoncommand = null;
	public InetAddress address = null;

	public Header[] headers = null;
	//requested host
	public String host = null;
	// web interface stores an error here for rendering
	public Exception exception = null;
	// web interface stores the logged in userid if applicable
	public String username = null;
	// system interface sets this if we're "runas" someone other than the owner
	public boolean issuid = false;
	// web interface logged in user ID, may be null if they cookie in as an avatar :)
	// avatar, from web interface, or second life
	public User avatar = null;
	// web interface cookie, used to logout things
	public Cookies cookie = null;
	public String cookiestring = null;
	public Form form = null;
	// system interface puts the object originating the request here
	public User sourceowner = null;
	public String sourcename = null;
	public User sourcedeveloper = null;
	public Region sourceregion = null;
	public String sourcelocation = null;

	// used by the HUD interface to stash things briefly
	public String command;
	public Zone zone = null;
	// used by the HUD interface to stash things briefly
	public boolean sendshow;
	public Integer roll = null;
	public GSVM vm;
	public Map<Integer,Set<String>> permissionsGroupCache=new HashMap<>();
	public String objectkey=null;
	public Objects object=null;

	public void flushPermissionsGroupCache() { permissionsGroupCache=new HashMap<>(); }
	Set<String> permissionscache = null;
	Set<Attribute> attributes = null;
	private String uri = null;
	private State target = null;
	private String regionname = null;
	private Region region = null;
	private Instance instance = null;
	// character
	private Char character = null;
	private Boolean superuser = null;
	private Boolean instanceowner = null;
	private final Map<TableRow, Map<String, String>> kvmaps = new HashMap<>();

	public State() {}


	public State(HttpRequest req, HttpResponse resp, HttpContext context) {
		this.req = req;
		this.resp = resp;
		this.context = context;
	}

	public State(Char c) {
		this.character = c;
		this.avatar = c.getPlayedBy();
		this.instance = c.getInstance();
		this.region = c.getRegion();
		this.zone = c.getZone();
	}

	public State(Instance i, Region r, Zone z, Char c) {
		this.instance = i;
		this.region = r;
		this.zone = z;
		this.character = c;
		this.avatar = c.getPlayedBy();
	}

	public static State getNonSpatial(Char c) {
		State ret = new State();
		ret.setInstance(c.getInstance());
		ret.setAvatar(c.getOwner());
		ret.setCharacter(c);
		return ret;
	}

	// system interface sets to "runas" - defaults to object owner but can be overridden.  THIS IS "WHO WE ARE RUNNING AS"
	// web interface sets this to the "logged in CHARACTER" object
	// system interface sets to instance

	public boolean hasModule(String module) throws UserException, SystemException {
		if (Modules.get(null, module).isEnabled(this)) { return true; }
		return false;
	}

	public Set<String> getCharacterGroupTypes() {
		final boolean debug = false;
		Set<String> types = new TreeSet<>();
		types.add("");
		for (Attribute a : getAttributes()) {
			if (debug) { System.out.println("Parsing attribute " + a.getNameSafe() + " of type " + a.getType()); }
			if (a.getType() == Attribute.ATTRIBUTETYPE.GROUP) {
				String grouptype = a.getSubType();
				if (debug) { System.out.println("Has sub type " + grouptype); }
				if (grouptype != null && !grouptype.isEmpty()) { types.add(grouptype); }
			}
		}
		return types;
	}

	public Attribute getAttributeOrException(String name) {
		Attribute a = getAttribute(name);
		if (a == null) { throw new UserException("No such character attribute '" + name + "'"); }
		return a;
	}

	public Attribute getAttribute(String name) {
		Set<Attribute> map = getAttributes();
		for (Attribute a : map) {
			if (name.equalsIgnoreCase(a.getName())) { return a; }
		}
		return null;
	}

	public Attribute getAttribute(CharacterGroup group) {
		boolean debug = false;
		String keyword = group.getType();
		if (debug) { System.out.println("getAttribute keyword is " + keyword); }
		for (Attribute attr : getAttributes()) {
			if (debug) { System.out.println("getAttribute checking attr " + attr + " of type " + attr.getType()); }
			if (attr.getType()== Attribute.ATTRIBUTETYPE.GROUP) {
				String type = attr.getSubType();
				if (debug) { System.out.println("getAttribute compare to " + type); }
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
	public String getFullURL() { return uri; }

	public String getDebasedURL() {
		if (uri == null) { return null; }
		if (uri.startsWith("/GPHUD/")) { return uri.substring(6); }
		return uri;
	}

	public String getDebasedNoQueryURL() {
		String ret = getDebasedURL();
		//System.out.println("Pre parsing:"+ret);
		if (ret.contains("?")) { ret = ret.substring(0, ret.indexOf("?")); }
		//System.out.println("Post parsing:"+ret);
		return ret;
	}

	public void setURL(String url) { uri = url; }

	public State getTarget() { return target; }

	public void setTarget(Char c) {
		target = new State(instance, region, zone, c);
	}

	public KVValue getTargetKV(String kvname) { return target.getKV(kvname); }

	/**
	 * Get the region name - this is EITHER the name of the Region object (see getRegion()) or a temporary string.
	 * The temporary string is set by setRegionName and is only used if the Region object is null (getRegion() errors, getRegionNullable() nulls).
	 * The temporary string is used during instance creation / region registration when there is no valid Region object at this point, but we need the data to create from.
	 *
	 * @return Region name (Region.getName() usually)
	 */
	public String getRegionName() {
		if (region != null) { return region.getName(); }
		// this fallback is used as a stub when we're registering a region and nothing more.
		return regionname;
	}

	public void setRegionName(String regionname) {
		this.regionname = regionname;
	}

	/**
	 * Get the region this connection is using.
	 *
	 * @return Region object
	 */
	public Region getRegion() {
		Region r = getRegionNullable();
		if (r == null) { throw new UserException("No region has been selected"); }
		return r;
	}

	public void setRegion(Region region) {
		region.validate(this);
		this.region = region;
	}

	public Region getRegionNullable() {
		if (region != null) { region.validate(this); }
		return region;
	}

	public Char getCharacter() {
		Char c = getCharacterNullable();
		if (c == null) { throw new UserException("No character is selected"); }
		return c;
	}

	public void setCharacter(Char character) {
		if (character != null) { character.validate(this); }
		this.character = character;
		if (this.character != null && this.avatar == null) { avatar = character.getPlayedBy(); }
	}

	public Char getCharacterNullable() {
		if (character != null) { character.validate(this); }
		return character;
	}

	public User getAvatar() { return avatar; }

	public void setAvatar(User avatar) {
		this.avatar = avatar;
	}

	public User avatar() { return avatar; }

	/**
	 * Return the Instance object associated with this connection.
	 *
	 * @return Instance object
	 */
	public Instance getInstance() {
		Instance i = getInstanceNullable();
		if (i == null) { throw new UserException("No instance has been selected"); }
		return i;
	}

	public void setInstance(Instance instance) {
		if (instance != null) { instance.validate(this); }
		this.instance = instance;
	}

	public Instance getInstanceNullable() {
		if (instance != null) { instance.validate(this); }
		return instance;
	}

	public String getInstanceString() {
		if (instance == null) { return "<null>"; }
		return instance.toString();
	}

	public String getInstanceAndRegionString() {
		return getInstanceString() + " @ " + getRegionName();
	}

	public String getOwnerString() {
		if (sourceowner == null) { return "<null>"; }
		return sourceowner.toString();
	}

	public String getURIString() {
		if (uri == null) { return "<null>"; }
		return uri;
	}

	public String getIdString() {
		String response = "";
		if (character != null) {
			response += character.toString();
			if (avatar != null) { response += "/Avatar:" + avatar.toString() + ""; }
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
	public boolean hasPermission(String permission) {
		if (permission == null || permission.isEmpty()) { return true; }
//        Modules.validatePermission(permission);
		if (isSuperUser()) { return true; }
		if (isInstanceOwner()) { return true; }
		preparePermissionsCache();
		if (permissionscache == null) { return false; }
		for (String check : permissionscache) { if (check.equalsIgnoreCase(permission)) { return true; } }
		return false;
	}

	/**
	 * Checks for a permission.
	 * Writes an error to the FORM if not present
	 *
	 * @param permission name of permission
	 * @return True/false
	 */
	public boolean hasPermissionOrAnnotateForm(String permission) {
		boolean haspermission = hasPermission(permission);
		if (haspermission) { return haspermission; }
		form.add(new TextError("Insufficient permissions: You require " + permission));
		return haspermission;
	}

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

	public boolean deniedPermission(String permission) throws UserException {
		if (permission == null || permission.isEmpty()) { return true; }
		if (this.hasPermission(permission)) { return false; }
		form.add(new TextError("Permission Denied!  You require permission " + permission + " to access this content"));
		return true;
	}

	public void assertPermission(String permission) throws SystemException, UserException {
		if (permission == null || permission.isEmpty()) { return; }
		if (hasPermission(permission)) { return; }
		throw new SystemException("ALERT! Permission assertion failed on permission " + permission);
	}

	private Map<String, String> getKVMap(TableRow dbo) {
		if (dbo == null) { throw new SystemException("Can not get KV map for null object"); }
		if (!kvmaps.containsKey(dbo)) {
			kvmaps.put(dbo, dbo.loadKVs());
		}
		return kvmaps.get(dbo);
	}

	private boolean kvDefined(TableRow o, KV kv) {
		if (o == null) { throw new SystemException("Can not check kv definition on a null object"); }
		Map<String, String> kvmap = getKVMap(o);
		if (kvmap.containsKey(kv.fullname().toLowerCase())) { return true; }
		return false;
	}

	public KV getKVDefinition(String kvname) {
		return Modules.getKVDefinition(this, kvname);
	}

	public List<TableRow> getTargetList(KV kv) {
		boolean debug = false;
		KV.KVSCOPE scope = kv.scope();
		// create a ordered list of all the relevant objects, wether valued or not
		List<TableRow> check = new ArrayList<>();
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
			Map<Integer, Event> eventmap = new TreeMap<>();
			for (Event e : instance.getActiveEvents()) {
				eventmap.put(e.getId(), e);
			}
			for (Integer id : eventmap.keySet()) {
				check.add(eventmap.get(id));
			}
		}
		// charactergroups in ID order
		if (scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.NONSPATIAL) {
			if (character != null) {
				Map<Integer, CharacterGroup> map = new TreeMap<>();
				for (CharacterGroup c : character.getGroups()) {
					map.put(c.getId(), c);
				}
				for (Integer id : map.keySet()) {
					check.add(map.get(id));
				}
			}
		}
		//character
		if (scope == KV.KVSCOPE.CHARACTER || scope == KV.KVSCOPE.COMPLETE || scope == KV.KVSCOPE.NONSPATIAL) {
			if (character != null) { check.add(character); }
		}
		if (debug) { System.out.println(kv.fullname() + " with scope " + kv.scope() + " returned " + check.size()); }
		return check;
	}

	// tells us which is the target from where we would derive a value.
	public TableRow determineTarget(KV kv) {
		boolean debug = false;
		if (debug) { System.out.println("Finding target for " + kv.fullname()); }
		List<TableRow> targets = getTargetList(kv);
		TableRow ret = null;
		switch (kv.hierarchy()) {
			case NONE:
				if (targets.size() > 1) {
					throw new SystemException("NONE hierarchy type returned " + targets.size() + " results... unable to compute :P");
				}
				if (targets.size() == 1) {
					ret = targets.get(0); // "the" element
				}
				if (debug) { System.out.println("Selected singular element as no hierarchy"); }
				break;
			case AUTHORITATIVE:
				// from highest to lowest, first value we find takes precedence.
				for (TableRow dbo : targets) {
					if (ret == null) { // already found? do nothing
						if (kvDefined(dbo, kv) && getKV(dbo, kv.fullname()) != null) { ret = dbo; }
					}
				}
				if (debug) { System.out.println("Selected highest auth element"); }
				break;
			case DELEGATING:
				// rather the inverse logic, just take the 'lowest' match
				for (TableRow dbo : targets) {
					if (debug) {
						System.out.println("DELEGATING - CHECKING " + dbo.getClass().getSimpleName() + "/" + dbo.getName());
						System.out.println("KVDEFINED: " + kvDefined(dbo, kv));
						System.out.println("VALUE : " + getKV(dbo, kv.fullname()));
					}
					if (kvDefined(dbo, kv) && getKV(dbo, kv.fullname()) != null) { ret = dbo; }
				}
				if (debug) { System.out.println("Selected lowest 'delegating' element"); }
				break;
			case CUMULATIVE:
				throw new SystemException("Can not determineTarget() a CUMULATIVE set, you should getTargetList(KV) it instead and sum it.  or just use getKV()");
			default:
				throw new SystemException("Unknown hierarchy type " + kv.hierarchy());
		}
		if (debug) {
			if (ret == null) { System.out.println("Selected null"); } else {
				System.out.println("Selected " + ret.getClass().getSimpleName() + "/" + ret.getName());
			}
		}
		return ret;
	}

	public KVValue getKV(String kvname) {
		try {
			String path = "";
			final boolean debug = false;
			if (kvname == null) {
				SystemException ex=new SystemException("Get KV on null K");
				logger().log(Level.WARNING,"Get KV on null K",ex);
				throw ex;
			}
			if (debug) { System.out.println("Calling get on " + kvname); }
			KV kv = getKVDefinition(kvname);
			if (kv.hierarchy() == KV.KVHIERARCHY.CUMULATIVE) {
				float sum = 0;
				boolean triggered = false;
				List<TableRow> list = getTargetList(kv);
				if (!list.isEmpty()) {
					for (TableRow dbo : getTargetList(kv)) {
						String raw = getKV(dbo, kvname);
						if (raw != null && !raw.isEmpty()) {
							sum = sum + Float.parseFloat(raw);
							triggered = true;
							if (kv.type() == KV.KVTYPE.INTEGER) {
								path = path + " +" + Integer.parseInt(raw) + " (" + dbo.getNameSafe() + ")";
							} else { path = path + " +" + Float.parseFloat(raw) + " (" + dbo.getNameSafe() + ")"; }
						}
					}
					if (triggered) {
						if (debug) {System.out.println("Cumulative return of " + sum); }
						if (kv.type() == KV.KVTYPE.INTEGER) { return new KVValue(((int) sum) + "", path); }
						return new KVValue(sum + "", path);
					}
				}
				if (debug) { System.out.println("Cumulative returning defaultvalue of " + templateDefault(kv)); }
				return new KVValue(templateDefault(kv), "Template Default");
			} else {
				TableRow target = determineTarget(kv);
				if (target == null) {
					if (debug) { System.out.println("Null target, returning default of " + templateDefault(kv)); }
					return new KVValue(templateDefault(kv), "No Target Template Default");
				}
				String value = getKV(target, kvname);
				if (value == null) {
					if (debug) {
						System.out.println("Null value from " + target.getClass().getSimpleName() + "/" + target.getName() + ", returning default of " + templateDefault(kv));
					}
					return new KVValue(templateDefault(kv), "Null Value Template Default");
				}
				if (debug) {
					System.out.println("Selected value of " + value + " from " + target.getClass().getSimpleName() + "/" + target.getName());
				}
				return new KVValue(getKV(target, kvname), "Direct value from " + target.getClass().getSimpleName() + " " + target.getNameSafe());
			}
		} catch (RuntimeException re) {
			throw new UserException("Failed to evaluate KV " + kvname + ": " + re.getLocalizedMessage(), re);
		}
	}

	public String templateDefault(KV kv) {
		String s = kv.defaultvalue();
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

	public String getRawKV(TableRow target, String kvname) {
		if (target == null) { throw new SystemException("Can not get kv " + kvname + " for null target"); }
		KV kv = getKVDefinition(kvname);
		if (kv == null) { throw new UserException("Failed to resolve " + kvname + " to a valid KV entity"); }
		return getKVMap(target).get(kvname.toLowerCase());
	}

	public String getKV(TableRow target, String kvname) {
		boolean debug = false;
		String s = getRawKV(target, kvname);
		KV kv = getKVDefinition(kvname);
		if (!kv.template()) { return s; }
		boolean evaluate = false;
		boolean isint = false;
		if (kv.type() == KV.KVTYPE.FLOAT) { evaluate = true; }
		if (kv.type() == KV.KVTYPE.INTEGER) {
			evaluate = true;
			isint = true;
		}
		if (debug) {
			System.out.println("PRE TEMPLATER : " + kvname + " = " + s + " with evaluate " + evaluate + " and isint " + isint);
		}
		String out;
		try { out = Templater.template(this, s, evaluate, isint); } catch (Exception e) {
			throw new UserException("Failed loading KV " + kvname + " for " + target.getTableName() + " " + target.getNameSafe() + " : " + e.getLocalizedMessage(), e);
		}
		if (kv.type()== COLOR) {
			while (out.startsWith("<<")) { out=out.replaceFirst("<<","<"); }
			while (out.endsWith(">>")) { out=out.replaceFirst(">>",">"); }
		}
		if (debug) { System.out.println("POST TEMPLATER : " + kvname + " = " + out); }
		return out;
	}

	public void purgeCache(TableRow dbo) { kvmaps.remove(dbo); }

	public void setKV(TableRow dbo, String key, String value) {
		if (value != null && !value.isEmpty()) {
			KV definition = this.getKVDefinition(key);
			if (!definition.template()) { // these are hard to verify :P
				switch (definition.type()) {
					case TEXT: // no checking here :P
						break;
					case INTEGER: // check it parses into an int
						try { Integer.parseInt(value); } catch (NumberFormatException e) {
							throw new UserException(key + " must be a whole number, you entered '" + value + "' (" + e.getLocalizedMessage() + ")");
						}
						break;
					case FLOAT:
						try { Float.parseFloat(value); } catch (NumberFormatException e) {
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
						try { Modules.getCommand(this, value); } catch (SystemException e) {
							throw new UserException(key + " must be an internal command, you entered '" + value + "' and it gave a weird error");
						} catch (UserException f) {
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

	public State simulate(Char c) {
		State simulated = new State();
		simulated.setInstance(getInstance());
		if (c == null) { c = getCharacterNullable(); }
		simulated.setCharacter(c);
		Set<Region> possibleregions = instance.getRegions(false);
		Region simulatedregion = new ArrayList<>(possibleregions).get((int) (Math.floor(Math.random() * possibleregions.size())));
		simulated.setRegion(simulatedregion);
		Set<Zone> possiblezones = simulatedregion.getZones();
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
	public Set<Attribute> getAttributes() {
		if (attributes != null) { return attributes; }
		attributes = new TreeSet<>();
		Set<Attribute> db = Attribute.getAttributes(this);
		attributes.addAll(db);
		for (Module module : Modules.getModules()) {
			//System.out.println("checking module "+module.getName());
			if (module.isEnabled(this)) {
				//System.out.println("is enabled true "+module.getName());
				attributes.addAll(module.getAttributes(this));
			}
		}
		return attributes;
	}

	public String toString() {
		String ret = "";
		if (instance != null) {
			if (!ret.isEmpty()) { ret += ", "; }
			ret += "Instance:" + instance.getName();
		}
		if (region != null) {
			if (!ret.isEmpty()) { ret += ", "; }
			ret += "Region:" + region.getName();
		}
		if (zone != null) {
			if (!ret.isEmpty()) { ret += ", "; }
			ret += "Zone:" + zone.getName();
		}
		if (character != null) {
			for (CharacterGroup c : character.getGroups()) {
				if (!ret.isEmpty()) { ret += ", "; }
				ret += "Group:" + c.getName();
			}
			if (!ret.isEmpty()) { ret += ", "; }
			ret += "Char:" + character.getName();
		}
		return ret;
	}

	@Override
	protected String dumpAdditionalStateToHtml() {
		if (vm!=null) {
			try {
				return vm.dumpStateToHtml();
			} catch (Throwable e) { return "Exceptioned: " + e.toString(); }
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
					return; // pointless
				} else {
					// NO avatar, NO character, YES instance
					return; // unworkable combo
				}
			} else {
				if (instance==null) {
					// NO avatar, YES character, NO instance
					instance=character.getInstance(); avatar=character.getOwner(); updateCookie(); return;
				} else {
					// NO avatar, YES character, YES instance
					character.validate(this);
					avatar=character.getOwner(); updateCookie(); return;
				}
			}
		} else {
			if (character==null) {
				if (instance==null) {
					// YES avatar, NO character, NO instance
					character=Char.getMostRecent(avatar); instance=character.getInstance(); updateCookie(); return;
				} else {
					// YES avatar, NO character, YES instance
					character=Char.getMostRecent(avatar, instance); updateCookie(); return;
				}
			} else {
				if (instance==null) {
					// YES avatar, YES character, NO instance
					instance=character.getInstance(); updateCookie(); return;
				} else {
					// YES avatar, YES character, YES instance
					return;
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


	public enum Sources {NONE, SYSTEM, USER, CONSOLE, SCRIPTING, OBJECT}
}
