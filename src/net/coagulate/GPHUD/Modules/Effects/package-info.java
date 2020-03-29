@Module.ModuleDefinition(defaultDisable=true, description="Support character effects, e.g. buffs/debuffs", forceConfig=true,
                         implementation="net.coagulate.GPHUD.Modules.Effects.EffectsModule")
@Permission.Permissions(name="Edit", description="Permission to edit effects", power=Permission.POWER.LOW)
@Permission.Permissions(name="Create", description="Permission to create effects", power=Permission.POWER.LOW)
@Permission.Permissions(name="Delete", description="Permission to delete effects", power=Permission.POWER.MEDIUM)
@Permission.Permissions(name="Apply", description="Permission to administratively (as an avatar) apply effects to a character",power=Permission.POWER.LOW)
@Permission.Permissions(name="Remove", description="Permission to administratively (as an avatar) remove an effect from a character",power=Permission.POWER.LOW)
@KV.KVS(name="ApplyMessage", description="Message to send to the character when they gain this effect", defaultvalue="", editpermission="Effects.Edit", type=KV.KVTYPE.TEXT, template=true, scope=KV.KVSCOPE.EFFECT, hierarchy=KV.KVHIERARCHY.NONE)
@KV.KVS(name="RemoveMessage", description="Message to send to the character when they lose this effect", defaultvalue="", editpermission="Effects.Edit", type=KV.KVTYPE.TEXT, template=true, scope=KV.KVSCOPE.EFFECT, hierarchy=KV.KVHIERARCHY.NONE)
package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
