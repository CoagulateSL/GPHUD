package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents a module.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Module {
	
	final String           name;
	final ModuleDefinition annotation;
	
	
	protected Module(final String name,final ModuleDefinition annotation) {
		this.name=name;
		this.annotation=annotation;
		Modules.register(this);
		
	}
	
	// ----- Internal Statics -----
	protected static void checkPublicStatic(@Nonnull final Method m) {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemImplementationException(
					"Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemImplementationException(
					"Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be public");
		}
	}
	
	// ---------- INSTANCE ----------
	public void kvConfigPage(@Nonnull final State st) {
		st.form().add(new TextHeader("KV Configuration for module "+getName()));
		GenericConfiguration.page(st,new SafeMap(),st.getInstance(),st.simulate(st.getCharacterNullable()),this);
	}
	
	public abstract boolean isGenerated();
	
	@Nonnull
	public String requires(final State st) {
		return annotation.requires();
	}
	
	@Nullable
	public abstract Set<SideSubMenu> getSideSubMenus(State st);
	
	@Nullable
	public abstract URL getURL(State st,String url);
	
	public abstract KV getKVDefinition(State st,String qualifiedname);
	
	@Nonnull
	public abstract Map<String,KV> getKVDefinitions(State st);
	
	@Nonnull
	public abstract Pool getPool(State st,String itemname);
	
	public abstract Permission getPermission(State st,String itemname);
	
	public boolean hasConfig(final State st) {
		return alwaysHasConfig();
	}
	
	public boolean dependanciesEnabled(final State st) {
		if (requires(st).isEmpty()) {
			return true;
		}
		final String[] deps=requires(st).split(",");
		for (final String dep: deps) {
			if (!Modules.get(null,dep).isEnabled(st)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isEnabled(@Nullable final State st) {
		final boolean debug=false;
		if (!canDisable()) {
			return true;
		}
		if (st==null) {
			return true;
		}
		if (!dependanciesEnabled(st)) {
			return false;
		}
		final String enabled=st.getKV(getName()+".enabled").value();
		if (enabled==null||enabled.isEmpty()) {
			return !defaultDisable();
		}
		return Boolean.parseBoolean(enabled);
	}
	
	@Nonnull
	public abstract Map<String,Pool> getPoolMap(State st);
	
	@Nonnull
	public String description() {
		return annotation.description();
	}
	
	//public abstract boolean hasPool(State st, Pools p);
	
	public boolean canDisable() {
		return annotation.canDisable();
	}
	
	public boolean defaultDisable() {
		return annotation.defaultDisable();
	}
	
	@Nonnull
	public Map<String,KV> getKVAppliesTo(final State st,final TableRow dbo) {
		final Map<String,KV> fullset=getKVDefinitions(st);
		final Map<String,KV> filtered=new TreeMap<>();
		for (final Map.Entry<String,KV> entry: fullset.entrySet()) {
			final KV v=entry.getValue();
			if (v.appliesTo(dbo)) {
				filtered.put(entry.getKey(),v);
			}
		}
		return filtered;
	}
	
	@Nullable
	public abstract SideMenu getSideMenu(State st);
	
	@Nonnull
	public abstract Set<URL> getAllContents(State st);
	
	public void validateKV(final State st,@Nonnull final String key) {
		if (getKVDefinitions(st).containsKey(key.toLowerCase())) {
			throw new SystemImplementationException("KV does not exist ["+key+"] in ["+getName()+"]");
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void validatePermission(final State st,@Nonnull final String permission) {
		if ("*".equals(permission)) { // just check the module exists here really, which we did by getting this far
			return;
		}
		final Map<String,Permission> perms=getPermissions(st);
		if (!perms.containsKey(permission.toLowerCase())) {
			throw new SystemImplementationException("Permission does not exist ["+permission+"] in ["+getName()+"]");
		}
	}
	
	/**
	 * Return a Map of lowercase string -> permission things
	 *
	 * @param st State for which to generate permissions for
	 * @return Map of lower case string permission names (not fully qualified) mapping to the permission object
	 */
	public abstract Map<String,Permission> getPermissions(State st);
	
	public void validateCommand(final State st,@Nonnull final String command) {
		if (!getCommands(st).containsKey(command.toLowerCase())) {
			throw new SystemImplementationException("Command does not exist ["+command+"] in ["+getName()+"]");
		}
	}
	
	@Nonnull
	public abstract Map<String,Command> getCommands(State st);
	
	public void addTemplateDescriptions(final State st,final Map<String,String> cumulativeMap) {
	}
    
    /*
dead code?
    public Map<String, KV> getKVExclusiveTo(State st, DBObject dbo) {
        boolean debug=false;
        Map<String, KV> fullset = getKVDefinitions(st);
        Map<String, KV> filtered = new TreeMap<>();
        for (String k : fullset.keySet()) {
            KV v = fullset.get(k);
            if (debug) { System.out.println(v.fullname()+" exclusive to "+dbo.getClass().getName()+"("+dbo.getName()+") exclusive:"+v.exclusiveTo(dbo)); }
            if (v.exclusiveTo(dbo)) {
                filtered.put(k, v);
            }
        }
        return filtered;
    }
    */
	
	public void addTemplateMethods(final State st,final Map<String,Method> cumulativeMap) {
	}
	
	public boolean alwaysHasConfig() {
		return annotation.forceConfig();
	}
	
	// ----- Internal Instance -----
	@Nonnull
	Response run(@Nonnull final State st,final String commandname,@Nonnull final String[] args) {
		final Command command=getCommand(st,commandname);
		return command.run(st,args);
	}
	
	@Nonnull
	public Command getCommand(final State st,final String commandname) {
		final Command ret=getCommandNullable(st,commandname);
		if (ret==null) {
			throw new UserInputLookupFailureException("Unable to find command "+commandname+" in module "+getName());
		}
		return ret;
	}
	
	@Nullable
	public abstract Command getCommandNullable(State st,String commandname);
	
	@Nonnull
	public Set<CharacterAttribute> getAttributes(final State st) {
		return new TreeSet<>();
	}
	
	protected abstract void initialiseInstance(State st);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface ModuleDefinition {
		// ---------- INSTANCE ----------
		@Nonnull String description();
		
		boolean canDisable() default true;
		
		boolean defaultDisable() default false;
		
		@Nonnull String implementation() default "";
		
		boolean forceConfig() default false;
		
		@Nonnull String requires() default "";
	}
	
}
