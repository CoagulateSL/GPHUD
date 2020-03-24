/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


/**
 * Configure attributes.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AttributeConfig {

	@Nonnull
	public static String blankNulls(@Nullable final String s) {
		if (s==null) { return ""; }
		return s;
	}

	@URLs(url="/configuration/characters")
	public static void configPage(@Nonnull final State st,
	                              final SafeMap values) { configPage(st,values,st.simulate(st.getCharacterNullable())); }

	public static void configPage(@Nonnull final State st,
	                              final SafeMap values,
	                              final State simulated) {
		final Form f=st.form();
		f.add(new TextHeader("Defined Attributes"));
		f.noForm();
		final Table at=new Table();
		f.add(at);
		at.add(new HeaderRow().add("Name").add("Attribute Type").add("SubType").add("Consumes Ability Points").add("Allow Self Modify").add("Required").add("Default"));
		for (final Attribute a: st.getAttributes()) {
			final Row r=new Row();
			if (a.readOnly()) { r.setbgcolor("#e0e0e0"); }
			at.add(r);
			at.add(a.getName());
			at.add(a.getType().toString());
			at.add(blankNulls(a.getSubType()));

			if (!a.readOnly() && st.hasPermission("Characters.CreateAttribute")) {
				at.add(new Form(st,true,"./characters/toggleap",a.usesAbilityPoints()+"","attribute",a.getName(),a.usesAbilityPoints()?"usesabilitypoints":"noop","set"));
			}
			else { at.add(a.usesAbilityPoints()); }

			if (!a.readOnly() && st.hasPermission("Characters.CreateAttribute")) {
				at.add(new Form(st,true,"./characters/toggleselfmodify",a.getSelfModify()+"","attribute",a.getName(),a.getSelfModify()?"selfmodify":"noop","set"));
			}
			else { at.add(a.getSelfModify()); }

			if (!a.readOnly() && st.hasPermission("Characters.CreateAttribute")) {
				at.add(new Form(st,true,"./characters/togglerequired",a.getRequired()+"","attribute",a.getName(),a.getRequired()?"required":"noop","set"));
			}
			else { at.add(a.getRequired()); }

			if (!a.readOnly() && st.hasPermission("Characters.CreateAttribute")) {
				at.add(new Form(st,true,"./characters/setdefaultvalue",a.getDefaultValue(),"attribute",a.getName(),"defaultvalue",a.getDefaultValue()));
			}
			else { at.add(blankNulls(a.getDefaultValue())); }

			if (!a.readOnly() && st.hasPermission("Characters.CreateAttribute")) {
				at.add(new Form(st,true,"./characters/deleteattribute","Delete Attribute","attribute",a.getName()));
			}
		}

		if (st.hasPermission("Characters.CreateAttribute")) {
			f.add(new Form(st,true,"./characters/addattribute","Create New Attribute"));
		}
		f.add(new Separator());
		Modules.get(st,"Characters").kvConfigPage(st);
	}


	@URLs(url="/configuration/characters/addattribute", requiresPermission="Characters.CreateAttribute")
	public static void addAttribute(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Characters.CreateAttribute",values);
	}

	@Nonnull
	public static List<String> getAttributeTypes(final State st) {
		final List<String> choices=new ArrayList<>();
		choices.add("INTEGER");
		choices.add("FLOAT");
		choices.add("GROUP");
		choices.add("TEXT");
		choices.add("COLOR");
		choices.add("EXPERIENCE");
		return choices;
	}

	@Nonnull
	@Commands(description="Create a new attribute", context=Command.Context.AVATAR, requiresPermission="Characters.CreateAttribute", permitObject=false)
	public static Response createAttribute(@Nonnull final State st,
	                                       @Nonnull @Arguments(description="Name of new attribute", type=Argument.ArgumentType.TEXT_INTERNAL_NAME, max=64) final String name,
	                                       @Arguments(description="Allow users to edit their own value without needing permissions", type=Argument.ArgumentType.BOOLEAN)
	                                       final Boolean selfmodify,
	                                       @Nullable @Arguments(description="Type of this attribute", type=Argument.ArgumentType.CHOICE, choiceMethod="getAttributeTypes")
	                                       final String attributetype,
	                                       @Nullable
	                                       @Arguments(description="Type of group, if GROUP, or awarding group TYPE if EXPERIENCE", mandatory=false, type=
			                                       Argument.ArgumentType.TEXT_INTERNAL_NAME, max=128)
	                                       final String grouptype,
	                                       @Arguments(description="Increases based off allocation of ability points? (only for INTEGER/FLOAT types)", mandatory=false, type=
			                                       Argument.ArgumentType.BOOLEAN)
	                                       final Boolean usesabilitypoints,
	                                       @Arguments(description="Attribute must be completed", type=Argument.ArgumentType.BOOLEAN) final Boolean required,
	                                       @Arguments(description="Default value (can be blank)", type=Argument.ArgumentType.TEXT_ONELINE, max=4096, mandatory=false)
	                                       final String defaultvalue) {
		// already exists as an attribute
		for (final Attribute a: st.getAttributes()) {
			if (a.getName().equalsIgnoreCase(name)) { return new ErrorResponse("This name is already claimed"); }
		}
		// already exists as a template
		for (String templatename: st.getTemplates(false).keySet()) {
			if (templatename.equalsIgnoreCase("--"+name+"--")) {
				return new ErrorResponse("There is already a (potential) template with that name, it can not also be used as an attribute name");
			}
		}
		if (name.length()<2) { return new ErrorResponse("Please enter a longer name"); }
		final String cleansed=name.replaceAll("[^A-Za-z]","");
		if (!cleansed.equals(name)) {
			return new ErrorResponse("Please only use A-Z in the attribute name, no spaces either");
		}
		if (attributetype==null || attributetype.isEmpty()) {
			return new ErrorResponse("Attribute type can not be null/empty");
		}
		// attribute type will be validated by the DB ENUM, expect low level errors if you fake this :P
		if ("Faction".equalsIgnoreCase(cleansed)) {
			return new ErrorResponse("You must use the factions module for faction management");
		}
		if ("GROUP".equals(attributetype)) {
			if (grouptype==null) { return new ErrorResponse("Group data type must have a group type attached"); }
			if ("Faction".equalsIgnoreCase(grouptype)) {
				return new ErrorResponse("You should not be creating a faction attribute ; use the factions module");
			}
		}

		st.getInstance().createAttribute(name,selfmodify,Attribute.fromString(attributetype),grouptype,usesabilitypoints,required,defaultvalue);
		Audit.audit(st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            null,
		            "CreateAttribute",
		            name,
		            null,
		            null,
		            "Avatar created new attribute "+name+" selfmod:"+selfmodify+" type:"+attributetype+" group:"+grouptype+" AP:"+usesabilitypoints+" REQ:"+required+" "+
				            "Default:"+defaultvalue
		           );
		return new OKResponse("Okay, attribute created");
	}

	@URLs(url="/configuration/characters/toggleap", requiresPermission="Characters.CreateAttribute")
	public static void toggleAP(@Nonnull final State st,
	                            @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Characters.SetAP",values);
	}

	@Nonnull
	@Commands(description="Set wether an existing attribute uses Ability Points", context=Command.Context.AVATAR, requiresPermission="Characters.CreateAttribute")
	public static Response setAP(@Nonnull final State st,
	                             @Nonnull @Arguments(description="Attribute", type=Argument.ArgumentType.ATTRIBUTE) final Attribute attribute,
	                             @Arguments(description="Increases based off allocation of ability points? (only for INTEGER/FLOAT types)", mandatory=false, type=
			                             Argument.ArgumentType.BOOLEAN)
	                             final Boolean usesabilitypoints) {
		attribute.validate(st);
		final boolean oldvalue=attribute.usesAbilityPoints();
		attribute.setUsesAbilityPoints(usesabilitypoints);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Attribute/SetAP",attribute.getName(),oldvalue+"",usesabilitypoints+"","Avatar set use of ability points");
		return new OKResponse("Attribute ability points flag updated");
	}


	@URLs(url="/configuration/characters/toggleselfmodify", requiresPermission="Characters.CreateAttribute")
	public static void toggleSelfModify(@Nonnull final State st,
	                                    @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Characters.SetSelfModify",values);
	}

	@Nonnull
	@Commands(description="Set wether an existing attribute can be directly updated by the character", context=Command.Context.AVATAR, requiresPermission="Characters"+
			".CreateAttribute")
	public static Response setSelfModify(@Nonnull final State st,
	                                     @Nonnull @Arguments(description="Attribute", type=Argument.ArgumentType.ATTRIBUTE) final Attribute attribute,
	                                     @Arguments(description="Free self modification allowed?", mandatory=false, type=Argument.ArgumentType.BOOLEAN)
	                                     final Boolean selfmodify) {
		attribute.validate(st);
		final boolean oldvalue=attribute.getSelfModify();
		attribute.setSelfModify(selfmodify);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Attribute/SetSelfModify",attribute.getName(),oldvalue+"",selfmodify+"","Avatar set self modify");
		return new OKResponse("Attribute self modification flag updated");
	}

	@URLs(url="/configuration/characters/togglerequired", requiresPermission="Characters.CreateAttribute")
	public static void toggleRequired(@Nonnull final State st,
	                                  @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Characters.SetRequired",values);
	}

	@Nonnull
	@Commands(description="Set wether an existing attribute must have a value", context=Command.Context.AVATAR, requiresPermission="Characters.CreateAttribute")
	public static Response setRequired(@Nonnull final State st,
	                                   @Nonnull @Arguments(description="Attribute", type=Argument.ArgumentType.ATTRIBUTE) final Attribute attribute,
	                                   @Arguments(description="Value required?", mandatory=false, type=Argument.ArgumentType.BOOLEAN) final Boolean required) {
		attribute.validate(st);
		final boolean oldvalue=attribute.getRequired();
		attribute.setRequired(required);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Attribute/SetRequired",attribute.getName(),oldvalue+"",required+"","Avatar set required");
		return new OKResponse("Attribute requried flag updated");
	}

	@URLs(url="/configuration/characters/setdefaultvalue", requiresPermission="Characters.CreateAttribute")
	public static void setDefaultValue(@Nonnull final State st,
	                                   @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Characters.SetDefault",values);
	}

	@Nonnull
	@Commands(description="Set the default value for an attribute", context=Command.Context.AVATAR, requiresPermission="Characters.CreateAttribute")
	public static Response setDefault(@Nonnull final State st,
	                                  @Nonnull @Arguments(description="Attribute", type=Argument.ArgumentType.ATTRIBUTE) final Attribute attribute,
	                                  @Arguments(description="Default value", mandatory=false, type=Argument.ArgumentType.TEXT_ONELINE, max=4096) final String defaultvalue) {
		attribute.validate(st);
		String oldvalue=attribute.getDefaultValue();
		if (oldvalue==null) { oldvalue="null"; }
		attribute.setDefaultValue(defaultvalue);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Attribute/SetSelfModify",attribute.getName(),oldvalue,defaultvalue,"Avatar set default");
		return new OKResponse("Attribute self modification flag  updated");
	}

	@URLs(url="/configuration/characters/deleteattribute", requiresPermission="Characters.DeleteAttribute")
	public static void deleteAttributeForm(@Nonnull final State st,
	                                       @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Characters.deleteAttribute",values);
	}

	@Nonnull
	@Commands(description="Delete an attribute", context=Command.Context.AVATAR, requiresPermission="Characters.DeleteAttribute", permitObject=false)
	public static Response deleteAttribute(@Nonnull final State st,
	                                       @Nonnull @Arguments(description="Attribute **AND ALL ITS DATA** to delete", type=Argument.ArgumentType.ATTRIBUTE)
	                                       final Attribute attribute,
	                                       @Arguments(description="CONFIRM AS THIS WILL IRREVERSIBLY DELETE DATA ATTACHED TO THIS ATTRIBUTE", mandatory=false, type=
			                                       Argument.ArgumentType.BOOLEAN)
	                                       final Boolean confirm) {
		attribute.validate(st);
		if (!confirm) { return new ErrorResponse("You did not confirm the operation"); }
		attribute.delete();
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Attribute/DELETE",attribute.getName(),null,null,"Avatar DELETED ATTRIBUTE ENTIRELY");
		return new OKResponse("Attribute and attached data has been DELETED");
	}

}
