/** This package defines various behaviours that can occur on object interactions */

@Classes.Change(date="2022-04-23",
                type=ChangeLogging.CHANGETYPE.Fix,
                component=Classes.COMPONENT.ObjectDriver,
                message="Teleporters no longer send a blank 'teleporter says' message which triggers poor HUD behaviour (says 's')")
@Classes.Change(date="2024-09-29",
                type=ChangeLogging.CHANGETYPE.Fix,
                component=Classes.COMPONENT.Core,
                message="Fixed caching issue in Object definitions (as in inworld objects) that could create a duplicate cache entry resulting in a split-brain between the website and the objects behaviour.")
package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Classes;
import net.coagulate.SL.ChangeLogging;