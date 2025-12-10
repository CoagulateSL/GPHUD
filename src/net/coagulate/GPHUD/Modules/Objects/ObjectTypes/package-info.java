/** This package defines various behaviours that can occur on object interactions */

@Classes.Change(date="2022-04-23",
                type=ChangeLogging.CHANGETYPE.Fix,
                component=Classes.COMPONENT.ObjectDriver,
                message="Teleporters no longer send a blank 'teleporter says' message which triggers poor HUD behaviour (says 's')")
@Classes.Change(date="2024-09-29",
                type=ChangeLogging.CHANGETYPE.Fix,
                component=Classes.COMPONENT.Core,
                message="Fixed caching issue in Object definitions (as in inworld objects) that could create a duplicate cache entry resulting in a split-brain between the website and the objects behaviour.")
		
@Classes.Change(date="2025-12-10",
                type=ChangeLogging.CHANGETYPE.Add,
                component=Classes.COMPONENT.ObjectDriver,
                message="Object driver now reports the distance to the clicking user, in scripts this is passed as the CALLERDISTANCE variable (type Float), this will be -1 if your object driver does not relay this parameter.  You may need to upgrade your Region Server which will update all your Object Drivers for this function to work.  Check by simply having a script report CALLERDISTANCE and check if it's always -1 for click type objects.")
		
package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Classes;
import net.coagulate.SL.ChangeLogging;