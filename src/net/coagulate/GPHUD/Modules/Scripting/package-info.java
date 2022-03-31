@Module.ModuleDefinition(description="Scripting support for GPHUD",
                         defaultDisable=true,
                         forceConfig=true,
                         implementation="net.coagulate.GPHUD.Modules.Scripting"+".ScriptingModule")

@Permission.Permissions(description="Allows the creation of scripts",
                        name="Create",
                        power=Permission.POWER.HIGH)

@Permission.Permissions(description="Allows the user to compile scripts that use privileged functions",
                        name="CompilePrivileged",
                        power=Permission.POWER.HIGH)

@Permission.Permissions(description="Allows the deletion of scripts",
                        name="Delete",
                        power=Permission.POWER.HIGH)

@Classes.Change(date = "2022-03-31",type = ChangeLogging.CHANGETYPE.Add,component = Classes.COMPONENT.GSVM,message = "Log warnings for type mismatch on variable assignment, and also assignment to a non-defined variable.  NOTE THESE WILL EVENTUALLY BE ERRORS.  Merged for review.")

package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.GPHUD.Classes;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.SL.ChangeLogging;
