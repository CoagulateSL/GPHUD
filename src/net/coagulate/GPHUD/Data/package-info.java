/**
 * This package handles database and SQL abstraction for GPHUD.
 * <p>
 * GPHUD supports "SQL calling path verification" - SQL calls are *ONLY* permitted from inside the Data package of GPHUD.
 */

@Change(type=CHANGETYPE.Fix,
        date="2022-04-22",
        component=COMPONENT.Core,
        message="Status updater no longer exceptions failing to find the running version at instances with no active regions (harmless silent error)") @Change(
		type=CHANGETYPE.Change,
		date="2022-04-23",
		component=COMPONENT.Core,
		message="Preserve the leveltext conveyance through the normal logon wipe, preventing the HUD from always reporting XP/level, now only reporting it at login if it has changed.")
package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.Classes.COMPONENT;
import net.coagulate.GPHUD.Classes.Change;
import net.coagulate.SL.ChangeLogging.CHANGETYPE;