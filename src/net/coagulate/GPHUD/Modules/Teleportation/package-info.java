@Module.ModuleDefinition(description="Teleportation support for GPHUD.  Dunk your players in the lake.  Or maybe the hospital when they die.", defaultDisable=true, forceConfig=true) @Permission.Permissions(description="Allows the user to create landmarks", power=Permission.POWER.LOW, name="CreateLandmark") @Permission.Permissions(description="Allows the user to delete landmarks", power=Permission.POWER.LOW, name="DeleteLandmark")

package net.coagulate.GPHUD.Modules.Teleportation;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
