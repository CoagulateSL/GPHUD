@Module.ModuleDefinition(description="Allows publishing certain instance information publicly", defaultDisable=true, forceConfig=true) @Permission.Permissions(description="Can configure publishing details", name="Config", power=Permission.POWER.LOW) @KV.KVS(name="PublishGroups", defaultvalue="false", description="Publish group memberships", editpermission="Publishing.Config", scope=KV.KVSCOPE.INSTANCE, template=false, type=KV.KVTYPE.BOOLEAN) @KV.KVS(name="PublishStatus", defaultvalue="false", description="Publish instance status", editpermission="Publishing.Config", scope=KV.KVSCOPE.INSTANCE, template=false, type=KV.KVTYPE.BOOLEAN) @KV.KVS(name="PublishStatusAndPlayers", defaultvalue="false", description="Publish instance status with online player list", editpermission="Publishing.Config", scope=KV.KVSCOPE.INSTANCE, template=false, type=KV.KVTYPE.BOOLEAN)

package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
