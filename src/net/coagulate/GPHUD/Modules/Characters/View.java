package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Data.Views.AuditTable;
import net.coagulate.GPHUD.Data.Views.PoolTable;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Interfaces.Responses.TabularResponse;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Configuration.ConfigurationHierarchy;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.Modules.Experience.GenericXPPool;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Notes.ViewNotes;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.GPHUD.Tests.TestFramework;
import net.coagulate.SL.Config;
import net.coagulate.SL.TestFrameworkPrototype;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;
import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.*;

/**
 * A simple command implementation.
 * Used by all interfaces for a general purpose "status" page.
 * Rather technical, like a thing hidden in the "About" menu ;)
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class View {
	
	//TODO - FIX THE "boolean full=" section of all these functions, and decide what data is private as a result of this
	
	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.ANY, description="Get textual status of this connection")
	public static Response status(@Nonnull final State st) {
		final TabularResponse t=new TabularResponse();
		t.openRow();
		t.add("Node").addNoNull((Config.getDevelopment()?"DEVELOPMENT // ":"Production // ")+Interface.getNode());
		t.openRow();
		t.add("Avatar");
		if (st.getAvatarNullable()==null) {
			t.add("");
		} else {
			t.addNoNull(st.getAvatar().toString());
		}
		t.openRow();
		t.add("Character").addNoNull(st.getCharacterNullable());
		t.openRow();
		t.add("Instance").addNoNull(st.getInstanceNullable());
		t.openRow();
		t.add("Region").addNoNull(st.getRegionNullable());
		t.openRow();
		t.add("Zone").addNoNull(st.zone);
		t.openRow();
		t.add("Source").addNoNull(st.getSourceNameNullable());
		t.openRow();
		t.add("SourceOwner");
		if (st.getSourceOwnerNullable()==null) {
			t.add("");
		} else {
			t.addNoNull(st.getSourceOwner().toString());
		}
		t.openRow();
		t.add("SourceDev");
		if (st.getSourceDeveloperNullable()==null) {
			t.add("");
		} else {
			t.addNoNull(st.getSourceDeveloper().toString());
		}
		return t;
	}
	
	@TestFramework.Test(name="Test characters.status")
	public static TestFrameworkPrototype.TestOutput testCharacterStatus(final TestFramework f) throws IOException {
		f.primaryRegion.execute("characters.status");
		return f.primaryRegion.testResultPresenceAndDiscard("output1",false);
	}
	
	@URLs(url="/characters/view/*")
	public static void viewCharacter(@Nonnull final State st,@Nonnull final SafeMap values) {
		st.form().noForm();
		//System.out.println(st.uri);
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		if (split.length==4) {
			final String id=split[split.length-1];
			final Char c=Char.get(Integer.parseInt(id));
			if (!st.hasPermission("Characters.ViewAll") && c.getId()!=st.getCharacter().getId()) {
				throw new UserAccessDeniedException("You do not have permission Characters.ViewAll and may only view your own character sheet");
			}
			viewCharacter(st,values,c,false);
			return;
		}
		if (split.length==6) {
			final String id=split[3];
			final Char c=Char.get(Integer.parseInt(id));
			final String attribute=split[4]+"."+split[5];
			if (!st.hasPermission("Characters.ViewAll") && c.getId()!=st.getCharacter().getId()) {
				throw new UserAccessDeniedException("You do not have permission Characters.ViewAll and may only view your own character sheet");
			}
			st.form().add(new ConfigurationHierarchy(st,st.getKVDefinition(attribute),st.simulate(c),values));
			return;
		}
		throw new SystemConsistencyException(
				"Unknown character view mode (length:"+split.length+" URI:"+st.getDebasedURL());
	}
	
	public static void viewCharacter(@Nonnull final State st,
	                                 @Nonnull final SafeMap values,
	                                 @Nonnull final Char c,
	                                 final boolean brief) {
		//boolean full = false;
		final State simulated=st.simulate(c);
		final String tz=st.getAvatar().getTimeZone();
		//if (st.getCharacterNullable() == c) { full = true; }
		//if (st.hasPermission("Characters.ViewAll")) { full = true; }
		final Form f=st.form();
		f.add(new TextSubHeader(c.getName()));
		final Table kvtable=new Table();
		f.add(kvtable);
		kvtable.openRow().add("Owning Avatar").add(c.getOwner().getGPHUDLink());
		String lastplayed="Never";
		final Integer lastplayedstored=c.getLastPlayed();
		if (lastplayedstored!=null) {
			lastplayed=fromUnixTime(lastplayedstored,tz);
		}
		kvtable.openRow().add("Last Played").add(lastplayed).add(tz);
		kvtable.openRow()
		       .add("Online Time (last week)")
		       .add(UnixTime.duration(Visit.sumVisits(c,UnixTime.getUnixTime()-(60*60*24*7))));
		kvtable.openRow()
		       .add("Online Time (last 4 weeks)")
		       .add(UnixTime.duration(Visit.sumVisits(c,UnixTime.getUnixTime()-(60*60*24*7*4))));
		kvtable.openRow().add("Connected");
		if (c.getURL()==null||c.getURL().isEmpty()) {
			kvtable.add("No");
		} else {
			kvtable.add("Yes");
		}
		if (st.hasModule("Experience")) {
			kvtable.openRow().add("Experience");
			final int xp=Experience.getExperience(st,c);
			kvtable.add(xp+" XP").add("Level "+Experience.toLevel(st,xp));
		}
		kvtable.openRow().add(new Cell("<i>Assuming "+simulated+"</i>",5));
		kvtable.add(new Row(new Cell(new TextSubHeader("Attributes"),5)).resetNumbering());
		final Set<String> experiences=new HashSet<>();
		for (final Attribute a: st.getAttributes()) {
			if (a.getType()==EXPERIENCE) {
				experiences.add(a.getName());
			}
		}
		for (final Attribute a: st.getAttributes()) {
			String content="";
			boolean isxp=false;
			for (final String s: experiences) {
				if (s.equalsIgnoreCase(a.getName())) {
					isxp=true;
				}
			}
			if (isxp&&st.hasPermission("experience.award"+a.getName())) {
				content="<button id=\"award-"+a.getName()+"\" "+"style=\"margin: 0\" "+"type=submit onClick=\""+
				        "document.getElementById('award-"+a.getName()+"').style"+".display='none';"+
				        "document.getElementById('editor-award-"+a.getName()+"').style.display='block';"+
				        "\">Award</button>";
				final String target=values.get("target");
				final String ammountstring=values.get("xp-ammount");
				final String reason=values.getOrDefault("xp-reason","");
				int ammount=1;
				if (!ammountstring.isEmpty()) {
					try {
						ammount=Integer.parseInt(ammountstring);
					} catch (@Nonnull final NumberFormatException ignored) {
					}
				}
				content+="<div id=\"editor-award-"+a.getName()+"\" style=\"display: none;\">"+"<form method=post>"+
				         "<input type=hidden name=target value="+a.getName()+">"+
				         "Award <input type=text name=xp-ammount size=4 value="+ammount+
				         "> XP for <input type=text name=xp-reason size=60 value=\""+reason+"\"> <button "+
				         "type=submit name=Award value=Award>Award</button>"+"</form>"+"</div>";
				if (values.containsKey("Award")&&values.getOrDefault("target","").equalsIgnoreCase(a.getName())&&
				    !reason.isEmpty()) {
					final Pool p=Modules.getPool(st,"Experience."+a.getName());
					final GenericXPPool gen=(GenericXPPool)p;
					try {
						gen.awardXP(st,c,reason,ammount,false);
						content="<font color=green><b>&nbsp;&nbsp;&nbsp;OK: </b>Awarded "+ammount+" "+a.getName()+
						        " to "+c.getName()+"</font>";
					} catch (@Nonnull final UserException e) {
						content="<font color=red><b>&nbsp;&nbsp;&nbsp;Error: </b>"+e.getLocalizedMessage()+"</font>";
					}
				}
				
				f.noForm();
			}
			kvtable.openRow();
			kvtable.add(a.getName());
			//System.out.println("About to print attribute "+a.getName());
			final String value=a.getCharacterValue(simulated);
			kvtable.add(value!=null?value:"");
			StringBuilder description=new StringBuilder(a.getCharacterValueDescription(simulated));
			if (a.getType()==SET||a.getType()==INVENTORY) {
				description=new StringBuilder();
				final CharacterSet set;
				String actionURL="/GPHUD/configuration/sets/setset";
				String actionAddURL=actionURL;
				String attributeField="set";
				String setButton="Set";
				String elementField="element";
				if (a.getType()==INVENTORY) {
					set=new Inventory(simulated.getCharacter(),a);
					actionURL="/GPHUD/configuration/inventory/set";
					actionAddURL="/GPHUD/configuration/inventory/add";
					attributeField="inventory";
					setButton="Add";
					elementField="item";
				} else {
					set=new CharacterSet(simulated.getCharacter(),a);
				}
				if (st.hasPermission("Characters.Set"+a.getName())) {
					for (final Map.Entry<String,Integer> element: set.elements().entrySet()) {
						final Form create=new Form();
						create.inline();
						create.setAction(actionURL);
						create.add(new Hidden("character",simulated.getCharacter().getName()));
						create.add(new Hidden(attributeField,a.getName()));
						create.add(new Hidden(elementField,element.getKey()));
						create.add(new Hidden("qty",String.valueOf(element.getValue())));
						create.add(new Hidden("okreturnurl",st.getFullURL()));
						create.add(new Button(element.getValue()+"x"+element.getKey()));
						description.append(create.asHtml(simulated,true));
					}
					final Form create=new Form();
					create.inline();
					create.setAction(actionAddURL);
					create.add(new Hidden("character",simulated.getCharacter().getName()));
					create.add(new Hidden(attributeField,a.getName()));
					create.add(new Hidden("okreturnurl",st.getFullURL()));
					create.add(new Hidden("qty","1"));
					create.add(new Button(setButton));
					content=create.asHtml(simulated,true);
				} else {
					// can't edit set
					description=new StringBuilder(set.textList());
				}
			}
			kvtable.add(description.toString());
			kvtable.add(content);
		}
		if (st.hasModule("notes")) {
			ViewNotes.viewNotes(st,c.getOwner(),c,true,false);
		}
		for (final Pool pool: CharacterPool.getPools(st,c)) {
			f.add("<a href=\"/GPHUD/characters/viewpool/"+c.getId()+"/"+pool.fullName()+"\">View "+pool.fullName()+
			      " History</a><br>");
		}
		f.add("<a href=\"/GPHUD/characters/viewkv/"+c.getId()+"\">View Character Configuration (KV Store)</a><br>");
		f.add("<a href=\"/GPHUD/characters/audit/"+c.getId()+"\">View Character Audit Trail</a><br>");
	}
	
	@URLs(url="/characters/audit/*")
	public static void viewCharacterAudit(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		if (split.length==4) {
			final String id=split[split.length-1];
			final Char c=Char.get(Integer.parseInt(id));
			final State simulated=st.simulate(c);
			f.add("<a href=\"/GPHUD/characters/view/"+c.getId()+"\">View Character "+c.getName()+"</a><br>");
			f.add(new TextSubHeader("Audit Trail"));
			f.add(new AuditTable(st,"audit",values,c));
		} else {
			throw new UserInputValidationParseException("Failed to extract character id");
		}
	}
	
	@URLs(url="/characters/viewpool/*")
	public static void viewCharacterPool(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		if (split.length==5) {
			final String id=split[split.length-2];
			final String poolname=split[split.length-1];
			final Char c=Char.get(Integer.parseInt(id));
			final State simulated=st.simulate(c);
			f.add("<a href=\"/GPHUD/characters/view/"+c.getId()+"\">View Character "+c.getName()+"</a><br>");
			final Pool pool=Modules.getPool(st,poolname);
			f.add(new TextSubHeader("Pool Log: "+pool.fullName()));
			f.add("<p><i>"+pool.description()+(pool.isGenerated()?" (Instance Specific)":""));
			//GenericConfiguration.page(st,values,c,simulated);
			f.add(new PoolTable(st,"pool",values,c,poolname));
			
		} else {
			throw new UserInputValidationParseException("Failed to extract character id");
		}
	}
	
	@URLs(url="/characters/viewkv/*")
	public static void viewCharacterKV(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		if (split.length==4) {
			final String id=split[3];
			final Char c=Char.get(Integer.parseInt(id));
			final State simulated=st.simulate(c);
			f.add("<a href=\"/GPHUD/characters/view/"+c.getId()+"\">View Character "+c.getName()+"</a><br>");
			f.add(new TextSubHeader("KV Configuration"));
			GenericConfiguration.page(st,values,c,simulated);
			return;
		}
		if (split.length==6) {
			final String id=split[3];
			final Char c=Char.get(Integer.parseInt(id));
			final State simulated=st.simulate(c);
			f.noForm();
			f.add("<a href=\"/GPHUD/characters/view/"+c.getId()+"\">View Character "+c.getName()+"</a><br>");
			f.add(new TextSubHeader("KV Configuration"));
			f.add(new ConfigurationHierarchy(st,Modules.getKVDefinition(st,split[4]+"."+split[5]),simulated,values));
			return;
		}
		throw new UserInputValidationParseException("Failed to extract character id");
	}
	
	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Show yourself privately your own character sheet",
	          permitObject=false,
	          permitExternal=false)
	public static Response view(@Nonnull final State st) {
		return new OKResponse(st.getKV("instance.ViewSelfTemplate").value());
	}
	
	@Nonnull
	@Commands(context=Context.CHARACTER, description="Publicly display your own character sheet", permitExternal=false)
	public static Response show(@Nonnull final State st) {
		return new SayResponse(st.getKV("instance.ShowSelfTemplate").value());
	}
	
	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Look at another's character sheet",
	          permitObject=false,
	          permitExternal=false)
	public static Response look(@Nonnull final State st,
	                            @Nonnull
	                            @Arguments(type=Argument.ArgumentType.CHARACTER_NEAR,
	                                       name="character",
	                                       description="Character to inspect") final Char character) {
		character.validate(st);
		final State target=new State();
		target.setInstance(st.getInstance());
		target.setRegion(st.getRegion());
		target.setCharacter(character);
		return new OKResponse(target.getKV("instance.ViewOtherTemplate").value());
	}
	
	
}
