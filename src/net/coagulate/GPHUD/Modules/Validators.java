package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.GPHUD;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Validators {
	public static boolean uuid(@Nonnull String value) {
		// something like 8dc52677-bea8-4fc3-b69b-21c5e2224306
		if (value.length() != "8dc52677-bea8-4fc3-b69b-21c5e2224306".length()) {
			GPHUD.getLogger("Validation").fine(value + " failed length check as UUID");
			return false;
		}
		if (!"-".equals(value.substring(8, 9))) {
			GPHUD.getLogger("Validation").fine(value + " no dash at pos 8");
			return false;
		}
		if (!"-".equals(value.substring(13, 14))) {
			GPHUD.getLogger("Validation").fine(value + " no dash at pos 13");
			return false;
		}
		if (!"-".equals(value.substring(18, 19))) {
			GPHUD.getLogger("Validation").fine(value + " no dash at pos 18");
			return false;
		}
		if (!"-".equals(value.substring(23, 24))) {
			GPHUD.getLogger("Validation").fine(value + " no dash at pos 23");
			return false;
		}
		if (!value.replaceAll("[0-9a-fA-F-]", "").isEmpty()) {
			GPHUD.getLogger("Validation").fine(value + " not 0-9a-f-, left '" + value.replaceAll("[0-9a-fA-F-]", "") + "'");
			return false;
		}
		return true;
	}

	public static boolean color(@Nonnull String value) {
		// e.g. <1,0.5,1>
		String[] parts = value.split(",");
		if (parts.length != 3) {
			GPHUD.getLogger("Validation").fine(value + " not 3 parts (commas)");
			return false;
		}
		for (int i = 0; i < 3; i++) { parts[i] = parts[i].trim(); }
		// first and last may contain <> surround
		if (parts[0].startsWith("<")) { parts[0] = parts[0].substring(1); }
		if (parts[2].endsWith(">")) { parts[2] = parts[2].substring(0, parts[2].length() - 1); }
		for (int i = 0; i < 3; i++) { parts[i] = parts[i].trim(); }
		// each part should parse to a float :P
		for (int i = 0; i < 3; i++) {
			try {
				float f = Float.parseFloat(parts[i]);
				if (f < 0 || f > 1) {
					GPHUD.getLogger("Validation").fine(value + " failed float range 0<=x<=1 for " + parts[i]);
					return false;
				}
			} catch (NumberFormatException e) {
				GPHUD.getLogger("Validation").fine(value + " failed float parse for " + parts[i]);
				return false;
			}
		}
		return true;
	}

}
