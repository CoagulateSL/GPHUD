/** Defines the basic modules framework and datastructures used by modules */

@Change(date="2022-04-25",
        type=CHANGETYPE.Add,
        component=COMPONENT.Core,
        message="Support for JSON enumerating permissions groups, allowing commands like PermissionsGroups.Invite to be used on a HUD menu")
		
@Change(date="2025-11-12",
        type=CHANGETYPE.Fix,
        component=COMPONENT.Core,
        message="Return zero when attempting to turn a null or blank KV into an integer, rather than erroring")

package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.Classes.COMPONENT;
import net.coagulate.GPHUD.Classes.Change;
import net.coagulate.SL.ChangeLogging.CHANGETYPE;