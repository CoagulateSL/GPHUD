@ModuleDefinition(description="Provides general Instance support and configuration elements",
                  canDisable=false)

@Permissions(name="OWNER",
             description="Exclusive to the owner of the instance",
             grantable=false,
             power=Permission.POWER.HIGH)

@Permissions(name="SetMOTD",
             description="Allowed to set the instance Message of the Day (MOTD)",
             power=Permission.POWER.LOW)

@KVS(name="MOTD",
     type=KVTYPE.TEXT,
     scope=KVSCOPE.INSTANCE,
     editPermission ="Instance.SetMOTD",
     defaultValue ="Welcome, --AVATAR--, you are connected as --NAME--",
     conveyAs ="motd",
     description="Instance Message of the Day (MOTD)",
     template=true)

@KVS(name="status",
     type=KVTYPE.TEXT,
     scope=KVSCOPE.INSTANCE,
     editPermission ="instance.owner",
     conveyAs ="instancestatus",
     defaultValue ="",
     description="Status of the instance (do not edit, it gets overwritten)",
     template=false,
     hidden=true)

@Permissions(name="ReceiveAdminMessages",
             description="These users will receive administrative messages in addition to the instance owner",
             power=Permission.POWER.LOW)

@Permissions(name="SendAdminMessages",
             description="These users will be able to send administrative messages",
             power=Permission.POWER.LOW)

@KVS(name="MaxCharacters",
     description="Maximum number of characters an avatar might own",
     defaultValue ="1",
     editPermission ="Instance.ConfigureCharacters",
     hierarchy=KVHIERARCHY.CUMULATIVE,
     scope=KVSCOPE.COMPLETE,
     template=false,
     type=KVTYPE.INTEGER)

@KVS(name="CharacterSwitchEnabled",
     description="Allowed to switch/create characters (consider applying to a zone)",
     defaultValue ="false",
     editPermission ="Instance.ConfigureCharacters",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=false,
     type=KVTYPE.BOOLEAN)

@KVS(name="AutoNameCharacter",
     description="Should avatars have a default character created using their avatar name (traditional behaviour)",
     editPermission ="Instance.ConfigureCharacters",
     defaultValue ="true",
     hierarchy=KVHIERARCHY.NONE,
     scope=KVSCOPE.INSTANCE,
     template=false,
     type=KVTYPE.BOOLEAN)

@KVS(name="ViewSelfTemplate",
     description="The default template for the self-view character sheet command",
     editPermission ="Instance.EditCharacterSheets",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     template=true,
     defaultValue ="--NEWLINE--Character: --NAME----NEWLINE--Played by:--AVATAR--")

@KVS(name="ViewOtherTemplate",
     description="The default template for the view-other character sheet command",
     editPermission ="Instance.EditCharacterSheets",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     template=true,
     defaultValue ="--NEWLINE--Character: --NAME----NEWLINE--Played by:--AVATAR--")

@KVS(name="ShowSelfTemplate",
     description="The default template for the public-show character sheet command",
     editPermission ="Instance.EditCharacterSheets",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     template=true,
     defaultValue ="--NEWLINE--Character: --NAME----NEWLINE--Played by:--AVATAR--")

@KVS(name="CharInitScript",
     description="Script to use to initialise a character's information, leave blank to use default",
     defaultValue ="",
     editPermission ="Instance.ConfigureCharacters",
     type=KVTYPE.TEXT,
     template=false,
     scope=KVSCOPE.INSTANCE,
     hierarchy=KVHIERARCHY.NONE)

@KVS(name="AllowSelfRetire",
     description="Allow the character to retire themselves",
     defaultValue ="false",
     editPermission ="Instance.ConfigureCharacters",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=false,
     type=KVTYPE.BOOLEAN)

@KVS(name="RunOnLogin",
     description="Command user should automatically run on login",
     defaultValue ="",
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=true,
     editPermission ="Instance.SetLoginScript")

@KVS(name="AllowedNamingSymbols",
     description="What characters may be used in a character's name, A-Z, a-z and space are assumed OK",
     defaultValue ="'-,.",
     editPermission ="Instance.ConfigureCharacters",
     type=KVTYPE.TEXT,
     template=false,
     scope=KVSCOPE.INSTANCE)

@KVS(name="FilteredNamingList",
     description="Comma separated list of words user may not include in their characters name",
     scope=KVSCOPE.INSTANCE,
     template=false,
     type=KVTYPE.TEXT,
     editPermission ="Instance.ConfigureCharacters",
     defaultValue ="")

@Permissions(name="EditCharacterSheets",
             description="Allows the editing of the character sheet display formats",
             power=Permission.POWER.LOW)

@Permissions(name="ModuleEnablement",
             description="Enable or disable modules at this instance",
             power=Permission.POWER.MEDIUM)

@Permissions(name="ServerOperator",
             description="The ability to get+deploy GPHUD Region Servers",
             power=Permission.POWER.MEDIUM)

@Permissions(name="ConfigureCharacters",
             description="Alter settings related to character limits (number of, etc)",
             power=Permission.POWER.LOW)

@Permissions(name="ManagePermissions",
             description="Can create and alter permissions groups (including giving themselves permissions, essentially)",
             power=Permission.POWER.HIGH)

@Permissions(name="PermissonsMembers",
             description="Can join or remove users from permissions groups",
             power=Permission.POWER.HIGH)

@Permissions(name="CookBooks",
             description="Can run cookbooks",
             power=Permission.POWER.MEDIUM)

@Permissions(name="SetLoginScript",
             description="Can change the 'run at login' command",
             power=Permission.POWER.MEDIUM)

// a cheaty thing that belongs to Characters, really
@SideMenus(name="(including retired)",
           priority=251,
           requiresPermission="Characters.ViewAll",
           url="/characters/retiredlist?sort=Name")

package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;

