package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

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

	public abstract String fullname();

	public abstract KVSCOPE scope();

	public abstract KVTYPE type();

	public abstract String description();

	public abstract String editpermission();

	public abstract String defaultvalue();

	public abstract String conveyas();

	public abstract KVHIERARCHY hierarchy();

	public abstract boolean template();

	public boolean hidden() { return false; }

	public boolean appliesTo(TableRow o) {
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
				if (o instanceof Instance ||
						o instanceof Region) { return true; }
				return false;
			case NONSPATIAL:
				if (o instanceof Instance ||
						o instanceof CharacterGroup ||
						o instanceof Char) { return true; }
				return false;
			case SPATIAL:
				if (o instanceof Instance ||
						o instanceof Region ||
						o instanceof Zone ||
						o instanceof Event) { return true; }
				return false;
			case COMPLETE:
				// probably should just return true but...
				if (o instanceof Instance ||
						o instanceof Region ||
						o instanceof Zone ||
						o instanceof Event ||
						o instanceof CharacterGroup ||
						o instanceof Char) { return true; }
				throw new SystemException("KV of COMPLETE scope was passed unknown DBObject " + o.getClass().getName() + ".  Ammend whitelist or debug caller.");
			case ZONE:
				if (o instanceof Zone) { return true; }
				return false;
			default:
				throw new SystemException("Unhandled scope " + scope());
		}
	}

	public void assertAppliesTo(TableRow o) {
		if (!appliesTo(o)) {
			throw new UserException("Can not apply scope " + scope() + " to " + name());
		}
	}

	public void setKV(State st, TableRow o, String value) throws UserException, SystemException {
		assertAppliesTo(o);
		if (!(TableRow.class.isAssignableFrom(o.getClass()))) {
			throw new SystemException("Object " + o.getClass() + " does not extend DBOBject while setting KV " + name());
		}
		TableRow applyto = o;
		st.setKV(o, name(), value);
		convey(st, value);
	}

	public void convey(State st, String value) throws UserException, SystemException {
		if (!conveyas().isEmpty()) {
			Set<Region> regions = st.getInstance().getRegions(false);
			JSONObject message = new JSONObject();
			message.put("incommand", "broadcast");
			message.put(conveyas(), value);
			for (Region r : regions) {
				st.logger().log(FINE, "Conveying to " + r + " '" + name() + "' " + conveyas() + "=" + value);
				Transmission t = new Transmission(r, message);
				t.start();
			}
		}
	}

	public enum KVSCOPE {INSTANCE, SERVER, SPATIAL, NONSPATIAL, CHARACTER, ZONE, EVENT, COMPLETE}

	public enum KVTYPE {TEXT, INTEGER, FLOAT, UUID, BOOLEAN, COMMAND, COLOR}
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
	public enum KVHIERARCHY {NONE, DELEGATING, AUTHORITATIVE, CUMULATIVE}

	/**
	 * Defines a KVS element.
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	@Repeatable(KVSS.class)
	public @interface KVS {
		String name();

		KVSCOPE scope();

		KVTYPE type();

		String description();

		String editpermission();

		String defaultvalue();

		String conveyas() default "";

		KVHIERARCHY hierarchy() default KVHIERARCHY.NONE;

		boolean template();

		boolean hidden() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface KVSS {
		KVS[] value();
	}
}
