package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.GPHUD.GPHUD;

/**
 *
 * @author Iain Price
 */
public abstract class Table extends net.coagulate.Core.Database.Table {

    @Override
    public DBConnection getDatabase() { return GPHUD.getDB(); }
    
}
