package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/**
 * Allows get/set of configuration values.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EditValues {
	@Commands(context = Context.AVATAR, description = "Set an instance level configuration value")
	public static Response setInstance(State st,
	                                   @Arguments(type = ArgumentType.KVLIST, description = "Key to set the value of")
			                                   String key,
	                                   @Arguments(type = ArgumentType.TEXT_ONELINE, description = "New value for the key", max = 4096)
			                                   String value) throws UserException, SystemException {
		Modules.validateKV(st, key);
		KV kv = Modules.getKVDefinition(st, key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '" + kv.editpermission() + "' necessary to set the value of " + key);
		}
		if (!kv.appliesTo(st.getInstance())) {
			return new ErrorResponse("KV " + kv.fullname() + " of scope " + kv.scope() + " does not apply to instances");
		}
		String oldvalue = st.getRawKV(st.getInstance(), key);
		st.setKV(st.getInstance(), key, value);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetInstanceKV", key, oldvalue, value, "Changed instance level configuration");
		// bit cludgy but...
		if (key.equalsIgnoreCase("GPHUDServer.AutoAttach") || key.equalsIgnoreCase("GPHUDServer.ParcelONLY")) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}
		return new OKResponse("Instance KV store has been updated");
	}

	@URLs(url = "/configuration/setinstancevalue")
	public static void setInstanceForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "configuration.setinstance", values);
	}

	@Commands(context = Context.AVATAR, description = "Set a region level configuration value")
	public static Response setRegion(State st,
	                                 @Arguments(type = ArgumentType.REGION, description = "Region to edit the key for")
			                                 Region region,
	                                 @Arguments(type = ArgumentType.KVLIST, description = "Key to set the value of")
			                                 String key,
	                                 @Arguments(type = ArgumentType.TEXT_ONELINE, description = "New value for the key", max = 4096)
			                                 String value) throws UserException, SystemException {
		region.validate(st);
		Modules.validateKV(st, key);
		KV kv = Modules.getKVDefinition(st, key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '" + kv.editpermission() + "' necessary to set the value of " + key);
		}
		if (!kv.appliesTo(region)) {
			return new ErrorResponse("KV " + kv.fullname() + " of scope " + kv.scope() + " does not apply to regions");
		}
		String oldvalue = st.getRawKV(region, key);
		st.setKV(region, key, value);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetRegionKV", region.getName() + "/" + key, oldvalue, value, "Changed region level configuration");
		// bit cludgy but...
		if (key.equalsIgnoreCase("GPHUDServer.AutoAttach") || key.equalsIgnoreCase("GPHUDServer.ParcelONLY")) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}

		return new OKResponse("Region " + region.getName() + " KV store has been updated");
	}

	@URLs(url = "/configuration/setregionvalue")
	public static void setRegionForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "configuration.setregion", values);
	}

	@Commands(context = Context.AVATAR, description = "Set a key value for a zone")
	public static Response setZone(State st,
	                               @Arguments(type = ArgumentType.ZONE, description = "Name of the zone")
			                               Zone zone,
	                               @Arguments(type = ArgumentType.KVLIST, description = "Key to set")
			                               String key,
	                               @Arguments(type = ArgumentType.TEXT_ONELINE, description = "Value to set to", max = 4096)
			                               String value) {
		zone.validate(st);
		Modules.validateKV(st, key);
		KV kv = Modules.getKVDefinition(st, key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '" + kv.editpermission() + "' necessary to set the value of " + key);
		}
		if (!kv.appliesTo(zone)) {
			return new ErrorResponse("KV " + kv.fullname() + " of scope " + kv.scope() + " does not apply to zones");
		}
		String oldvalue = st.getRawKV(zone, key);
		st.setKV(zone, key, value);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetZoneKV", zone.getName() + "/" + key, oldvalue, value, "Updated zone KV entry");
		return new OKResponse("KV Store updated for zone '" + zone.getName() + "'");
	}

	@URLs(url = "/configuration/setzonevalue")
	public static void setZoneForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "configuration.setzone", values);
	}

	@Commands(context = Context.AVATAR, description = "Set an event level configuration value")
	public static Response setEvent(State st,
	                                @Arguments(type = ArgumentType.EVENT, description = "Character group to edit the key for")
			                                Event event,
	                                @Arguments(type = ArgumentType.KVLIST, description = "Key to set the value of")
			                                String key,
	                                @Arguments(type = ArgumentType.TEXT_ONELINE, description = "New value for the key", max = 4096)
			                                String value) throws UserException, SystemException {
		event.validate(st);
		Modules.validateKV(st, key);
		KV kv = Modules.getKVDefinition(st, key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '" + kv.editpermission() + "' necessary to set the value of " + key);
		}
		if (!kv.appliesTo(event)) {
			return new ErrorResponse("KV " + kv.fullname() + " of scope " + kv.scope() + " does not apply to events");
		}
		String oldvalue = st.getRawKV(event, key);
		st.setKV(event, key, value);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetEventKV", event.getName() + "/" + key, oldvalue, value, "Changed event level configuration");
		return new OKResponse("Event KV store has been updated for event " + event.getName());
	}


	@URLs(url = "/configuration/seteventvalue")
	public static void setEventForm(State st, SafeMap values) {
		Modules.simpleHtml(st, "configuration.setevent", values);
	}

	@Commands(context = Context.AVATAR, description = "Set a character group level configuration value")
	public static Response setGroup(State st,
	                                @Arguments(type = ArgumentType.CHARACTERGROUP, description = "Character group to edit the key for")
			                                CharacterGroup group,
	                                @Arguments(type = ArgumentType.KVLIST, description = "Key to set the value of")
			                                String key,
	                                @Arguments(type = ArgumentType.TEXT_ONELINE, description = "New value for the key", max = 4096)
			                                String value) throws UserException, SystemException {
		group.validate(st);
		Modules.validateKV(st, key);
		KV kv = Modules.getKVDefinition(st, key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '" + kv.editpermission() + "' necessary to set the value of " + key);
		}
		if (!kv.appliesTo(group)) {
			return new ErrorResponse("KV " + kv.fullname() + " of scope " + kv.scope() + " does not apply to character groups");
		}
		String oldvalue = st.getRawKV(group, key);
		st.setKV(group, key, value);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetGroupKV", group.getName() + "/" + key, oldvalue, value, "Changed group level configuration");
		// bit cludgy but...
		// to be done TODO
		if (key.equalsIgnoreCase("GPHUDServer.AutoAttach") || key.equalsIgnoreCase("GPHUDServer.ParcelONLY")) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}

		return new OKResponse("Group KV store has been updated for [" + group.getTypeNotNull() + "] " + group.getName());
	}

	@URLs(url = "/configuration/setgroupvalue")
	public static void setGroupForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "configuration.setgroup", values);
	}

	@Commands(context = Context.AVATAR, description = "Set a character level configuration value")
	public static Response setChar(State st,
	                               @Arguments(type = ArgumentType.CHARACTER, description = "Character to edit the key for")
			                               Char character,
	                               @Arguments(type = ArgumentType.KVLIST, description = "Key to set the value of")
			                               String key,
	                               @Arguments(type = ArgumentType.TEXT_ONELINE, description = "New value for the key", max = 4096)
			                               String value) throws UserException, SystemException {
		character.validate(st);
		Modules.validateKV(st, key);
		KV kv = Modules.getKVDefinition(st, key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '" + kv.editpermission() + "' necessary to set the value of " + key);
		}
		if (!kv.appliesTo(character)) {
			return new ErrorResponse("KV " + kv.fullname() + " of scope " + kv.scope() + " does not apply to characters");
		}
		String oldvalue = st.getRawKV(character, key);
		st.setKV(character, key, value);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, character, "SetCharKV", character.getName() + "/" + key, oldvalue, value, "Changed character scope KV configuration");

		return new OKResponse("Character KV store has been updated for " + character.getName());
	}

	@URLs(url = "/configuration/setcharvalue")
	public static void setCharForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "configuration.setchar", values);
	}

	@URLs(url = "/configuration/setself")
	public static void setSelfForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "characters.set", values);
	}
}
