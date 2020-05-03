@Module.ModuleDefinition(description="Allows objects to connect in and be assigned behaviour",
                         defaultDisable=true)

@Permission.Permissions(name="Connect",
                        description="Allowed to connect objects to GPHUD",
                        power=Permission.POWER.MEDIUM)

@Permission.Permissions(name="View",
                        description="Ability to view all connected objects",
                        power=Permission.POWER.LOW)

@Permission.Permissions(name="ObjectTypes",
                        description="Ability to create edit or delete object types (behaviours)",
                        power=Permission.POWER.MEDIUM)

@Permission.Permissions(name="MapObjects",
                        description="The ability to map obejcts to object types",
                        power=Permission.POWER.LOW)

@Permission.Permissions(name="RebootObjects",
                        description="The ability to reboot in-world scripted objects",
                        power=Permission.POWER.LOW)

@Permission.Permissions(name="ShutdownObjects",
                        description="The ability to shutdown in-world scripted objects",
                        power=Permission.POWER.MEDIUM)

@Permission.Permissions(name="GetDriver",
                        description="Allowed to get a copy of the Object Driver script",
                        power=Permission.POWER.MEDIUM)

package net.coagulate.GPHUD.Modules.Objects;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
