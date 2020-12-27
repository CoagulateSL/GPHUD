package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class KV extends NameComparable {

	// ---------- INSTANCE ----------
	public abstract boolean isGenerated();

	@Nonnull
	public abstract String fullName();

	@Nonnull
	public abstract KVSCOPE scope();

	@Nonnull
	public abstract KVTYPE type();

	@Nonnull
	public abstract String description();

	@Nonnull
	public abstract String editPermission();

	public abstract String defaultValue();

	@Nonnull
	public abstract String conveyAs();

	@Nonnull
	public abstract KVHIERARCHY hierarchy();

	@Nonnull
	public String onUpdate() { return ""; }

	public abstract boolean template();

	public boolean hidden() { return false; }

	public boolean appliesTo(final TableRow o) {
		switch (scope()) {
			case CHARACTER:
				return o instanceof Char;
			case EVENT:
				return o instanceof Event;
			case INSTANCE:
				return o instanceof Instance;
			case SERVER:
				return o instanceof Instance || o instanceof Region;
			case NONSPATIAL:
				return o instanceof Instance || o instanceof CharacterGroup || o instanceof Char || o instanceof Effect;
			case SPATIAL:
				return o instanceof Instance || o instanceof Region || o instanceof Zone || o instanceof Event;
			case COMPLETE:
				// probably should just return true but...
				if (o instanceof Instance || o instanceof Region || o instanceof Zone || o instanceof Event || o instanceof CharacterGroup || o instanceof Char || o instanceof Effect) {
					return true;
				}
				throw new SystemImplementationException("KV of COMPLETE scope was passed unknown DBObject "+o.getClass().getName()+".  Ammend whitelist or debug caller.");
			case ZONE:
				return o instanceof Zone;
			case EFFECT:
				return o instanceof Effect;
			default:
				throw new SystemImplementationException("Unhandled scope "+scope());
		}
	}

	public void assertAppliesTo(final TableRow o) {
		if (!appliesTo(o)) {
			throw new UserInputStateException("Can not apply scope "+scope()+" to "+name());
		}
	}

	public void setKV(@Nonnull final State st,
	                  @Nonnull final TableRow o,
	                  final String value) {
		assertAppliesTo(o);
		st.setKV(o,name(),value);
		//convey(st,value);
	}

	@Override
	public String toString() {
		return fullName();
	}

	/**
	 * Called by State.setKV when a particular KV is updated
	 *
	 * @param state         The associated state making the KV change
	 * @param updatedObject The updated TableRow object (formerly DBObject)
	 * @param newValue      the new value written to the KV table
	 */
	public void callOnUpdate(final State state,
	                         final TableRow updatedObject,
	                         final String newValue) {
		// does this KV have an onUpdate method
		if (onUpdate().isEmpty()) { return; }
		final String targetMethod=onUpdate();
		final Matcher split=Pattern.compile("(.*)\\.([^.]*)").matcher(targetMethod);
		if (!split.matches()) { throw new SystemImplementationException("String "+targetMethod+" didn't match the regexp in callOnUpdate()"); }
		final String className=split.group(1);
		final String methodName=split.group(2);
		try {
			final Class<?> aClass=Class.forName(className);
			final Method method=aClass.getMethod(methodName,State.class,KV.class,TableRow.class,String.class);
			method.invoke(null,state,this,updatedObject,newValue);
		}
		catch (final InvocationTargetException e) {
			throw new SystemImplementationException("onUpdate target exceptioned for KV "+ fullName(),e);
		}
		catch (final NoSuchMethodException e) {
			throw new SystemImplementationException("onUpdate method "+targetMethod+" from KV "+ fullName()+" does not exist or match signature",e);
		}
		catch (final IllegalAccessException e) {
			throw new SystemImplementationException("onUpdate method "+targetMethod+" from KV "+ fullName()+" does not have correct access modifier",e);
		}
		catch (final ClassNotFoundException e) {
			throw new SystemImplementationException("onUpdate class "+className+" from KV "+ fullName()+" does not exist",e);
		}

	}

	public enum KVSCOPE {
		INSTANCE,
		SERVER,
		SPATIAL,
		NONSPATIAL,
		CHARACTER,
		ZONE,
		EVENT,
		EFFECT,
		COMPLETE
	}
    /* DEAD CODE?
    public  boolean exclusiveTo(DBObject o) {
         switch (scope()) {
            case CHARACTER:
                if (o instanceof Char) { return true; }
                return false;
            case EVENT:
                if (o instanceof Event) { return true; }
                return false;
            case INSTANCE:
                if (o instanceof Instance) { return true; }
                return false;
            case SERVER: return false;
            case SPATIAL:
                return false;
            case COMPLETE:
                return false;
            case ZONE:
                if (o instanceof Zone) { return true; }
                return false;
            default:
                throw new SystemException("Unhandled scope "+scope());
        }
    }
    */


	public enum KVTYPE {
		TEXT,
		INTEGER,
		FLOAT,
		UUID,
		BOOLEAN,
		COMMAND,
		COLOR
	}

	// Configurable THINGS
	// Characters, Events, Zones, Regions, Instances, Avatars (To be removed?), CharacterGroups (to be added)
	public enum KVHIERARCHY {
		NONE,
		DELEGATING,
		AUTHORITATIVE,
		CUMULATIVE
	}

	/**
	 * Defines a KVS element.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	@Repeatable(KVSS.class)
	public @interface KVS {
		// ---------- INSTANCE ----------
		@Nonnull String name();

		@Nonnull KVSCOPE scope();

		@Nonnull KVTYPE type();

		@Nonnull String description();

		@Nonnull String editPermission();

		@Nonnull String defaultValue();

		@Nonnull String defaultValueOSGrid() default "";

		@Nonnull String conveyAs() default "";

		@Nonnull KVHIERARCHY hierarchy() default KVHIERARCHY.NONE;

		@Nonnull String onUpdate() default "";

		boolean template();

		boolean hidden() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface KVSS {
		// ---------- INSTANCE ----------
		@Nonnull KVS[] value();
	}
}
