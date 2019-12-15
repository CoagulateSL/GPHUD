package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * @author Iain Price
 */
public abstract class Table extends net.coagulate.Core.Database.Table {

	@Nonnull
	@Override
	public DBConnection getDatabase() { return GPHUD.getDB(); }

	public Logger logger() { return SL.getLogger(getClass().getSimpleName()); }
}
