package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.logging.Level.FINE;

/**
 * Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class KV extends NameComparable {

	// ---------- INSTANCE ----------
	public abstract boolean isGenerated();

	@Nonnull
	public abstract String fullname();

	@Nonnull
	public abstract KVSCOPE scope();

	@Nonnull
	public abstract KVTYPE type();

	@Nonnull
	public abstract String description();

	@Nonnull public abstract String editpermission();

	public abstract String defaultvalue();

	@Nonnull public abstract String conveyas();

	@Nonnull public abstract KVHIERARCHY hierarchy();

	@Nonnull public String onUpdate() { return ""; }

	public abstract boolean template();

	public boolean hidden() { return false; }

	public boolean appliesTo(final TableRow o) {
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
			case SERVER:
				if (o instanceof Instance || o instanceof Region) { return true; }
				return false;
			case NONSPATIAL:
				if (o instanceof Instance || o instanceof CharacterGroup || o instanceof Char || o instanceof Effect) { return true; }
				return false;
			case SPATIAL:
				if (o instanceof Instance || o instanceof Region || o instanceof Zone || o instanceof Event) {
					return true;
				}
				return false;
			case COMPLETE:
				// probably should just return true but...
				if (o instanceof Instance || o instanceof Region || o instanceof Zone || o instanceof Event || o instanceof CharacterGroup || o instanceof Char || o instanceof Effect) {
					return true;
				}
				throw new SystemImplementationException("KV of COMPLETE scope was passed unknown DBObject "+o.getClass().getName()+".  Ammend whitelist or debug caller.");
			case ZONE:
				if (o instanceof Zone) { return true; }
				return false;
			case EFFECT:
				if (o instanceof Effect) { return true; }
				return false;
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

	public void convey(@Nonnull final State st,
	                   final String value) {
		if (!conveyas().isEmpty()) {
			final Set<Region> regions=Region.getRegions(st,false);
			final JSONObject message=new JSONObject();
			message.put("incommand","broadcast");
			message.put(conveyas(),value);
			for (final Region r: regions) {
				st.logger().log(FINE,"Conveying to "+r+" '"+name()+"' "+conveyas()+"="+value);
				final Transmission t=new Transmission(r,message);
				t.start();
			}
		}
	}

	@Override public String toString() {
		return fullname();
	}

	/**
	 * Called by State.setKV when a particular KV is updated
	 *
	 * @param state         The associated state making the KV change
	 * @param updatedobject The updated TableRow object (formerly DBObject)
	 * @param newvalue      the new value written to the KV table
	 */
	public void callOnUpdate(State state,TableRow updatedobject,String newvalue) {
		System.out.println("CALL ON UPDATE:"+onUpdate());
		// does this KV have an onUpdate method
		if (onUpdate().isEmpty()) { return; }
		String targetmethod=onUpdate();
		Matcher split=Pattern.compile("(.*)\\.([^.]*)").matcher(targetmethod);
		if (!split.matches()) { throw new SystemImplementationException("String "+targetmethod+" didn't match the regexp in callOnUpdate()"); }
		String classname=split.group(1);
		String methodname=split.group(2);
		try {
			Class<?> clas=Class.forName(classname);
			Method method=clas.getMethod(methodname,State.class,KV.class,TableRow.class,String.class);
			method.invoke(null,state,this,updatedobject,newvalue);
		}
		catch (InvocationTargetException e) {
			throw new SystemImplementationException("onUpdate target exceptioned for KV "+fullname(),e);
		}
		catch (NoSuchMethodException e) {
			throw new SystemImplementationException("onUpdate method "+targetmethod+" from KV "+fullname()+" does not exist or match signature",e);
		}
		catch (IllegalAccessException e) {
			throw new SystemImplementationException("onUpdate method "+targetmethod+" from KV "+fullname()+" does not have correct access modifier",e);
		}
		catch (ClassNotFoundException e) {
			throw new SystemImplementationException("onUpdate class "+classname+" from KV "+fullname()+" does not exist",e);
		}

	}

	public enum KVSCOPE {
		INSTANCE,SERVER,SPATIAL,NONSPATIAL,CHARACTER,ZONE,EVENT,EFFECT,
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

		@Nonnull String editpermission();

		@Nonnull String defaultvalue();

		@Nonnull String conveyas() default "";

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
