package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.SafeMap;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.util.*;

/**
 * DateTime utilities.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class DateTime {

	@Nonnull
	private static String fromUnixTime(final int date, @Nonnull final DateFormat df) { return df.format(new Date(((long) (date)) * ((long) 1000))); }

	@Nonnull
	public static String fromUnixTime(final int date, final String timezone) {
		final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		df.setTimeZone(TimeZone.getTimeZone(timezone));
		return fromUnixTime(date, df);
	}

	@Nonnull
	public static Row inputRow(final SafeMap values) {
		throw new SystemImplementationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * Set up an input form
	 *
	 * @param prefix Prefix for the field names
	 * @param values Existing values
	 * @return appropriate table row
	 */
	@Nonnull
	public static Row inputDateTimeRow(final String prefix, @Nonnull final SafeMap values, final String defaulttimezone) {
		final Row t = new Row();
		String tz = values.get(prefix + "timezone");
		if (tz.isEmpty()) { tz = defaulttimezone; }
		t.add(prefix);
		t.add(ti(prefix, "day", 2, values));
		t.add(ti(prefix, "month", 2, values));
		t.add(ti(prefix, "year", 4, values));
		t.add(ti(prefix, "hour", 2, values));
		t.add(ti(prefix, "minute", 2, values));
		t.add(DateTime.getTimeZoneList(prefix + "timezone", tz));
		return t;
	}

	@Nonnull
	private static TextInput ti(final String prefix, final String component, final int size, @Nonnull final SafeMap values) {
		final String name = prefix + component;
		return new TextInput(name, values.get(name), size);
	}


	/**
	 * Set up a duration input form.
	 *
	 * @param prefix  Prefix for the field names
	 * @param values  Existing values
	 * @param spacers If true inserts blank cells to help line up with date input box
	 * @return Table Row
	 */
	@Nonnull
	public static Row inputIntervalRow(final String prefix, @Nonnull final SafeMap values, final boolean spacers) {
		final Row t = new Row();
		t.add(prefix);
		t.add(ti(prefix, "day", 2, values));
		if (spacers) {
			t.add("");
			t.add("");
		}
		t.add(ti(prefix, "hour", 2, values));
		t.add(ti(prefix, "minute", 2, values));
		return t;
	}

	/**
	 * Set up duration input form, without spacers.
	 *
	 * @param prefix Prefix for field names
	 * @param values Existing values
	 * @return Table Row
	 */
	@Nonnull
	public static Row inputIntervalRow(final String prefix, @Nonnull final SafeMap values) {
		return inputIntervalRow(prefix, values, false);
	}

	/**
	 * Convert the Form into a unixtime.
	 *
	 * @param prefix Prefix to extract from the value map
	 * @param values The value map
	 * @return Unix time for the input date time
	 * @throws UserException If the numbers fail to parse
	 */
	public static int outputDateTime(final String prefix, @Nonnull final SafeMap values, final String defaulttimezone) throws UserException {
		try {
			String timezone = values.get(prefix + "timezone");
			if (timezone.isEmpty()) {
				timezone = defaulttimezone;
			}
			final int day = Integer.parseInt(values.get(prefix + "day"));
			final int month = Integer.parseInt(values.get(prefix + "month"));
			final int year = Integer.parseInt(values.get(prefix + "year"));
			final int hour = Integer.parseInt(values.get(prefix + "hour"));
			final int minute = Integer.parseInt(values.get(prefix + "minute"));
			return UnixTime.create(timezone, day, month, year, hour, minute);
		} catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Failed to parse number : " + e.getMessage(), e);
		}
	}

	/**
	 * Convert the form interval into a duration (number of seconds)
	 *
	 * @param prefix Prefix to extract from the value map
	 * @param values The value map
	 * @return Number of seconds
	 * @throws UserException If the numbers fail to parse
	 */
	public static int outputInterval(final String prefix, @Nonnull final SafeMap values) throws UserException {
		try {
			int day = Integer.parseInt(values.get(prefix + "day"));
			int hour = Integer.parseInt(values.get(prefix + "hour"));
			int minute = Integer.parseInt(values.get(prefix + "minute"));

			day = day * 24 * 60 * 60;
			hour = hour * 60 * 60;
			minute = minute * 60;
			return day + hour + minute;
		} catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Failed to parse number : " + e.getMessage(), e);
		}
	}

	@Nonnull
	public static List<String> getTimeZones() {
		final String[] tzs = TimeZone.getAvailableIDs();
		final List<String> tzlist = new ArrayList<>();
		tzlist.add("America/Los_Angeles");
		tzlist.addAll(Arrays.asList(tzs));
		return tzlist;
	}

	@Nonnull
	public static DropDownList getTimeZoneList(final String name, final String value) {
		final DropDownList dropdown = new DropDownList(name);
		for (final String tz : getTimeZones()) { dropdown.add(tz); }
		dropdown.setValue(value);
		return dropdown;
	}
}

