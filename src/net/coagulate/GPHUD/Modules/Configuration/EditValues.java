package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows get/set of configuration values.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EditValues {
	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set an instance level configuration value")
	public static Response setInstance(@Nonnull final State st,
	                                   @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                       description="Key to set the value of") final String key,
	                                   @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                              description="New value for the key",
	                                              max=4096,
	                                              mandatory=false) final String value) {
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(st.getInstance())) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to instances");
		}
		final String oldvalue=st.getRawKV(st.getInstance(),key);
		st.setKV(st.getInstance(),key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetInstanceKV",key,oldvalue,value,"Changed instance level configuration");
		// bit cludgy but...
		if ("GPHUDServer.AutoAttach".equalsIgnoreCase(key) || "GPHUDServer.ParcelONLY".equalsIgnoreCase(key)) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}
		return new OKResponse("Instance KV store has been updated");
	}

	@URLs(url="/configuration/setinstancevalue")
	public static void setInstanceForm(@Nonnull final State st,
	                                   @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.setinstance",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set a region level configuration value")
	public static Response setRegion(@Nonnull final State st,
	                                 @Nonnull @Arguments(type=ArgumentType.REGION,
	                                                     description="Region to edit the key for") final Region region,
	                                 @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                     description="Key to set the value of") final String key,
	                                 @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                            description="New value for the key",
	                                            max=4096,
	                                            mandatory=false) final String value) {
		region.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(region)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to regions");
		}
		final String oldvalue=st.getRawKV(region,key);
		st.setKV(region,key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetRegionKV",region.getName()+"/"+key,oldvalue,value,"Changed region level configuration");
		// bit cludgy but...
		if ("GPHUDServer.AutoAttach".equalsIgnoreCase(key) || "GPHUDServer.ParcelONLY".equalsIgnoreCase(key)) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}

		return new OKResponse("Region "+region.getName()+" KV store has been updated");
	}

	@URLs(url="/configuration/setregionvalue")
	public static void setRegionForm(@Nonnull final State st,
	                                 @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.setregion",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set a key value for a zone")
	public static Response setZone(@Nonnull final State st,
	                               @Nonnull @Arguments(type=ArgumentType.ZONE,
	                                                   description="Name of the zone") final Zone zone,
	                               @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                   description="Key to set") final String key,
	                               @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                          description="Value to set to",
	                                          max=4096,
	                                          mandatory=false) final String value) {
		zone.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(zone)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to zones");
		}
		final String oldvalue=st.getRawKV(zone,key);
		st.setKV(zone,key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetZoneKV",zone.getName()+"/"+key,oldvalue,value,"Updated zone KV entry");
		return new OKResponse("KV Store updated for zone '"+zone.getName()+"'");
	}

	@URLs(url="/configuration/setzonevalue")
	public static void setZoneForm(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.setzone",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set an event level configuration value")
	public static Response setEvent(@Nonnull final State st,
	                                @Nonnull @Arguments(type=ArgumentType.EVENT,
	                                                    description="Character group to edit the key for") final Event event,
	                                @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                    description="Key to set the value of") final String key,
	                                @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                           description="New value for the key",
	                                           max=4096,
	                                           mandatory=false) final String value) {
		event.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(event)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to events");
		}
		final String oldvalue=st.getRawKV(event,key);
		st.setKV(event,key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetEventKV",event.getName()+"/"+key,oldvalue,value,"Changed event level configuration");
		return new OKResponse("Event KV store has been updated for event "+event.getName());
	}


	@URLs(url="/configuration/seteventvalue")
	public static void setEventForm(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.setevent",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set a character group level configuration value")
	public static Response setGroup(@Nonnull final State st,
	                                @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP,
	                                                    description="Character group to edit the key for") final CharacterGroup group,
	                                @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                    description="Key to set the value of") final String key,
	                                @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                           description="New value for the key",
	                                           max=4096,
	                                           mandatory=false) final String value) {
		group.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(group)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to character groups");
		}
		final String oldvalue=st.getRawKV(group,key);
		st.setKV(group,key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetGroupKV",group.getName()+"/"+key,oldvalue,value,"Changed group level configuration");
		// bit cludgy but...
		// to be done TODO
		if ("GPHUDServer.AutoAttach".equalsIgnoreCase(key) || "GPHUDServer.ParcelONLY".equalsIgnoreCase(key)) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}

		return new OKResponse("Group KV store has been updated for ["+group.getTypeNotNull()+"] "+group.getName());
	}

	@URLs(url="/configuration/setgroupvalue")
	public static void setGroupForm(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.setgroup",values);
	}

	@Commands(context=Context.AVATAR,
	          description="Set an effect level configuration value")
	public static Response setEffect(@Nonnull final State st,
	                                 @Nonnull @Arguments(type=ArgumentType.EFFECT,
	                                                     description="Effect to edit the key for") final Effect effect,
	                                 @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                     description="Key to set the value of") final String key,
	                                 @Nullable @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                      description="New value for the key",
	                                                      max=4096,
	                                                      mandatory=false) final String value) {
		effect.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(effect)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to effects");
		}
		final String oldvalue=st.getRawKV(effect,key);
		st.setKV(effect,key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetGroupKV",effect.getName()+"/"+key,oldvalue,value,"Changed effect level configuration");
		// bit cludgy but...
		// to be done TODO
		if ("GPHUDServer.AutoAttach".equalsIgnoreCase(key) || "GPHUDServer.ParcelONLY".equalsIgnoreCase(key)) {
			net.coagulate.GPHUD.Modules.GPHUDServer.Register.sendAttachConfig(st);
		}

		return new OKResponse("Effect KV store has been updated for "+effect.getName());

	}

	@URLs(url="/configuration/seteffectvalue")
	public static void setEffectForm(@Nonnull final State st,
	                                 @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.seteffect",values);
	}


	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set a character level configuration value")
	public static Response setChar(@Nonnull final State st,
	                               @Nonnull @Arguments(type=ArgumentType.CHARACTER,
	                                                   description="Character to edit the key for") final Char character,
	                               @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                   description="Key to set the value of") final String key,
	                               @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                          description="New value for the key",
	                                          max=4096,
	                                          mandatory=false) final String value) {
		character.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(character)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to characters");
		}
		final String oldvalue=st.getRawKV(character,key);
		st.setKV(character,key,value);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,character,"SetCharKV",character.getName()+"/"+key,oldvalue,value,"Changed character scope KV configuration");

		return new OKResponse("Character KV store has been updated for "+character.getName());
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Adjusts a character level configuration value.  This must be a INTEGER.  For CUMULATIVE types this will read the current char level KV and alter it by this ammount, for DELEGATING types this will read the current total value, adjust it, and write this to the character's KV")
	public static Response deltaCharInt(@Nonnull final State st,
	                                    @Nonnull @Arguments(type=ArgumentType.CHARACTER,
	                                                        description="Character to edit the key for") final Char character,
	                                    @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                        description="Key to set the value of") final String key,
	                                    @Nonnull @Arguments(type=ArgumentType.INTEGER,
	                                                        description="Ammount to change the total value by",
	                                                        max=4096) final Integer delta) {
		character.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(character)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to characters");
		}
		if (!(kv.type()==KVTYPE.INTEGER)) {
			return new ErrorResponse("This command will only change INTEGER KVs, the requested key "+key+" is of type "+kv.type());
		}
		// we have very different behaviour depending on the hierarchy type:
		Integer newvalue=null;
		State targetstate=new State(character);
		switch (kv.hierarchy()) {
			case DELEGATING: // delegating means character value takes precedence ; read current total value and straight set char KV to result
				newvalue=targetstate.getKV(key).intValue()+delta;
				break;
			case CUMULATIVE: // cumulative sums the stack, so we can adjust by reading the char only value, unnulling and adjusting directly
			case NONE: // no hierarchy, so read char only and write char only
				String curvalue=targetstate.getKV(character,key);
				if (curvalue==null) {
					newvalue=delta;
				}
				else {
					newvalue=Integer.parseInt(curvalue)+delta;
				}
				break;
			case AUTHORITATIVE: // an unused hierarchy.  character level would be dominated by any number of other things so might be irrelevant
				// seems most likely authoritative will never be used and even NONE is more likely useful than this type, so for now
				throw new UserInputInvalidChoiceException("KV "+key+" is of hierarchy type AUTHORITATIVE and no behaviour is coded for deltaChar in this case");
		}
		if (newvalue==null) { throw new SystemImplementationException("Newvalue is null after a delta operation?"); }
		final String oldvalue=st.getRawKV(character,key);
		st.setKV(character,key,newvalue.toString());
		Audit.audit(st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            character,
		            "DeltaCharKV",
		            character.getName()+"/"+key,
		            oldvalue,
		            newvalue.toString(),
		            "Deltaed character scope KV configuration by "+delta
		           );

		return new OKResponse("Character KV store has been adjusted for "+character.getName());
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Adjusts a character level configuration value.  This must be a FLOAT.  For CUMULATIVE types this will read the current char level KV and alter it by this ammount, for DELEGATING types this will read the current total value, adjust it, and write this to the character's KV")
	public static Response deltaCharFloat(@Nonnull final State st,
	                                      @Nonnull @Arguments(type=ArgumentType.CHARACTER,
	                                                          description="Character to edit the key for") final Char character,
	                                      @Nonnull @Arguments(type=ArgumentType.KVLIST,
	                                                          description="Key to set the value of") final String key,
	                                      @Nonnull @Arguments(type=ArgumentType.INTEGER,
	                                                          description="Ammount to change the total value by",
	                                                          max=4096) final Float delta) {
		character.validate(st);
		Modules.validateKV(st,key);
		final KV kv=Modules.getKVDefinition(st,key);
		if (!st.hasPermission(kv.editpermission())) {
			return new ErrorResponse("You lack permission '"+kv.editpermission()+"' necessary to set the value of "+key);
		}
		if (!kv.appliesTo(character)) {
			return new ErrorResponse("KV "+kv.fullname()+" of scope "+kv.scope()+" does not apply to characters");
		}
		if (!(kv.type()==KVTYPE.FLOAT)) {
			return new ErrorResponse("This command will only change FLOAT KVs, the requested key "+key+" is of type "+kv.type());
		}
		// we have very different behaviour depending on the hierarchy type:
		Float newvalue=null;
		State targetstate=new State(character);
		switch (kv.hierarchy()) {
			case DELEGATING: // delegating means character value takes precedence ; read current total value and straight set char KV to result
				newvalue=targetstate.getKV(key).intValue()+delta;
				break;
			case CUMULATIVE: // cumulative sums the stack, so we can adjust by reading the char only value, unnulling and adjusting directly
			case NONE: // no hierarchy, so read char only and write char only
				String curvalue=targetstate.getKV(character,key);
				if (curvalue==null) {
					newvalue=delta;
				}
				else {
					newvalue=Float.parseFloat(curvalue)+delta;
				}
				break;
			case AUTHORITATIVE: // an unused hierarchy.  character level would be dominated by any number of other things so might be irrelevant
				// seems most likely authoritative will never be used and even NONE is more likely useful than this type, so for now
				throw new UserInputInvalidChoiceException("KV "+key+" is of hierarchy type AUTHORITATIVE and no behaviour is coded for deltaChar in this case");
		}
		if (newvalue==null) { throw new SystemImplementationException("Newvalue is null after a delta operation?"); }
		final String oldvalue=st.getRawKV(character,key);
		st.setKV(character,key,newvalue.toString());
		Audit.audit(st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            character,
		            "DeltaCharKV",
		            character.getName()+"/"+key,
		            oldvalue,
		            newvalue.toString(),
		            "Deltaed character scope KV configuration by "+delta
		           );

		return new OKResponse("Character KV store has been adjusted for "+character.getName());
	}

	@URLs(url="/configuration/setcharvalue")
	public static void setCharForm(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"configuration.setchar",values);
	}

	@URLs(url="/configuration/setself")
	public static void setSelfForm(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"characters.set",values);
	}
}
