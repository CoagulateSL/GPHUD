package net.coagulate.GPHUD;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.DumpableState;
import net.coagulate.GPHUD.Data.Objects;
import net.coagulate.GPHUD.Data.*;
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

	private final Map<TableRow,Map<String,String>> kvmaps=new HashMap<>();
	@Nonnull
	public Sources source=Sources.NONE;
	@Nullable
	public HttpContext context;
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
	@Nullable
	public String cookiestring;
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
	// used by Effect to only run the expiration checker once per player request as any effects intensive stuff will spam calls to the checker
	public boolean expirationchecked;
	@Nullable
	Set<String> permissionscache;
	@Nullable
	Set<Attribute> attributes;
	// map of post values read in the user interface
	@Nullable
	private SafeMap postmap;
	@Nullable
	private String callbackurl;
	@Nullable
	private HttpRequest req;
	@Nullable
	private HttpResponse resp;
	// system interface puts input here
	@Nullable
	private JSONObject json;
	// system interface sets to raw json string
	@Nullable
	private InetAddress address;
	@Nullable
	private Header[] headers;
	// web interface cookie, used to logout things
	@Nullable
	private Cookies cookie;
	@Nullable
	private Form form;
	// system interface puts the object originating the request here
	@Nullable
	private User sourceowner;
	@Nullable
	private String sourcename;
	@Nullable
	private User sourcedeveloper;
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
	private boolean elevated;

	public State() {}

	public State(@Nullable final HttpRequest req,
	             @Nullable final HttpResponse resp,
	             @Nullable final HttpContext context) {
		req(req);
		resp(resp);
		this.context=context;
	}


	public State(@Nonnull final Char c) {
		character=c;
		avatar=c.getPlayedBy();
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
		avatar=c.getPlayedBy();
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
		if (Modules.get(null,module).isEnabled(this)) { return true; }
		return false;
	}

	@Nonnull
	public Set<String> getCharacterGroupTypes() {
		final boolean debug=false;
		final Set<String> types=new TreeSet<>();
		types.add("");
		for (final Attribute a: getAttributes()) {
			if (a.getType()==Attribute.ATTRIBUTETYPE.GROUP) {
				final String grouptype=a.getSubType();
				if (grouptype!=null && !grouptype.isEmpty()) { types.add(grouptype); }
			}
		}
		return types;
	}

	@Nullable
	public Attribute getAttributeOrException(@Nonnull final String name) {
		final Attribute a=getAttribute(name);
		if (a==null) { throw new UserInputLookupFailureException("No such character attribute '"+name+"'"); }
		return a;
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
		final boolean debug=false;
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
		if (uri==null) { throw new SystemConsistencyException("Attempted to get URI but it's null?"); }
		return uri;
	}

	@Nonnull
	public String getDebasedURL() {
		if (uri==null) { throw new SystemConsistencyException("Attempted to get URI but it's null?"); }
		if (uri.startsWith("/GPHUD/")) { return uri.substring(6); }
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

	@Nonnull
	public KVValue getTargetKV(@Nonnull final String kvname) { return getTarget().getKV(kvname); }

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
		return regionname;
	}

	public void setRegionName(final String regionname) {
		this.regionname=regionname;
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
		if (this.character!=null && avatar==null) { avatar=character.getPlayedBy(); }
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

	@Nullable
	public String getInstanceAndRegionString() {
		return getInstanceString()+" @ "+getRegionName();
	}

	@Nonnull
	public String getOwnerString() {
		return getSourceowner().toString();
	}

	@Nullable
	public String getURIString() {
		if (uri==null) { return "<null>"; }
		return uri;
	}

	@Nonnull
	public String getIdString() {
		String response="";
		if (character!=null) {
			response+=character.toString();
			if (avatar!=null) { response+="/Avatar:"+avatar+""; }
			return response;
		}
		if (avatar!=null) { return avatar.toString(); }
		return "NOID#?";
	}

	public void flushSuperUser() { superuser=null; }

	public boolean isSuperUser() {
		populateSuperUser();
		if (superuser==null) { return false; }
		return superuser;
	}

	public void flushPermissionsCache() { permissionscache=null; }

	/**
	 * Checks, and caches, if a user has a permission.
	 * Note this assumes superuser is always allowed, as is instance owner.
	 * DO NOT USE THIS TO PROTECT SUPERUSER ONLY OPERATIONS IN ANY WAY, INSTANCE OWNERS CAN ALWAYS DO EVERYTHING THE PERMISSION SYSTEM ALLOWS.
	 *
	 * @param permission Permission string to check
	 *
	 * @return true/false
	 */
	public boolean hasPermission(@Nullable final String permission) {
		if (permission==null || permission.isEmpty()) { return true; }
		//        Modules.validatePermission(permission);
		if (isSuperUser()) { return true; }
		if (isInstanceOwner()) { return true; }
		if (elevated()) { return true; }
		preparePermissionsCache();
		if (permissionscache==null) { return false; }
		for (final String check: permissionscache) { if (check.equalsIgnoreCase(permission)) { return true; } }
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
		final boolean haspermission=hasPermission(permission);
		if (haspermission) { return true; }
		form().add(new TextError("Insufficient permissions: You require "+permission));
		return false;
	}

	@Nullable
	public Set<String> getPermissions() {
		preparePermissionsCache();
		return permissionscache;
	}

	public void flushInstanceOwner() { instanceowner=null; }

	public boolean isInstanceOwner() {
		prepareInstanceOwner();
		if (instanceowner==null) { return false; }
		return instanceowner;
	}

	public boolean deniedPermission(@Nullable final String permission) {
		if (permission==null || permission.isEmpty()) { return true; }
		if (hasPermission(permission)) { return false; }
		form().add(new TextError("Permission Denied!  You require permission "+permission+" to access this content"));
		return true;
	}

	public void assertPermission(@Nullable final String permission) {
		if (permission==null || permission.isEmpty()) { return; }
		if (hasPermission(permission)) { return; }
		throw new SystemConsistencyException("ALERT! Permission assertion failed on permission "+permission);
	}

	@Nonnull
	public KV getKVDefinition(@Nonnull final String kvname) {
		return Modules.getKVDefinition(this,kvname);
	}

	@Nonnull
	public List<TableRow> getTargetList(@Nonnull final KV kv) {
		final boolean debug=false;
		final KV.KVSCOPE scope=kv.scope();
		// create a ordered list of all the relevant objects, wether valued or not
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
			final Map<Integer,Event> eventmap=new TreeMap<>();
			for (final Event e: Event.getActive(this)) {
				eventmap.put(e.getId(),e);
			}
			check.addAll(eventmap.values());
		}
		// charactergroups in ID order
		if (scope==KV.KVSCOPE.COMPLETE || scope==KV.KVSCOPE.NONSPATIAL) {
			if (character!=null) {
				final Map<Integer,CharacterGroup> map=new TreeMap<>();
				for (final CharacterGroup c: character.getGroups()) {
					map.put(c.getId(),c);
				}
				check.addAll(map.values());
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
		final boolean debug=false;
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
						if (kvDefined(dbo,kv) && getKV(dbo,kv.fullname())!=null) { ret=dbo; }
					}
				}
				break;
			case DELEGATING:
				// rather the inverse logic, just take the 'lowest' match
				for (final TableRow dbo: targets) {
					if (kvDefined(dbo,kv) && getKV(dbo,kv.fullname())!=null) { ret=dbo; }
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
	public KVValue getKV(@Nonnull final String kvname) {
		try {
			final StringBuilder path=new StringBuilder();
			final boolean debug=false;
			final KV kv=getKVDefinition(kvname);
			if (kv.hierarchy()==KV.KVHIERARCHY.CUMULATIVE) {
				float sum=0;
				boolean triggered=false;
				final List<TableRow> list=getTargetList(kv);
				if (!list.isEmpty()) {
					for (final TableRow dbo: getTargetList(kv)) {
						final String raw=getKV(dbo,kvname);
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
						if (kv.type()==KV.KVTYPE.INTEGER) { return new KVValue(((int) sum)+"",path.toString()); }
						return new KVValue(sum+"",path.toString());
					}
				}
				return new KVValue(templateDefault(kv),"Template Default");
			}
			else {
				final TableRow target=determineTarget(kv);
				if (target==null) {
					return new KVValue(templateDefault(kv),"No Target Template Default");
				}
				final String value=getKV(target,kvname);
				if (value==null) {
					return new KVValue(templateDefault(kv),"Null Value Template Default");
				}
				return new KVValue(getKV(target,kvname),"Direct value from "+target.getClass().getSimpleName()+" "+target.getNameSafe());
			}
		}
		catch (@Nonnull final RuntimeException re) {
			throw new UserConfigurationException("Failed to evaluate KV "+kvname+": "+re.getLocalizedMessage(),re);
		}
	}

	public String templateDefault(@Nonnull final KV kv) {
		final String s=kv.defaultvalue();
		if (!kv.template()) { return s; }
		boolean evaluate=false;
		boolean isint=false;
		if (kv.type()==KV.KVTYPE.FLOAT) { evaluate=true; }
		if (kv.type()==KV.KVTYPE.INTEGER) {
			evaluate=true;
			isint=true;
		}
		return Templater.template(this,s,evaluate,isint);
	}

	public String getRawKV(@Nonnull final TableRow target,
	                       @Nonnull final String kvname) {
		final KV kv=getKVDefinition(kvname);
		//if (kv == null) { throw new UserInputLookupFailureException("Failed to resolve " + kvname + " to a valid KV entity"); }
		return getKVMap(target).get(kvname.toLowerCase());
	}

	public String getKV(@Nonnull final TableRow target,
	                    @Nonnull final String kvname) {
		final boolean debug=false;
		final String s=getRawKV(target,kvname);
		final KV kv=getKVDefinition(kvname);
		if (!kv.template()) { return s; }
		boolean evaluate=false;
		boolean isint=false;
		if (kv.type()==KV.KVTYPE.FLOAT) { evaluate=true; }
		if (kv.type()==KV.KVTYPE.INTEGER) {
			evaluate=true;
			isint=true;
		}
		String out;
		try { out=Templater.template(this,s,evaluate,isint); }
		catch (@Nonnull final Exception e) {
			throw new UserConfigurationException("Failed loading KV "+kvname+" for "+target.getTableName()+" "+target.getNameSafe()+" : "+e.getLocalizedMessage(),e);
		}
		if (kv.type()==COLOR && out!=null) {
			while (out.startsWith("<<")) { out=out.replaceFirst("<<","<"); }
			while (out.endsWith(">>")) { out=out.replaceFirst(">>",">"); }
		}
		return out;
	}

	public void purgeCache(final TableRow dbo) { kvmaps.remove(dbo); }

	public void setKV(@Nonnull final TableRow dbo,
	                  @Nonnull final String key,
	                  @Nullable String value) {
		if (value!=null && !value.isEmpty()) {
			final KV definition=getKVDefinition(key);
			if (!definition.template()) { // these are hard to verify :P
				switch (definition.type()) {
					case TEXT: // no checking here :P
						break;
					case INTEGER: // check it parses into an int
						try { Integer.parseInt(value); }
						catch (@Nonnull final NumberFormatException e) {
							throw new UserInputValidationParseException(key+" must be a whole number, you entered '"+value+"' ("+e.getLocalizedMessage()+")");
						}
						break;
					case FLOAT:
						try { Float.parseFloat(value); }
						catch (@Nonnull final NumberFormatException e) {
							throw new UserInputValidationParseException(key+" must be a number, you entered '"+value+"' ("+e.getLocalizedMessage()+")");
						}
						break;
					case UUID:
						if (!Validators.uuid(value)) {
							throw new UserInputValidationParseException(key+" must be a UUID , you entered '"+value+"'");
						}
						break;
					case BOOLEAN:
						value=value.toLowerCase();
						if (!("true".equals(value) || "false".equals(value))) {
							throw new UserInputValidationParseException(key+" must be true/false");
						}
						break;
					case COMMAND:
						try { Modules.getCommandNullable(this,value); }
						catch (@Nonnull final SystemException e) {
							throw new UserInputValidationParseException(key+" must be an internal command, you entered '"+value+"' and it gave a weird error");
						}
						catch (@Nonnull final UserException f) {
							throw new UserInputValidationParseException(key+" must be an internal command, you entered '"+value+"' ("+f.getLocalizedMessage()+")");
						}
						break;
					case COLOR:
						if (!Validators.color(value)) {
							throw new UserInputValidationParseException(key+" must be a COLOR (in LSL format, e.g. '< 1 , 0.5 , 1 >', all numbers in range 0.0-1.0, you "+"entered '"+value+"')");
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
		// push to all, unless we're modifying ourselves, then we'll be picked up on the outbound.
		getInstance().pushConveyances();
	}

	@Nonnull
	public State simulate(@Nullable Char c) {
		final State simulated=new State();
		simulated.setInstance(getInstance());
		if (c==null) { c=getCharacterNullable(); }
		simulated.setCharacter(c);
		final Set<Region> possibleregions=Region.getRegions(this,false);
		final Region simulatedregion=new ArrayList<>(possibleregions).get((int) (Math.floor(Math.random()*possibleregions.size())));
		simulated.setRegion(simulatedregion);
		final Set<Zone> possiblezones=simulatedregion.getZones();
		if (!possiblezones.isEmpty()) {
			simulated.zone=new ArrayList<>(possiblezones).get((int) (Math.floor(Math.random()*possiblezones.size())));
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
			if (ret.length()>0) { ret.append(", "); }
			ret.append("Instance:").append(instance.getName());
		}
		if (region!=null) {
			if (ret.length()>0) { ret.append(", "); }
			ret.append("Region:").append(region.getName());
		}
		if (zone!=null) {
			if (ret.length()>0) { ret.append(", "); }
			ret.append("Zone:").append(zone.getName());
		}
		if (character!=null) {
			for (final CharacterGroup c: character.getGroups()) {
				if (ret.length()>0) { ret.append(", "); }
				ret.append("Group:").append(c.getName());
			}
			if (ret.length()>0) { ret.append(", "); }
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
	public User getSourcedeveloper() {
		if (sourcedeveloper==null) { throw new SystemConsistencyException("There is no source developer!"); }
		return sourcedeveloper;
	}

	public void setSourcedeveloper(@Nullable final User sourcedeveloper) {
		this.sourcedeveloper=sourcedeveloper;
	}

	@Nullable
	public User getSourcedeveloperNullable() {
		return sourcedeveloper;
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
	public User getSourceowner() {
		if (sourceowner==null) { throw new SystemConsistencyException("There is no source owner?"); }
		return sourceowner;
	}

	public void setSourceowner(@Nullable final User sourceowner) {
		this.sourceowner=sourceowner;
	}

	@Nullable
	public User getSourceownerNullable() {
		return sourceowner;
	}

	@Nonnull
	public HttpResponse resp() {
		if (resp==null) { throw new SystemImplementationException("There is no HTTP Response object"); }
		return resp;
	}

	public void resp(@Nullable final HttpResponse resp) {
		this.resp=resp;
	}

	@Nonnull
	public HttpRequest req() {
		if (req==null) { throw new SystemImplementationException("There is no HTTP Request object"); }
		return req;
	}

	public void req(@Nullable final HttpRequest req) {
		this.req=req;
	}

	@Nonnull
	public Header[] headers() {
		if (headers==null) { throw new SystemImplementationException("Headers are null"); }
		return headers;
	}

	public void headers(@Nullable final Header[] headers) {
		this.headers=headers;
	}

	@Nullable
	public String getSourcenameNullable() { return sourcename; }

	@Nonnull
	public String getSourcename() {
		if (sourcename==null) { throw new SystemImplementationException("Source name is null"); }
		return sourcename;
	}

	public void setSourcename(@Nullable final String sourcename) {
		this.sourcename=sourcename;
	}

	@Nonnull
	public InetAddress address() {
		if (address==null) { throw new SystemImplementationException("Remote host address is null"); }
		return address;
	}

	public void address(@Nullable final InetAddress address) {
		this.address=address;
	}

	@Nonnull
	public Cookies cookie() {
		if (cookie==null) { throw new SystemImplementationException("Cookies are null"); }
		return cookie;
	}

	public void cookie(@Nullable final Cookies cookie) {
		this.cookie=cookie;
	}

	public boolean hasCookie() {
		if (cookie==null) { return false; }
		return true;
	}

	@Nonnull
	public String callbackurl() {
		if (callbackurl==null) { throw new SystemImplementationException("Callback URL is null"); }
		return callbackurl;
	}

	@Nullable
	public String callbackurlNullable() {
		return callbackurl;
	}

	public void callbackurl(@Nullable final String callbackurl) {
		this.callbackurl=callbackurl;
	}

	@Nonnull
	public SafeMap postmap() {
		if (postmap==null) { throw new SystemImplementationException("Post map is null"); }
		return postmap;
	}

	public void postmap(@Nullable final SafeMap postmap) {
		this.postmap=postmap;
	}

	public void elevate(final boolean elevate) { elevated=elevate; }

	public boolean elevated() { return elevated; }

	public Map<String,String> getTemplates() {
		return Templater.getTemplates(this);
	}

	// ----- Internal Instance -----
	@Nonnull
	@Override
	protected String dumpAdditionalStateToHtml() {
		if (vm!=null) {
			try {
				return vm.dumpStateToHtml();
			}
			catch (@Nonnull final Throwable e) { return "Exceptioned: "+e; }
		}
		return "";
	}

	private void populateSuperUser() { if (superuser==null && avatar!=null) { superuser=avatar.isSuperAdmin(); } }

	private void preparePermissionsCache() {
		if (permissionscache==null && avatar!=null && instance!=null) {
			permissionscache=new HashSet<>();
			permissionscache.addAll(PermissionsGroup.getPermissions(instance,avatar));
		}
	}

	private void prepareInstanceOwner() {
		if (instanceowner==null && instance!=null && avatar!=null) {
			instanceowner=instance.getOwner()==avatar;
		}
	}

	private Map<String,String> getKVMap(@Nonnull final TableRow dbo) {
		if (!kvmaps.containsKey(dbo)) {
			kvmaps.put(dbo,dbo.loadKVs());
		}
		return kvmaps.get(dbo);
	}

	private boolean kvDefined(@Nonnull final TableRow o,
	                          @Nonnull final KV kv) {
		final Map<String,String> kvmap=getKVMap(o);
		if (kvmap.containsKey(kv.fullname().toLowerCase())) { return true; }
		return false;
	}

	private void updateCookie() {
		if (hasCookie()) {
			if (cookie().getCharacter()!=character) { cookie().setCharacter(character); }
			if (cookie().getAvatar()!=avatar) { cookie().setAvatar(getAvatar()); }
			if (cookie().getInstance()!=instance) { cookie().setInstance(instance); }
		}
	}

	public enum Sources {
		NONE,
		SYSTEM,
		USER,
		CONSOLE,
		SCRIPTING,
		OBJECT
	}


}
