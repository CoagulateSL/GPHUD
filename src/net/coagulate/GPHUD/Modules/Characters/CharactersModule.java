/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Experience.QuotaedXP;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.*;

/**
 * Module for characters and their dynamic definitions
 *
 * @author iain
 */
public class CharactersModule extends ModuleAnnotation {


	public CharactersModule(String name, ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
	}

	public static String templateAttribute(State st, String template) {
		if (template == null) { throw new SystemException("Null template?"); }
		template = template.substring(2, template.length() - 2);
		if (template.startsWith("TARGET:")) {
			template = template.substring(7);
			st = st.getTarget();
			if (st == null) { throw new UserException("No target!"); }
		}
		Attribute attr = null;
		for (Attribute a : st.getAttributes()) {
			if (a.getName().equalsIgnoreCase(template)) {
				if (attr != null) {
					throw new SystemException("Unexpected duplicate resolution for attribute " + attr.getName() + " and " + a.getName());
				}
				attr = a;
			}
		}
		if (attr == null) { throw new SystemException("Failed to resolve attribute definition " + template); }
		//System.out.println(attr);
		if (attr.isKV()) {
			return st.getKV("characters." + template).value();
		}
		if (attr.getType() == GROUP) {
			CharacterGroup group = st.getCharacter().getGroup(attr.getSubType());
			if (group == null) { return ""; }
			return group.getName();
		}
		if (attr.getType() == POOL || attr.getType() == EXPERIENCE) {
			if (attr instanceof QuotaedXP) {
				QuotaedXP xp = (QuotaedXP) attr;
				return st.getCharacter().sumPool(xp.getPool(st)) + "";
			} else { return "POOL"; }
		}
		throw new SystemException("Failed to resolve templateAttribute for " + attr + " of type " + attr.getType());
	}

	public static int spentAbilityPoints(State st) {
		int total = 0;
		for (Attribute attribute : st.getAttributes()) {
			//System.out.println("ATTRIBUTE IS "+attribute);
			try {
				if (attribute.usesAbilityPoints()) {
					String spent = st.getKV(st.getCharacter(), "Characters." + attribute.getName());
					if (spent != null && !spent.isEmpty()) { total = total + Integer.parseInt(spent); }
				}
			} catch (NoDataException e) {} // attribute deleted race condition
		}
		return total;
	}

	public static int maxAbilityPoints(State st) { return st.getKV("Experience.AbilityPoints").intValue(); }

	public static int abilityPointsRemaining(State st) { return maxAbilityPoints(st) - spentAbilityPoints(st); }

	@Template(name = "ABILITYPOINTS", description = "Number of ability points the character has")
	public static String abilityPoints(State st, String key) {
		if (st.getCharacterNullable() == null) { return ""; }
		return abilityPointsRemaining(st) + "";
	}

	/**
	 * As used by the multiple choice list options.
	 *
	 * @param st State which infers instance
	 * @return List of attributes which can be raised by ability points
	 */
	public static List<String> getRaisableAttributesList(State st) {
		List<String> ret = new ArrayList<>();
		for (Attribute attribute : st.getAttributes()) {
			if (attribute.isKV() && attribute.usesAbilityPoints()) {
				// type check too
				if (attribute.getType() == Attribute.ATTRIBUTETYPE.INTEGER || attribute.getType() == Attribute.ATTRIBUTETYPE.FLOAT) {
					ret.add(attribute.getName());
				}
			}
		}
		return ret;
	}

	@Commands(context = Command.Context.CHARACTER, description = "Spend an ability point to raise an attribute", permitUserWeb = false)
	public static Response spendAbilityPoint(State st,
	                                         @Arguments(choiceMethod = "getRaisableAttributesList", description = "Attribute to spend an ability point on", type = Argument.ArgumentType.CHOICE)
			                                         String attribute) {
		int remain = abilityPointsRemaining(st);
		if (remain <= 0) { return new ErrorResponse("You have no remaining ability points to spend."); }
		Attribute attr = Attribute.find(st.getInstance(), attribute);
		if (!attr.usesAbilityPoints()) {
			return new ErrorResponse("This attribute can not be increased through ability points");
		}
		if (attr.getKVType() != KV.KVTYPE.FLOAT && attr.getKVType() != KV.KVTYPE.INTEGER) {
			throw new UserException("Can not increase a non integer/float attribute type");
		}
		int existing = st.getKV("Characters." + attribute).intValue();
		int max = st.getKV("Characters." + attribute + "max").intValue();
		if (max != 0) {
			if (existing >= max) {
				throw new UserException("You may not increase " + attribute + " any further, the maximum is " + max);
			}
		}
		String localstr = st.getKV(st.getCharacter(), "Characters." + attribute);
		int local = 0;
		if (localstr != null && !localstr.isEmpty()) { local = Integer.parseInt(localstr); }
		st.setKV(st.getCharacter(), "characters." + attribute, (local + 1) + "");
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, st.getCharacter(), "AbilityIncrease", attribute, local + "", (local + 1) + "", "Character spent ability point to increase " + attribute + " from (total) " + existing + " to " + (existing + 1));
		String remaining = "";
		remain--;
		if (remain > 0) {
			remaining = ".  You have " + remain + " more ability points to spend.";
			JSONObject json = Modules.getJSONTemplate(st, "characters.spendabilitypoint");
			json.put("message", "Ability point spent on " + attribute + ", it increased to " + (existing + 1) + remaining);
			return new JSONResponse(json);
		}
		return new OKResponse("Ability point spent on " + attribute + ", it increased to " + (existing + 1) + remaining);
	}

	@Commands(context = Command.Context.CHARACTER, description = "Change a value about your own character")
	public static Response set(State st,
	                           @Arguments(type = Argument.ArgumentType.ATTRIBUTE_WRITABLE, description = "Attribute to set")
			                           Attribute attribute,
	                           @Arguments(type = Argument.ArgumentType.TEXT_ONELINE, description = "Value to use", mandatory = false, max = 4096)
			                           String value) {
		if (attribute == null) { return new ErrorResponse("You must supply an attribute to set"); }
		attribute.validate(st);
		if (attribute.getSelfModify() == false) {
			return new ErrorResponse("This attribute is not self modifiable, it can only be changed directly by an admin");
		}
		KV kv = st.getKVDefinition("Characters." + attribute.getName());
		String oldvalue = st.getRawKV(st.getCharacter(), kv.fullname());
		st.setKV(st.getCharacter(), kv.fullname(), value);
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, null, "AttributeSet", attribute.getNameSafe(), oldvalue, value, "Character updated their own " + attribute.getNameSafe() + " via KV " + kv.fullname());
		return new OKResponse("Your character updated for Attribute " + attribute.getName());
	}

	//Map<String,KV> base=new TreeMap<>();
	@Override
	@Deprecated
	public void registerKV(KV a) throws UserException {
		throw new SystemException("It is no longer permitted to have manual registrations inside Characters module");
	}

	@Override
	public Map<String, KV> getKVDefinitions(State st) {
		//String attributes=st.getKV("Characters.Attributes");
		Map<String, KV> kv = new TreeMap<>();
		for (Attribute attribute : st.getAttributes()) {
			if (attribute.isKV()) {
				kv.put(attribute.getName().toLowerCase(), new AttributeKV(attribute));
				kv.put(attribute.getName().toLowerCase() + "max", new AttributeMaxKV(attribute));
			}
		}
		return kv;
	}

	@Override
	public KV getKVDefinition(State st, String qualifiedname) throws SystemException {
		// avoid infinite loops as we look up definitions and try get our attributes to make more defintiions etc
		return getKVDefinitions(st).get(qualifiedname.toLowerCase());
	}

	@Override
	public void addTemplateDescriptions(State st, Map<String, String> addto) {
		Map<String, KV> ourmap = getKVDefinitions(st);
		for (Attribute attr : st.getAttributes()) {
			addto.put("--" + attr.getName().toUpperCase() + "--", "Character attribute " + attr.getName());
			addto.put("--TARGET:" + attr.getName().toUpperCase() + "--", "TARGET Character attribute " + attr.getName());
		}
	}

	@Override
	public void addTemplateMethods(State st, Map<String, Method> addto) {
		Map<String, KV> ourmap = getKVDefinitions(st);
		for (Attribute attr : st.getAttributes()) {
			try {
				addto.put("--" + attr.getName().toUpperCase() + "--", this.getClass().getMethod("templateAttribute", State.class, String.class));
				addto.put("--TARGET:" + attr.getName().toUpperCase() + "--", this.getClass().getMethod("templateAttribute", State.class, String.class));
			} catch (NoSuchMethodException | SecurityException ex) {
				SL.report("Templating referencing exception??", ex, st);
				st.logger().log(SEVERE, "Exception referencing own templating method??", ex);
			}
		}
	}

	@Override
	public Map<String, Permission> getPermissions(State st) {
		Map<String, Permission> map = super.getPermissions(st);
		if (st == null) { return map; }
		for (Attribute a : st.getAttributes()) {
			map.put("set" + a.getName().toLowerCase(), new AttributePermission(a));
		}
		return map;
	}
}
