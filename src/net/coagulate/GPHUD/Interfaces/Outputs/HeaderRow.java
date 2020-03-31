package net.coagulate.GPHUD.Interfaces.Outputs;

/**
 * A row in a table that is of header type e.g. 'th'.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class HeaderRow extends Row {

	public HeaderRow() {super();}

	public HeaderRow(final Cell c) {
		super(c);
	}

	public HeaderRow(final String c) {
		super(c);
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isHeader() { return true; }

}
