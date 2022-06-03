package net.coagulate.GPHUD;

import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;

import javax.annotation.Nonnull;

public class Utils {
	// ---------- STATICS ----------

	/**
	 * Convert a string vector into an int array.
	 *
	 * @param s String vector, x,y,z format with optional angle brackets (SL format)
	 *
	 * @return 3 part int array [x,y,z]
	 */
	@Nonnull
	public static int[] parseVector(String s) {
		s=s.replaceAll("<","");
		s=s.replaceAll(">","");
		s=s.replaceAll("\\(","");
		s=s.replaceAll("\\)","");
		final String[] parts=s.split(",");
		if (parts.length!=3) {
			throw new UserInputValidationParseException("Could not decompose co-ordinates properly",true);
		}
		final int[] pos=new int[3];
		try { pos[0]=(int) Float.parseFloat(parts[0]); }
		catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Error processing X number " + parts[0] + " - " + e.getMessage(), e,true);
		}
		try { pos[1]=(int) Float.parseFloat(parts[1]); }
		catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Error processing Y number " + parts[1] + " - " + e.getMessage(), e,true);
		}
		try { pos[2]=(int) Float.parseFloat(parts[2]); }
		catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Error processing Z number " + parts[2] + " - " + e.getMessage(), e,true);
		}
		return pos;
	}
}
