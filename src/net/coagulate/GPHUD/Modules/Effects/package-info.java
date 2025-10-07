@Module.ModuleDefinition(defaultDisable=true,
                         description="Support character effects, e.g. buffs/debuffs",
                         forceConfig=true,
                         implementation="net.coagulate.GPHUD.Modules.Effects.EffectsModule")

@Permission.Permissions(name="Edit", description="Permission to edit effects", power=Permission.POWER.LOW)

@Permission.Permissions(name="Create", description="Permission to create effects", power=Permission.POWER.LOW)

@Permission.Permissions(name="Delete", description="Permission to delete effects", power=Permission.POWER.MEDIUM)

@Permission.Permissions(name="Apply",
                        description="Permission to administratively (as an avatar) apply effects to a character",
                        power=Permission.POWER.LOW)

@Permission.Permissions(name="Remove",
                        description="Permission to administratively (as an avatar) remove an effect from a character",
                        power=Permission.POWER.LOW)

@KV.KVS(name="ApplyMessage",
        description="Message to send to the character when they gain this effect",
        defaultValue="",
        editPermission="Effects.Edit",
        type=KV.KVTYPE.TEXT,
        template=true,
        scope=KV.KVSCOPE.EFFECT,
        hierarchy=KV.KVHIERARCHY.DELEGATING)

@KV.KVS(name="RemoveMessage",
        description="Message to send to the character when they lose this effect",
        defaultValue="",
        editPermission="Effects.Edit",
        type=KV.KVTYPE.TEXT,
        template=true,
        scope=KV.KVSCOPE.EFFECT,
        hierarchy=KV.KVHIERARCHY.DELEGATING)

@KV.KVS(name="EffectIcon",
        description="UUID of a texture to use to represent this effect in the HUD",
        defaultValue="b39860d0-8c5c-5d51-9dbf-3ef55dafe8a4",
        editPermission="Effects.Edit",
        template=false,
        hierarchy=KVHIERARCHY.DELEGATING,
        scope=KVSCOPE.EFFECT,
        type=KVTYPE.UUID)

@KV.KVS(name="ShowEffect",
        description="Does this effect show up on the HUD summary icons",
        defaultValue="true",
        editPermission="Effects.Edit",
        template=false,
        hierarchy=KVHIERARCHY.DELEGATING,
        scope=KVSCOPE.EFFECT,
        type=KVTYPE.BOOLEAN)
@Classes.Change(date="2025-10-07",
                type=ChangeLogging.CHANGETYPE.Fix,
                component=Classes.COMPONENT.Core,
                message="Fixed a possible error applying an effect due to non atomic SQL update (credit: ggscott@github)")
package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Classes;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.SL.ChangeLogging;
