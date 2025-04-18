/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.Data.*;
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
import net.coagulate.SL.CacheConfig;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	
	
	public CharactersModule(final String name,final ModuleDefinition annotation) {
		super(name,annotation);
	}
	
	// ---------- STATICS ----------
	@Nullable
	public static String templateAttribute(State st,@Nonnull String template) {
		template=template.substring(2,template.length()-2);
		if (template.startsWith("TARGET:")) {
			template=template.substring(7);
			st=st.getTargetNullable();
			if (st==null) {
				throw new UserInputEmptyException("No target!");
			}
		}
		Attribute attr=null;
		for (final Attribute a: st.getAttributes()) {
			if (a.getName().equalsIgnoreCase(template)) {
				if (attr!=null) {
					throw new SystemConsistencyException(
							"Unexpected duplicate resolution for attribute "+attr.getName()+" and "+a.getName());
				}
				attr=a;
			}
		}
		if (attr==null) {
			throw new SystemConsistencyException("Failed to resolve attribute definition "+template);
		}
		if (attr.isKV()) {
			return st.getKV("characters."+template).value();
		}
		if (attr.getType()==GROUP) {
			if (attr.getSubType()==null) {
				return null;
			}
			final CharacterGroup group=CharacterGroup.getGroup(st,attr.getSubType());
			if (group==null) {
				return "";
			}
			return group.getName();
		}
		if (attr.getType()==POOL||attr.getType()==EXPERIENCE) {
			if (attr instanceof final QuotaedXP xp) {
				return String.valueOf(CharacterPool.sumPool(st,xp.getPool(st)));
			} else {
				return "POOL";
			}
		}
		if (attr.getType()==CURRENCY) {
			return Currency.find(st,attr.getName()).shortSum(st);
		}
		if (attr.getType()==SET) {
			final CharacterSet set=new CharacterSet(st.getCharacter(),attr);
			return set.countElements()+" / "+set.countTotal();
		}
		if (attr.getType()==INVENTORY) {
			final Inventory inventory=new Inventory(st.getCharacter(),attr);
			return inventory.countElements()+" / "+inventory.countTotal();
		}
		throw new SystemConsistencyException(
				"Failed to resolve templateAttribute for "+attr+" of type "+attr.getType());
	}
	
	public static int spentAbilityPoints(@Nonnull final State st) {
		int total=0;
		for (final Attribute attribute: st.getAttributes()) {
			//System.out.println("ATTRIBUTE IS "+attribute);
			try {
				if (attribute.usesAbilityPoints()) {
					final String spent=st.getKV(st.getCharacter(),"Characters."+attribute.getName());
					if (spent!=null&&!spent.isEmpty()) {
						total=total+Integer.parseInt(spent);
					}
				}
			} catch (@Nonnull final NoDataException ignore) {
			} // attribute deleted race condition
		}
		return total;
	}
	
	@Nonnull
	@Template(name="ABILITYPOINTS", description="Number of ability points the character has")
	public static String abilityPoints(@Nonnull final State st,final String key) {
		if (st.getCharacterNullable()==null) {
			return "";
		}
		return String.valueOf(abilityPointsRemaining(st));
	}
	
	public static int maxAbilityPoints(@Nonnull final State st) {
		return st.getKV("Experience.AbilityPoints").intValue();
	}
	
	public static int abilityPointsRemaining(@Nonnull final State st) {
		return maxAbilityPoints(st)-spentAbilityPoints(st);
	}
	
	/**
	 * As used by the multiple choice list options.
	 *
	 * @param st State which infers instance
	 * @return List of attributes which can be raised by ability points
	 */
	@Nonnull
	public static List<String> getRaisableAttributesList(@Nonnull final State st) {
		final List<String> ret=new ArrayList<>();
		for (final Attribute attribute: st.getAttributes()) {
			if (attribute.isKV()&&attribute.usesAbilityPoints()) {
				// type check too
				if (attribute.getType()==INTEGER||attribute.getType()==FLOAT) {
					ret.add(attribute.getName());
				}
			}
		}
		return ret;
	}
	
	@Nonnull
	@Commands(context=Command.Context.CHARACTER,
	          description="Spend an ability point to raise an attribute",
	          permitExternal=false)
	public static Response spendAbilityPoint(@Nonnull final State st,
	                                         @Arguments(choiceMethod="net.coagulate.GPHUD.Modules.Characters.CharactersModule.getRaisableAttributesList",
	                                                    name="attribute",
	                                                    description="Attribute to spend an ability point on",
	                                                    type=Argument.ArgumentType.CHOICE) final String attribute) {
		int remain=abilityPointsRemaining(st);
		if (remain<=0) {
			return new ErrorResponse("You have no remaining ability points to spend.");
		}
		final Attribute attr=Attribute.find(st.getInstance(),attribute);
		if (!attr.usesAbilityPoints()) {
			return new ErrorResponse("This attribute can not be increased through ability points");
		}
		if (attr.getKVType()!=KV.KVTYPE.FLOAT&&attr.getKVType()!=KV.KVTYPE.INTEGER) {
			throw new UserInputStateException("Can not increase a non integer/float attribute type");
		}
		final int existing=st.getKV("Characters."+attribute).intValue();
		final int max=st.getKV("Characters."+attribute+"max").intValue();
		if (max!=0) {
			if (existing>=max) {
				return new ErrorResponse("You may not increase "+attribute+" any further, the maximum is "+max);
			}
		}
		final String localstr=st.getKV(st.getCharacter(),"Characters."+attribute);
		int local=0;
		if (localstr!=null&&!localstr.isEmpty()) {
			local=Integer.parseInt(localstr);
		}
		st.setKV(st.getCharacter(),"characters."+attribute,String.valueOf(local+1));
		Audit.audit(true,
		            st,
		            Audit.OPERATOR.CHARACTER,
		            null,
		            st.getCharacter(),
		            "AbilityIncrease",
		            attribute,
		            String.valueOf(local),
		            String.valueOf(local+1),
		            "Character spent ability point to increase "+attribute+" from (total) "+existing+" to "+
		            (existing+1));
		String remaining="";
		remain--;
		if (remain>0) {
			remaining=".  You have "+remain+" more ability points to spend.";
			final JSONObject json=Modules.getJSONTemplate(st,"characters.spendabilitypoint");
			JSONResponse.message(json,
			                     "Ability point spent on "+attribute+", it increased to "+(existing+1)+remaining,
			                     st.protocol);
			return new JSONResponse(json);
		}
		return new OKResponse("Ability point spent on "+attribute+", it increased to "+(existing+1)+remaining);
	}
	
	@Nonnull
	@Commands(context=Command.Context.CHARACTER, description="Change a value about your own character")
	public static Response set(@Nonnull final State st,
	                           @Nullable
	                           @Arguments(type=Argument.ArgumentType.ATTRIBUTE_WRITABLE,
	                                      name="attribute",
	                                      description="Attribute to set") final Attribute attribute,
	                           @Arguments(type=Argument.ArgumentType.TEXT_ONELINE,
	                                      name="value",
	                                      description="Value to use",
	                                      mandatory=false,
	                                      max=4096) final String value) {
		if (attribute==null) {
			return new ErrorResponse("You must supply an attribute to set");
		}
		attribute.validate(st);
		if (!attribute.getSelfModify()) {
			return new ErrorResponse(
					"This attribute is not self modifiable, it can only be changed directly by an admin");
		}
		final KV kv=st.getKVDefinition("Characters."+attribute.getName());
		final String oldvalue=st.getRawKV(st.getCharacter(),kv.fullName());
		st.setKV(st.getCharacter(),kv.fullName(),value);
		Audit.audit(true,
		            st,
		            Audit.OPERATOR.CHARACTER,
		            null,
		            null,
		            "AttributeSet",
		            attribute.getNameSafe(),
		            oldvalue,
		            value,
		            "Character updated their own "+attribute.getNameSafe()+" via KV "+kv.fullName());
		return new OKResponse("Your character updated for Attribute "+attribute.getName());
	}
	
	@Override
	public KV getKVDefinition(@Nonnull final State st,@Nonnull final String qualifiedname) {
		// avoid infinite loops as we look up definitions and try get our attributes to make more defintiions etc
		return getKVDefinitions(st).get(qualifiedname.toLowerCase());
	}
	
	// naive never flushed cache with short life time.  Just to see if this actually helps much
	private static final Cache<Instance,Map<String,KV>> kvDefsCache=
			Cache.getCache("GPHUD/CharactersModuleKVDefinitions",CacheConfig.SHORT);

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(@Nonnull final State st) {
		//String attributes=st.getKV("Characters.Attributes");
		return kvDefsCache.get(st.getInstance(),()->{
			final Map<String,KV> kv=new TreeMap<>();
			for (final Attribute attribute: st.getAttributes()) {
				if (attribute.isKV()) {
					kv.put(attribute.getName().toLowerCase(),new AttributeKV(attribute));
					kv.put(attribute.getName().toLowerCase()+"max",new AttributeMaxKV(attribute));
				}
			}
			return kv;
		});
	}
	@Override
	public Permission getPermission(final State st,@Nonnull final String itemname) {
		final Permission p=super.getPermission(st,itemname);
		if (p!=null) {
			return p;
		}
		if (itemname.toLowerCase().startsWith("set")) {
			final String attributeName=itemname.substring(3);
			try {
				final Attribute attribute=st.getAttribute(attributeName);
				if (attribute==null) {
					return null;
				}
				return new AttributePermission(attribute);
			} catch (final UserInputLookupFailureException ignored) {
			}
			
		}
		return null;
	}
	
	@Override
	public Map<String,Permission> getPermissions(@Nullable final State st) {
		final Map<String,Permission> map=new TreeMap<>(super.getPermissions(st));
		if (st==null) {
			return map;
		}
		for (final Attribute a: st.getAttributes()) {
			map.put("set"+a.getName().toLowerCase(),new AttributePermission(a));
		}
		return map;
	}
	
	@Override
	public void addTemplateDescriptions(@Nonnull final State st,@Nonnull final Map<String,String> cumulativeMap) {
		final Map<String,KV> ourmap=getKVDefinitions(st);
		for (final Attribute attr: st.getAttributes()) {
			cumulativeMap.put("--"+attr.getName().toUpperCase()+"--","Character attribute "+attr.getName());
			cumulativeMap.put("--TARGET:"+attr.getName().toUpperCase()+"--",
			                  "TARGET Character attribute "+attr.getName());
		}
	}
	
	@Override
	public void addTemplateMethods(@Nonnull final State st,@Nonnull final Map<String,Method> cumulativeMap) {
		final Map<String,KV> ourmap=getKVDefinitions(st);
		for (final Attribute attr: st.getAttributes()) {
			try {
				cumulativeMap.put("--"+attr.getName().toUpperCase()+"--",
				                  getClass().getMethod("templateAttribute",State.class,String.class));
				cumulativeMap.put("--TARGET:"+attr.getName().toUpperCase()+"--",
				                  getClass().getMethod("templateAttribute",State.class,String.class));
			} catch (@Nonnull final NoSuchMethodException|SecurityException ex) {
				SL.report("Templating referencing exception??",ex,st);
				st.logger().log(SEVERE,"Exception referencing own templating method??",ex);
			}
		}
	}
}
