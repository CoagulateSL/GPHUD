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



@Classes.Change(date="2022-03-31",component = Classes.COMPONENT.GSVM,type = ChangeLogging.CHANGETYPE.Add,message="Added BCDiscard, pops a value from the stack and discards it")
@Classes.Change(date="2022-03-31",component = Classes.COMPONENT.Scripting,type = ChangeLogging.CHANGETYPE.Add,message = "New top level language construct, the DiscardExpression.  No longer must every expression have an assignment, e.g. 'Response discard=gsAPI(Stuff)', but can just be written as gsAPI(Stuff), the result will now be automatically discarded.")

package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.GPHUD.Classes;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.SL.ChangeLogging;
