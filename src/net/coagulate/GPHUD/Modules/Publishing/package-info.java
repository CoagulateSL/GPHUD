@Module.ModuleDefinition(description="Allows publishing certain instance information publicly",
                         defaultDisable=true,
                         forceConfig=true)

@Permission.Permissions(description="Can configure publishing details",
                        name="Config",
                        power=Permission.POWER.LOW)

@KV.KVS(name="PublishGroups",
        defaultValue ="false",
        description="Publish group memberships",
        editPermission ="Publishing.Config",
        scope=KV.KVSCOPE.INSTANCE,
        template=false,
        type=KV.KVTYPE.BOOLEAN)

@KV.KVS(name="PublishStatus",
        defaultValue ="false",
        description="Publish instance status",
        editPermission ="Publishing.Config",
        scope=KV.KVSCOPE.INSTANCE,
        template=false,
        type=KV.KVTYPE.BOOLEAN)

@KV.KVS(name="PublishStatusAndPlayers",
        defaultValue ="false",
        description="Publish instance status with online player list",
        editPermission ="Publishing.Config",
        scope=KV.KVSCOPE.INSTANCE,
        template=false,
        type=KV.KVTYPE.BOOLEAN)

package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
