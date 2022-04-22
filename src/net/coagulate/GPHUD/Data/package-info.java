/**
 * This package handles database and SQL abstraction for GPHUD.
 * <p>
 * GPHUD supports "SQL calling path verification" - SQL calls are *ONLY* permitted from inside the Data package of GPHUD.
 */

@Classes.Change(type=ChangeLogging.CHANGETYPE.Fix, date="2022-04-22", component=Classes.COMPONENT.Core, message="Status updater no longer exceptions failing to find the running version at instances with no active regions (harmless silent error)")
package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.Classes;
import net.coagulate.SL.ChangeLogging;