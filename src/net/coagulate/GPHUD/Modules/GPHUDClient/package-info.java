/** GPHUD Client module, contains settings and drivers for the HUD its self */

@ModuleDefinition(canDisable=false,
                  description="Provides server side support for the HUD and thin-client mode",
                  implementation="net.coagulate.GPHUD.Modules.GPHUDClient.GPHUDClientModule")

@Permissions(name="config", description="Ability to configure the GPHUD interface", power=Permission.POWER.MEDIUM)

@Permissions(name="EditHUDText", description="Ability to configure the HUD floating text", power=Permission.POWER.LOW)

@KVS(name="widthmultiplier",
     description="Width of HUD versus its height (e.g. 2x, 1x, etc)",
     defaultValue="2",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.FLOAT,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false,
     conveyAs="sizeratio")

@KVS(name="logo",
     description="Texture UUID for the HUD (right click, copy "+"asset UUID)",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     defaultValue="36c48d34-3d84-7b9a-9979-cda80cf1d96f",
     defaultValueOSGrid="9676cf69-b36c-4edc-b470-57f2ef4a9505",
     conveyAs="setlogo",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton1",
     description="Command for Quick Button 1 (Top Left)",
     defaultValue="alias.roll",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton2",
     description="Command for Quick Button 2 (Top Right)",
     defaultValue="roller.rollagainst",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton3",
     description="Command for Quick Button 3 (Middle Left)",
     defaultValue="alias.damage",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton4",
     description="Command for Quick Button 4 (Middle Right)",
     defaultValue="",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton5",
     description="Command for Quick Button 5 (Bottom Left)",
     defaultValue="characters.spendabilitypoint",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton6",
     description="Command for Quick Button 6 (Bottom Right)",
     defaultValue="gphudclient.openwebsite",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COMMAND,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton1texture",
     description="Texture for Quick Button 1 (Top Left)",
     defaultValue="4250c8ec-6dba-927b-f68f-000a456bd8ba",
     defaultValueOSGrid="62437493-4ad4-452b-a262-0e4a2d1abee4",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyAs="qb1texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton2texture",
     description="Texture for Quick Button 2 (Top Right)",
     defaultValue="eab5cd3c-ac2e-290b-df46-a53c9114f610",
     defaultValueOSGrid="f502f651-9717-4609-8d3c-f1172ede1d24",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyAs="qb2texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton3texture",
     description="Texture for Quick Button 3 (Middle Left)",
     defaultValue="d41ccbd1-1144-3788-14cc-5fc26f3da905",
     defaultValueOSGrid="bbbb744a-f506-4609-922c-fca24bc35e69",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyAs="qb3texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton4texture",
     description="Texture for Quick Button 4 (Middle Right)",
     defaultValue="5748decc-f629-461c-9a36-a35a221fe21f",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyAs="qb4texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton5texture",
     description="Texture for Quick Button 5 (Bottom Left)",
     defaultValue="ffdaa452-d5cd-0203-de84-4f814732cff0",
     defaultValueOSGrid="9428a5a1-b7c0-4113-bf4c-93874415a7f3",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyAs="qb5texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="quickbutton6texture",
     description="Texture for Quick Button 6 (Bottom Right)",
     defaultValue="b2aedfae-8401-441e-d9d1-b5b330bce411",
     defaultValueOSGrid="594c8d90-5b51-4da4-b92a-9c75e3f47f41",
     editPermission="gphudclient.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.UUID,
     conveyAs="qb6texture",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="HudText",
     defaultValue="--NAME--",
     description="Floating text to show above the HUD",
     editPermission="GPHUDClient.EditHUDText",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyAs="hudtext")

@KVS(name="HudTextColor",
     defaultValue="<0.5,1.0,0.5>",
     description="Color for the HUD floating text (LSL color vector)",
     editPermission="GPHUDClient.EditHUDText",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COLOR,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyAs="hudcolor")

@KVS(name="TitlerText",
     defaultValue="--NAME----NEWLINE----FACTION--",
     description="Content of the titler text, see Templates",
     editPermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.TEXT,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyAs="titlertext")

@KVS(name="TitlerColor",
     defaultValue="<1.0,1.0,1.0>",
     description="Color for the titler text",
     editPermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.COLOR,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=true,
     conveyAs="titlercolor")

@KVS(name="RpChannel",
     defaultValue="2",
     conveyAs="rpchannel",
     description="RP Channel for proxying chat as character",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=true,
     type=KVTYPE.INTEGER,
     editPermission="GPHUDClient.Config")

@KVS(name="NamesLessRPPrefix",
     defaultValue="",
     conveyAs="namelessprefix",
     editPermission="GPHUDClient.Config",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.NONSPATIAL,
     type=KVTYPE.TEXT,
     template=false,
     description="If chat on the RP channel starts with this character, the name is not emitted at the start of the output.  Space to disable")


@KVS(name="Name",
     defaultValue="--NAME--",
     description="Conveys the character name to the HUD for the RP Channel's use",
     conveyAs="name",
     editPermission="instance.owner",
     hidden=true,
     hierarchy=KVHIERARCHY.NONE,
     scope=KVSCOPE.CHARACTER,
     template=true,
     type=KVTYPE.TEXT)

@KVS(name="TitlerAltitude",
     defaultValue="0.19",
     conveyAs="titlerz",
     description="Titler Altitude (height above character in meters)",
     editPermission="gphudclient.config",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     template=false,
     type=KVTYPE.FLOAT)

@KVS(name="UIXMenus",
     type=KVTYPE.BOOLEAN,
     description="Use the HUD UIX panel for menu rendering",
     defaultValue="false",
     editPermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     hierarchy=KVHIERARCHY.DELEGATING,
     conveyAs="uixmenus",
     template=false)

@KVS(name="UIXBalance",
     type=KVTYPE.BOOLEAN,
     description="Balance the quick buttons three either side of the main HUD",
     defaultValue="false",
     editPermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     hierarchy=KVHIERARCHY.DELEGATING,
     conveyAs="qbbalance",
     template=false)

@KVS(name="OpenListener",
     type=KVTYPE.BOOLEAN,
     description="Open's the HUD's command listener on /1 to listen to objects owned by the wearer too",
     defaultValue="false",
     editPermission="GPHUDClient.Config",
     scope=KVSCOPE.COMPLETE,
     hierarchy=KVHIERARCHY.DELEGATING,
     conveyAs="opencmd",
     template=false)

@KVS(name="AllowManualByDefault",
     defaultValue="true",
     editPermission="GPHUDClient.Config",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false,
     description="If enabled, allows manual selection on default 'character input' prompts from scripting",
     scope=KVSCOPE.INSTANCE,
     type=KVTYPE.BOOLEAN)

@KVS(name="TitlerAttachment",
     defaultValue="Head",
     description="Attachment location for the Titler, can be set to 'None' to disable the Titler entirely",
     type=KVTYPE.TEXT,
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     editPermission="GPHUDClient.Config",
     template=false)

@KVS(name="TitlerAttachmentConverted",
     defaultValue="--TITLERATTACHPOINT--",
     description="Converted attachment point as the HUD needs it - do not change!",
     type=KVTYPE.TEXT,
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.INSTANCE,
     conveyAs="titler",
     editPermission="User.SuperAdmin",
     template=true)

@Change(date="2022-04-23",
        type=CHANGETYPE.Delete,
        component=COMPONENT.HUD,
        message="Removed HUD's personal version startup message") @Change(date="2022-04-23",
                                                                          type=CHANGETYPE.Change,
                                                                          component=COMPONENT.HUD,
                                                                          message="Truncated general version startup messages")

@Change(date="2024-10-06",
        type=CHANGETYPE.Fix,
        component=COMPONENT.HUD,
        message="Resolved a race condition on reboot calls to the HUD, e.g. GPHUDClient.Reboot, where the HUD would reconnect instantly and may end up logging its self out.  HUD now waits for logout to complete before reconnecting.  <b>Requires region server update.</b>")

@Change(date="2024-10-06",
        type=CHANGETYPE.Delete,
        component=COMPONENT.API,
        message="Removed console and scripting access to GPHUDClient.offerWebsite - this command only ever works if the user directly calls this from the HUD")

@Change(date="2024-10-06",
        type=CHANGETYPE.Add,
        component=COMPONENT.Scripting,
        message="Added gsOfferWebsite which properly formate a GPHUDClient.offerWebsite response and forwards it to the player's HUD")
		
package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Classes.COMPONENT;
import net.coagulate.GPHUD.Classes.Change;
import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.SL.ChangeLogging.CHANGETYPE;


