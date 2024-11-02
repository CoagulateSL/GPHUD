/**
 * This package handles database and SQL abstraction for GPHUD.
 * <p>
 * GPHUD supports "SQL calling path verification" - SQL calls are *ONLY* permitted from inside the Data package of GPHUD.
 */

@Change(type=Fix,
        date="2022-04-22",
        component=Core,
        message="Status updater no longer exceptions failing to find the running version at instances with no active regions (harmless silent error)")

@Change(type=Change,
		date="2022-04-23",
		component=Core,
		message="Preserve the leveltext conveyance through the normal logon wipe, preventing the HUD from always reporting XP/level, now only reporting it at login if it has changed.")

@Change(type=Fix,
        date="2023-02-04",
        component=Core,
        message="Altered error from conflicting group memberships to properly report a fixable condition to the user")

@Change(type=Change,
        date="2023-04-03",
        component=Core,
        message="Character renames are now checked against the name filtering policy")
		
package net.coagulate.GPHUD.Data;

import static net.coagulate.GPHUD.Classes.COMPONENT.Core;
import static net.coagulate.GPHUD.Classes.Change;
import static net.coagulate.SL.ChangeLogging.CHANGETYPE.Change;
import static net.coagulate.SL.ChangeLogging.CHANGETYPE.Fix;
