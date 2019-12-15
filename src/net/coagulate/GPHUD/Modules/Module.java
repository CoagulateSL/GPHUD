package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.Pool.Pools;
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

	final String name;
	final ModuleDefinition annotation;


	public Module(String name, ModuleDefinition annotation) {
		this.name = name;
		this.annotation = annotation;
		Modules.register(this);

	}

	protected static void checkPublicStatic(@Nonnull Method m) throws SystemException {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
		}
	}

	public void kvConfigPage(@Nonnull State st) {
		st.form().add(new TextHeader("KV Configuration for module " + getName()));
		GenericConfiguration.page(st, new SafeMap(), st.getInstance(), st.simulate(st.getCharacterNullable()), this);
	}

	public abstract boolean isGenerated();

	public String getName() { return name; }

	@Nonnull
	Response run(@Nonnull State st, String commandname, @Nonnull String[] args) throws UserException, SystemException {
		Command command = getCommand(st, commandname);
		return command.run(st, args);
	}

	@Nullable
	public abstract Set<SideSubMenu> getSideSubMenus(State st);

	@Nonnull
	public String requires(State st) { return annotation.requires(); }

	@Nullable
	public abstract URL getURL(State st, String url);

	@Nonnull
	public abstract Map<String, KV> getKVDefinitions(State st);

	public abstract KV getKVDefinition(State st, String qualifiedname);

	@Nullable
	public abstract Command getCommand(State st, String commandname);

	public abstract Pool getPool(State st, String itemname);

	public abstract Permission getPermission(State st, String itemname);

	@Nonnull
	public abstract Map<String, Pool> getPoolMap(State st);

	public abstract boolean hasPool(State st, Pools p);

	@Nonnull
	public abstract Map<String, Command> getCommands(State st);

	public boolean hasConfig(State st) { return alwaysHasConfig(); }

	public boolean dependanciesEnabled(State st) {
		if (requires(st).isEmpty()) { return true; }
		String[] deps = requires(st).split(",");
		for (String dep : deps) {
			if (!Modules.get(null, dep).isEnabled(st)) { return false; }
		}
		return true;
	}

	public boolean isEnabled(@Nullable State st) throws UserException, SystemException {
		boolean debug = false;
		if (!canDisable()) {
			return true;
		}
		if (st == null) {
			return true;
		}
		if (!dependanciesEnabled(st)) {
			return false;
		}
		String enabled = st.getKV(getName() + ".enabled").value();
		if (enabled == null || enabled.isEmpty()) {
			return !defaultDisable();
		}
		return Boolean.parseBoolean(enabled);
	}

	public abstract Map<String, Permission> getPermissions(State st);

	@Nullable
	public abstract SideMenu getSideMenu(State st);

	@Nonnull
	public abstract Set<URL> getAllContents(State st);

	@Nonnull
	public String description() { return annotation.description(); }

	public boolean canDisable() { return annotation.canDisable(); }

	public boolean defaultDisable() { return annotation.defaultDisable(); }

	protected abstract void initialiseInstance(State st);

	@Nonnull
	public Map<String, KV> getKVAppliesTo(State st, TableRow dbo) {
		Map<String, KV> fullset = getKVDefinitions(st);
		Map<String, KV> filtered = new TreeMap<>();
		for (Map.Entry<String, KV> entry : fullset.entrySet()) {
			KV v = entry.getValue();
			if (v.appliesTo(dbo)) {
				filtered.put(entry.getKey(), v);
			}
		}
		return filtered;
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

	public void validateKV(State st, @Nonnull String key) {
		if (getKVDefinitions(st).containsKey(key.toLowerCase())) {
			throw new SystemException("KV does not exist [" + key + "] in [" + this.getName() + "]");
		}
	}

	public void validatePermission(State st, @Nonnull String permission) {
		Map<String, Permission> perms = getPermissions(st);
		if (!perms.containsKey(permission.toLowerCase())) {
			throw new SystemException("Permission does not exist [" + permission + "] in [" + this.getName() + "]");
		}
	}

	public void validateCommand(State st, @Nonnull String command) {
		if (!getCommands(st).containsKey(command.toLowerCase())) {
			throw new SystemException("Command does not exist [" + command + "] in [" + this.getName() + "]");
		}
	}

	public boolean alwaysHasConfig() {
		return annotation.forceConfig();
	}

	public void addTemplateDescriptions(State st, Map<String, String> templates) {}

	public void addTemplateMethods(State st, Map<String, Method> ret) { }

	@Nonnull
	public Set<CharacterAttribute> getAttributes(State st) {
		return new TreeSet<>();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface ModuleDefinition {
		@Nonnull String description();

		boolean canDisable() default true;

		boolean defaultDisable() default false;

		@Nonnull String implementation() default "";

		boolean forceConfig() default false;

		@Nonnull String requires() default "";
	}

}
