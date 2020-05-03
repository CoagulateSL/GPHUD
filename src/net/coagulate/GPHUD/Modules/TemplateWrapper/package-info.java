@Module.ModuleDefinition(description="Wraps templates with prefix/suffix if and only if they contain a value",
                         defaultDisable=true,
                         implementation="net.coagulate.GPHUD.Modules.TemplateWrapper.TemplateWrapper")

@Permission.Permissions(description="Ability to alter affixes",
                        name="EditAffix",
                        power=Permission.POWER.LOW)

package net.coagulate.GPHUD.Modules.TemplateWrapper;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
