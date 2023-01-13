package net.coagulate.GPHUD;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.util.TreeMap;

/**
 * A map that never returns nulls.
 *
 * @author iain
 */
@SuppressWarnings("MethodOverloadsMethodOfSuperclass")
public class SafeMap extends TreeMap<String,String> {
	@Serial private static final long serialVersionUID=1L;
	
	// ---------- INSTANCE ----------
	public boolean submit() {
		return ("Submit".equals(get("Submit")));
	}
	
	@Nonnull
	public String get(final String key) {
		return nonull(super.get(key));
	}
	
	// ----- Internal Statics -----
	@Nonnull
	private static String nonull(@Nullable final String s) {
		if (s==null) {
			return "";
		}
		return s;
	}
	
	public void debugDump() {
		for (final String k: keySet()) {
			System.out.println("DEBUG DUMP SAFEMAP: "+k+"="+get(k));
		}
	}
}
