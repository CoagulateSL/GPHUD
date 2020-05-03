@ModuleDefinition(canDisable=false,
                  description="Provides server side support for the HUD and thin-client mode",
                  implementation="net.coagulate.GPHUD.Modules.GPHUDClient.GPHUDClientModule")

@Permissions(name="config",
             description="Ability to configure the GPHUD interface",
             power=Permission.POWER.MEDIUM)

@Permissions(name="EditHUDText",
             description="Ability to configure the HUD floating text",
             power=Permission.POWER.LOW)

@KVS(name="widthmultiplier",
     description="Width of HUD versus its height (e.g. 2x, 1x, etc)",
     defaultvalue="2",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.FLOAT,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false,
     conveyas="sizeratio")

@KVS(name="logo",
     description="Texture UUID for the HUD (right click, copy "+"asset UUID)",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     defaultvalue="36c48d34-3d84-7b9a-9979-cda80cf1d96f",
     conveyas="setlogo",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton1",
     description="Command for Quick Button 1 (Top Left)",
     defaultvalue="alias.roll",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton2",
     description="Command for Quick Button 2 (Top Right)",
     defaultvalue="roller.rollagainst",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton3",
     description="Command for Quick Button 3 (Middle Left)",
     defaultvalue="alias.damage",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton4",
     description="Command for Quick Button 4 (Middle Right)",
     defaultvalue="",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton5",
     description="Command for Quick Button 5 (Bottom Left)",
     defaultvalue="characters.spendabilitypoint",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton6",
     description="Command for Quick Button 6 (Bottom Right)",
     defaultvalue="gphudclient.openwebsite",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton1texture",
     description="Texture for Quick Button 1 (Top Left)",
     defaultvalue="4250c8ec-6dba-927b-f68f-000a456bd8ba",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyas="qb1texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton2texture",
     description="Texture for Quick Button 2 (Top Right)",
     defaultvalue="eab5cd3c-ac2e-290b-df46-a53c9114f610",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyas="qb2texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton3texture",
     description="Texture for Quick Button 3 (Middle Left)",
     defaultvalue="d41ccbd1-1144-3788-14cc-5fc26f3da905",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyas="qb3texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton4texture",
     description="Texture for Quick Button 4 (Middle Right)",
     defaultvalue="5748decc-f629-461c-9a36-a35a221fe21f",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyas="qb4texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton5texture",
     description="Texture for Quick Button 5 (Bottom Left)",
     defaultvalue="ffdaa452-d5cd-0203-de84-4f814732cff0",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyas="qb5texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton6texture",
     description="Texture for Quick Button 6 (Bottom Right)",
     defaultvalue="b2aedfae-8401-441e-d9d1-b5b330bce411",
     editpermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyas="qb6texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="HudText",
     defaultvalue="--NAME--",
     description="Floating text to show above the HUD",
     editpermission="GPHUDClient.EditHUDText",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyas="hudtext")

@KVS(name="HudTextColor",
     defaultvalue="<0.5,1.0,0.5>",
     description="Color for the HUD floating text (LSL color vector)",
     editpermission="GPHUDClient.EditHUDText",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COLOR,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyas="hudcolor")

@KVS(name="TitlerText",
     defaultvalue="--NAME----NEWLINE----FACTION--",
     description="Content of the titler text, see Templates",
     editpermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyas="titlertext")

@KVS(name="TitlerColor",
     defaultvalue="<1.0,1.0,1.0>",
     description="Color for the titler text",
     editpermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COLOR,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyas="titlercolor")

@KVS(name="RpChannel",
     defaultvalue="2",
     conveyas="rpchannel",
     description="RP Channel for proxying chat as character",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=true,
     type=KVTYPE.INTEGER,
     editpermission="GPHUDClient.Config")

@KVS(name="Name",
     defaultvalue="--NAME--",
     description="Conveys the character name to the HUD for the RP Channel's use",
     conveyas="name",
     editpermission="instance.owner",
     hidden=true,
     hierarchy=KVHIERARCHY.NONE,
     scope=KVSCOPE.CHARACTER,
     template=true,
     type=KVTYPE.TEXT)

@KVS(name="TitlerAltitude",
     defaultvalue="0.19",
     conveyas="titlerz",
     description="Titler Altitude (height above character",
     editpermission="gphudclient.config",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=false,
     type=KVTYPE.FLOAT)

@KVS(name="UIXMenus",
     type=KVTYPE.BOOLEAN,
     description="Use the HUD UIX panel for menu rendering",
     defaultvalue="false",
     editpermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     hierarchy=KVHIERARCHY.DELEGATING,
     conveyas="uixmenus",
     template=false)

@KVS(name="UIXBalance",
     type=KVTYPE.BOOLEAN,
     description="Balance the quickbuttons three either side of the main HUD",
     defaultvalue="false",
     editpermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     hierarchy=KVHIERARCHY.DELEGATING,
     conveyas="qbbalance",
     template=false)

package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

