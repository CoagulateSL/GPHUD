package net.coagulate.GPHUD.Modules.Sets;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterSet;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Sets {
	
	@Nonnull
	@Command.Commands(context=Command.Context.AVATAR, description="Add a number of an element to a set")
	public static Response add(@Nonnull final State st,
	                           @Nonnull
	                           @Argument.Arguments(type=Argument.ArgumentType.CHARACTER,
	                                               description="Character to modify",
	                                               name="character") final Char character,
	                           @Nonnull
	                           @Argument.Arguments(type=Argument.ArgumentType.SET,
	                                               description="Set to add to",
	                                               name="set") final Attribute set,
	                           @Nonnull
	                           @Argument.Arguments(description="Element to add to set",
	                                               type=Argument.ArgumentType.TEXT_ONELINE,
	                                               name="element",
	                                               max=128) final String element,
	                           @Nullable
	                           @Argument.Arguments(name="qty",
	                                               type=Argument.ArgumentType.INTEGER,
	                                               description="Number of element to add (or remove, if negative)",
	                                               mandatory=false) Integer qty) {
		preCheck(st,set,character,true);
		if (qty==null) {
			qty=1;
		}
		// guess we're ok then (!)
		final CharacterSet characterSet=new CharacterSet(character,set);
		final int oldValue=characterSet.count(element);
		final int total=characterSet.add(element,qty);
		Audit.audit(true,
		            st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            character,
		            "Add",
		            set.getName(),
		            String.valueOf(oldValue),
		            String.valueOf(total),
		            "Added "+qty+" "+element+" to set, totalling "+total);
		return new OKResponse(
				"Added "+qty+" "+element+" to "+character+"'s "+set+", changing total from "+oldValue+" to "+total);
	}
	
	private static void preCheck(final State st,final Attribute set,final Char character,final boolean checkAdmin) {
		// Attribute must be a set
		if (set.getType()!=Attribute.ATTRIBUTETYPE.SET) {
			throw new UserInputStateException("Attribute "+set.getName()+" is of type "+set.getType()+" not SET",true);
		}
		// Attribute must belong to instance (!)
		if (set.getInstance()!=st.getInstance()) {
			throw new SystemImplementationException("Attribute "+set+" is not from instance "+st.getInstanceString());
		}
		// check permission
		if (checkAdmin&&!st.hasPermission("Characters.Set"+set.getName())) {
			throw new UserAccessDeniedException(
					"You do not have permission Characters.Set"+set.getName()+" required to modify this characters set",
					true);
		}
		// check character is of right instance
		if (st.getInstance()!=character.getInstance()) {
			throw new SystemImplementationException(
					"Target character "+character+" is not from instance "+st.getInstanceString());
		}
	}
	
	@URL.URLs(url="/configuration/sets/setset")
	public static void setForm(@Nonnull final State st,@Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Sets.Set",values);
	}
	
	@Nonnull
	@Command.Commands(context=Command.Context.AVATAR, description="Set the number of an element in a set")
	public static Response set(@Nonnull final State st,
	                           @Nonnull
	                           @Argument.Arguments(type=Argument.ArgumentType.CHARACTER,
	                                               description="Character to modify",
	                                               name="character") final Char character,
	                           @Nonnull
	                           @Argument.Arguments(type=Argument.ArgumentType.SET,
	                                               description="Set to add to",
	                                               name="set") final Attribute set,
	                           @Nonnull
	                           @Argument.Arguments(description="Element to add to set",
	                                               type=Argument.ArgumentType.TEXT_ONELINE,
	                                               name="element",
	                                               max=128) final String element,
	                           @Nullable
	                           @Argument.Arguments(name="qty",
	                                               type=Argument.ArgumentType.INTEGER,
	                                               description="Quantity to set to",
	                                               mandatory=false) Integer qty) {
		preCheck(st,set,character,true);
		if (qty==null) {
			qty=1;
		}
		// guess we're ok then (!)
		final CharacterSet characterSet=new CharacterSet(character,set);
		final int oldValue=characterSet.count(element);
		characterSet.set(element,qty);
		Audit.audit(true,
		            st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            character,
		            "Set",
		            set.getName(),
		            String.valueOf(oldValue),
		            String.valueOf(qty),
		            "Set "+qty+" x "+element);
		return new OKResponse("Set "+qty+" "+element+" in "+character+"'s "+set+" (was "+oldValue+")");
	}
	
	@Nonnull
	@Command.Commands(context=Command.Context.AVATAR, description="Wipes the entire contents of a set")
	public static Response wipe(@Nonnull final State st,
	                            @Nonnull
	                            @Argument.Arguments(type=Argument.ArgumentType.CHARACTER,
	                                                description="Character to modify",
	                                                name="character") final Char character,
	                            @Nonnull
	                            @Argument.Arguments(type=Argument.ArgumentType.SET,
	                                                description="Set to wipe",
	                                                name="set") final Attribute set) {
		preCheck(st,set,character,true);
		// guess we're ok then (!)
		final CharacterSet characterSet=new CharacterSet(character,set);
		final int elements=characterSet.countElements();
		final int totalCount=characterSet.countTotal();
		characterSet.wipe();
		Audit.audit(true,
		            st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            character,
		            "Empty",
		            set.getName(),
		            null,
		            null,
		            "Wiped set "+set.getName()+", formerly containing "+elements+" elements with total quantity "+
		            totalCount);
		return new OKResponse(
				"Wiped "+character+"'s "+set+", formerly containing "+elements+" elements with total quantity "+
				totalCount);
	}
	
	@Nonnull
	@Command.Commands(context=Command.Context.CHARACTER, description="Reports on the contents of a set")
	public static Response view(@Nonnull final State st,
	                            @Nonnull
	                            @Argument.Arguments(type=Argument.ArgumentType.SET,
	                                                description="Set to view",
	                                                name="set") final Attribute set) {
		preCheck(st,set,st.getCharacter(),false);
		return new OKResponse(set.getName()+" contains: "+new CharacterSet(st.getCharacter(),set).textList());
	}
}
