
@ModuleDefinition(description = "Provides general Instance support and configuration elements", canDisable = false)
@Permissions(name = "OWNER", description = "Exclusive to the owner of the instance", grantable = false,power = Permission.POWER.HIGH)
@Permissions(name = "SetMOTD", description = "Allowed to set the instance Message of the Day (MOTD)",power = Permission.POWER.LOW)
@KVS(name = "MOTD", type = KVTYPE.TEXT, scope = KVSCOPE.INSTANCE, editpermission = "Instance.SetMOTD", defaultvalue = "Welcome, --AVATAR--, you are connected as --NAME--", conveyas = "motd", description = "Instance Message of the Day (MOTD)", template = true)
@KVS(name = "status", type = KVTYPE.TEXT, scope = KVSCOPE.INSTANCE, editpermission = "instance.owner", conveyas = "instancestatus", defaultvalue = "", description = "Status of the instance (do not edit, it gets overwritten)", template = false, hidden = true)
@Permissions(name = "ReceiveAdminMessages", description = "These users will receive administrative messages in addition to the instance owner",power = Permission.POWER.LOW)
@Permissions(name = "SendAdminMessages", description = "These users will be able to send administrative messages",power = Permission.POWER.LOW)
@KVS(name = "MaxCharacters", description = "Maximum number of characters an avatar might own", defaultvalue = "1", editpermission = "Instance.ConfigureCharacters", hierarchy = KVHIERARCHY.CUMULATIVE, scope = KVSCOPE.COMPLETE, template = false, type = KVTYPE.INTEGER)
@KVS(name = "CharacterSwitchEnabled", description = "Allowed to switch/create characters (consider applying to a zone)", defaultvalue = "false", editpermission = "Instance.ConfigureCharacters", hierarchy = KVHIERARCHY.DELEGATING, scope = KVSCOPE.COMPLETE, template = false, type = KVTYPE.BOOLEAN)
@KVS(name = "AutoNameCharacter", description = "Should avatars have a default character created using their avatar name (traditional behaviour)", editpermission = "Instance.ConfigureCharacters", defaultvalue = "true", hierarchy = KVHIERARCHY.NONE, scope = KVSCOPE.INSTANCE, template = false, type = KVTYPE.BOOLEAN)
@KVS(name = "ViewSelfTemplate", description = "The default template for the self-view character sheet command", editpermission = "Instance.EditCharacterSheets", hierarchy = KVHIERARCHY.DELEGATING, scope = KVSCOPE.COMPLETE, type = KVTYPE.TEXT, template = true, defaultvalue = "--NEWLINE--Character: --NAME----NEWLINE--Played by:--AVATAR--")
@KVS(name = "ViewOtherTemplate", description = "The default template for the view-other character sheet command", editpermission = "Instance.EditCharacterSheets", hierarchy = KVHIERARCHY.DELEGATING, scope = KVSCOPE.COMPLETE, type = KVTYPE.TEXT, template = true, defaultvalue = "--NEWLINE--Character: --NAME----NEWLINE--Played by:--AVATAR--")
@KVS(name = "ShowSelfTemplate", description = "The default template for the public-show character sheet command", editpermission = "Instance.EditCharacterSheets", hierarchy = KVHIERARCHY.DELEGATING, scope = KVSCOPE.COMPLETE, type = KVTYPE.TEXT, template = true, defaultvalue = "--NEWLINE--Character: --NAME----NEWLINE--Played by:--AVATAR--")
@KVS(name = "AllowSelfRetire", description = "Allow the character to retire themselves", defaultvalue = "false", editpermission = "Instance.ConfigureCharacters", hierarchy = KVHIERARCHY.DELEGATING, scope = KVSCOPE.COMPLETE, template = false, type = KVTYPE.BOOLEAN)
@Permissions(name = "EditCharacterSheets", description = "Allows the editing of the character sheet display formats",power = Permission.POWER.LOW)
@Permissions(name = "ModuleEnablement",description = "Enable or disable modules at this instance",power = Permission.POWER.MEDIUM)
@Permissions(name="ServerOperator",description="The ability to get+deploy GPHUD Region Servers",power = Permission.POWER.MEDIUM)
@Permissions(name="ConfigureCharacters",description="Alter settings related to character limits (number of, etc)",power=Permission.POWER.LOW)
@Permissions(name="ManagePermissions",description="Can create and alter permissions groups (including giving themselves permissions, essentially)",power = Permission.POWER.HIGH)

package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

