@ModuleDefinition(defaultDisable = true, description = "Events manager created timed/locationed events, optionally awarding events xp", forceConfig = true, requires = "Zoning", implementation = "net.coagulate.GPHUD.Modules.Events.EventsModule")
@Permissions(name = "Create", description = "Permission to create a brand new event")
@Permissions(name = "Locations", description = "Ability to add or remove locations from an event")
@Permissions(name = "Schedule", description = "Ability to update the schedule for an event")
@Permissions(name = "Messages", description = "Ability to configure messages sent out related to events")
@Permissions(name = "PerEventEventXP", description = "Manage settings on events related to awarding event xp")
@Permissions(name = "EventXP", description = "Configure global settings related to event xp")
@KVS(name = "ZoneStartMessage", editpermission = "Events.Messages", description = "Message sent to the zones the event is in when it starts", scope = KVSCOPE.EVENT, type = KVTYPE.TEXT, defaultvalue = "", template = false)
@KVS(name = "BroadcastStartMessage", editpermission = "Events.Messages", description = "Message sent to all characters when the event starts", scope = KVSCOPE.EVENT, type = KVTYPE.TEXT, defaultvalue = "", template = false)
@KVS(name = "EventEntryMessage", editpermission = "Events.Messages", description = "Message sent to people entering the event AFTER it has started", scope = KVSCOPE.EVENT, type = KVTYPE.TEXT, defaultvalue = "", template = false)
@KVS(name = "EventExitMessage", editpermission = "Events.Messages", description = "Message sent to people exiting the event AFTER it has started", scope = KVSCOPE.EVENT, type = KVTYPE.TEXT, defaultvalue = "", template = false)
@KVS(name = "ZoneStopMessage", editpermission = "Events.Messages", description = "Message sent to the zones the event is in when it stops", scope = KVSCOPE.EVENT, type = KVTYPE.TEXT, defaultvalue = "", template = false)
@KVS(name = "BroadcastStopMessage", editpermission = "Events.Messages", description = "Message sent to all characters when the event stops", scope = KVSCOPE.EVENT, type = KVTYPE.TEXT, defaultvalue = "", template = false)
@KVS(name = "ThisEventXPLimit", editpermission = "Events.PerEventEventXP", defaultvalue = "0", scope = KVSCOPE.EVENT, type = KVTYPE.INTEGER, description = "Maximum Event XP a character can receive from this event (can not exceed global Event XP limit)", template = false)
@KVS(name = "ThisEventXPMinutes", editpermission = "Events.PerEventEventXP", defaultvalue = "60", scope = KVSCOPE.EVENT, type = KVTYPE.INTEGER, description = "Number of minutes spent in the event to get a point of Event XP, up to the various limits.", template = false)
@KVS(name = "EventXPPeriod", editpermission = "Events.EventXP", scope = KVSCOPE.INSTANCE, type = KVTYPE.FLOAT, defaultvalue = "6.75", description = "Number of days over which the character can not earn more than the Event XP limit", template = false)
@KVS(name = "EventXPLimit", editpermission = "Events.EventXP", scope = KVSCOPE.INSTANCE, type = KVTYPE.INTEGER, defaultvalue = "5", description = "Maximum ammount of Event XP earnable within the period", template = false)
@Pools(name = "EventXP", description = "Experience earned through special events")
@SideMenu.SideMenus(name = "Events", priority = 60, url = "/events")
package net.coagulate.GPHUD.Modules.Events;

import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.Pool.Pools;
import net.coagulate.GPHUD.Modules.SideMenu;

