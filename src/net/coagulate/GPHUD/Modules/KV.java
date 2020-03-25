package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.lang.annotation.*;
import java.util.Set;

import static java.util.logging.Level.FINE;

/**
 * Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class KV extends NameComparable {

	public abstract boolean isGenerated();

	@Nonnull
	public abstract String fullname();

	@Nonnull
	public abstract KVSCOPE scope();

	@Nonnull
	public abstract KVTYPE type();

	@Nonnull
	public abstract String description();

	@Nonnull
	public abstract String editpermission();

	public abstract String defaultvalue();

	@Nonnull
	public abstract String conveyas();

	@Nonnull
	public abstract KVHIERARCHY hierarchy();

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
		convey(st,value);
	}

	public void convey(@Nonnull final State st,
	                   final String value) {
		if (!conveyas().isEmpty()) {
			final Set<Region> regions=st.getInstance().getRegions(false);
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

	public enum KVTYPE {
		TEXT,
		INTEGER,
		FLOAT,
		UUID,
		BOOLEAN,
		COMMAND,
		COLOR
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
		@Nonnull String name();

		@Nonnull KVSCOPE scope();

		@Nonnull KVTYPE type();

		@Nonnull String description();

		@Nonnull String editpermission();

		@Nonnull String defaultvalue();

		@Nonnull String conveyas() default "";

		@Nonnull KVHIERARCHY hierarchy() default KVHIERARCHY.NONE;

		boolean template();

		boolean hidden() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface KVSS {
		@Nonnull KVS[] value();
	}

	@Override
	public String toString() {
		return fullname();
	}
}
