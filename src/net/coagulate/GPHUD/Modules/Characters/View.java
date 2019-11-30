package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
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

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;
import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.EXPERIENCE;

/**
 * A simple command implementation.
 * Used by all interfaces for a general purpose "status" page.
 * Rather technical, like a thing hidden in the "About" menu ;)
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class View {

	//TODO - FIX THE "boolean full=" section of all these functions, and decide what data is private as a result of this

	@Nonnull
	@Commands(context = Context.ANY, description = "Get status of this connection")
	public static Response status(@Nonnull State st) {
		TabularResponse t = new TabularResponse();
		t.openRow();
		t.add("Node").addNoNull((GPHUD.DEV ? "DEVELOPMENT // " : "Production // ") + Interface.getNode());
		t.openRow();
		t.add("Avatar").addNoNull(st.avatar().getGPHUDLink());
		t.openRow();
		t.add("Character").addNoNull(st.getCharacter());
		t.openRow();
		t.add("Instance").addNoNull(st.getInstanceNullable());
		t.openRow();
		t.add("Region").addNoNull(st.getRegionNullable());
		t.openRow();
		t.add("Zone").addNoNull(st.zone);
		t.openRow();
		t.add("Source").addNoNull(st.sourcename);
		t.openRow();
		t.add("SourceOwner").addNoNull(st.sourceowner.getGPHUDLink());
		t.openRow();
		t.add("SourceDev").addNoNull(st.sourcedeveloper.getGPHUDLink());
		return t;
	}

	@URLs(url = "/characters/view/*")
	public static void viewCharacter(@Nonnull State st, @Nonnull SafeMap values) throws UserException, SystemException {
		st.form.noForm();
		//System.out.println(st.uri);
		String[] split = st.getDebasedURL().split("/");
		//System.out.println(split.length);
		if (split.length == 4) {
			String id = split[split.length - 1];
			Char c = Char.get(Integer.parseInt(id));
			viewCharacter(st, values, c, false);
			return;
		}
		if (split.length == 6) {
			String id = split[3];
			Char c = Char.get(Integer.parseInt(id));
			String attribute = split[4] + "." + split[5];
			st.form.add(new ConfigurationHierarchy(st, st.getKVDefinition(attribute), st.simulate(c), values));
			return;
		}
		throw new SystemException("Unknown character view mode (length:" + split.length + " URI:" + st.getDebasedURL());
	}

	public static void viewCharacter(@Nonnull State st, @Nonnull SafeMap values, @Nonnull Char c, boolean brief) throws UserException, SystemException {
		boolean full = false;
		State simulated = st.simulate(c);
		String tz = st.avatar().getTimeZone();
		if (st.getCharacterNullable() == c) { full = true; }
		if (st.hasPermission("Characters.ViewAll")) { full = true; }
		Form f = st.form;
		f.add(new TextSubHeader(c.getName()));
		Table kvtable = new Table();
		f.add(kvtable);
		kvtable.openRow().add("Owning Avatar").add(c.getOwner().getGPHUDLink());
		String lastplayed = fromUnixTime(c.getLastPlayed(), tz);
		kvtable.openRow().add("Last Played").add(lastplayed).add(tz);
		kvtable.openRow().add("Connected");
		if (c.getURL() == null || c.getURL().isEmpty()) { kvtable.add("No"); } else { kvtable.add("Yes"); }
		if (st.hasModule("Experience")) {
			kvtable.openRow().add("Experience");
			int xp = Experience.getExperience(st, c);
			kvtable.add(xp + " XP").add("Level " + Experience.toLevel(st, xp));
		}
		kvtable.openRow().add(new Cell("<i>Assuming " + simulated.toString() + "</i>", 5));
		kvtable.openRow().add(new Cell(new TextSubHeader("Attributes"), 5));
		Set<String> experiences = new HashSet<>();
		for (Attribute a : st.getAttributes()) {
			if (a.getType() == EXPERIENCE) { experiences.add(a.getName() + "XP"); }
		}
		for (Attribute a : st.getAttributes()) {
			String content = "";
			boolean isxp = false;
			for (String s : experiences) { if (s.equalsIgnoreCase(a.getName())) { isxp = true; }}
			if (isxp && st.hasPermission("experience.award" + a.getName())) {
				content = "<button id=\"award-" + a.getName() + "\" "
						+ "style=\"margin: 0\" "
						+ "type=submit onClick=\""
						+ "document.getElementById('award-" + a.getName() + "').style.display='none';"
						+ "document.getElementById('editor-award-" + a.getName() + "').style.display='block';"
						+ "\">Award</button>";
				String target = values.get("target");
				String ammountstring = values.get("xp-ammount");
				String reason = values.getOrDefault("xp-reason", "");
				int ammount = 1;
				if (ammountstring != null && !ammountstring.isEmpty()) {
					try { ammount = Integer.parseInt(ammountstring); } catch (NumberFormatException e) {}
				}
				content += "<div id=\"editor-award-" + a.getName() + "\" style=\"display: none;\">"
						+ "<form method=post>"
						+ "<input type=hidden name=target value=" + a.getName() + ">"
						+ "Award <input type=text name=xp-ammount size=4 value=" + ammount + "> XP for <input type=text name=xp-reason size=60 value=\"" + reason + "\"> <button type=submit name=Award value=Award>Award</button>"
						+ "</form>"
						+ "</div>";
				if (values.containsKey("Award") && values.getOrDefault("target", "").equalsIgnoreCase(a.getName()) && !reason.isEmpty()) {
					Pool p = Modules.getPool(st, "Experience." + a.getName());
					GenericXPPool gen = (GenericXPPool) p;
					try {
						gen.awardXP(st, c, reason, ammount, false);
						content = "<font color=green><b>&nbsp;&nbsp;&nbsp;OK: </b>Awarded " + ammount + " " + a.getName() + " to " + c.getName() + "</font>";
					} catch (UserException e) {
						content = "<font color=red><b>&nbsp;&nbsp;&nbsp;Error: </b>" + e.getLocalizedMessage() + "</font>";
					}
				}

				f.noForm();
			}
			kvtable.openRow();
			kvtable.add(a.getName());
			//System.out.println("About to print attribute "+a.getName());
			kvtable.add(a.getCharacterValue(simulated));
			kvtable.add(a.getCharacterValueDescription(simulated));
			kvtable.add(content);
		}
		if (st.hasModule("notes")) {
			ViewNotes.viewNotes(st,c.getOwner(), c,true );
		}
		if (brief) { return; }
		f.add(new TextSubHeader("KV Configuration"));
		GenericConfiguration.page(st, values, c, simulated);
		f.add(new TextSubHeader("Audit Trail"));
		f.add(Audit.formatAudit(Audit.getAudit(st.getInstance(), null, c), st.avatar().getTimeZone()));
	}

	@Nonnull
	@Commands(context = Context.CHARACTER, description = "Show yourself privately your own character sheet",permitObject = false)
	public static Response view(@Nonnull State st) {
		return new OKResponse(st.getKV("instance.ViewSelfTemplate").value());
	}

	@Nonnull
	@Commands(context = Context.CHARACTER, description = "Publicly display your own character sheet")
	public static Response show(@Nonnull State st) {
		return new SayResponse(st.getKV("instance.ShowSelfTemplate").value());
	}

	@Nonnull
	@Commands(context = Context.CHARACTER, description = "Look at another's character sheet",permitObject = false)
	public static Response look(@Nonnull State st,
	                            @Nonnull @Arguments(type = Argument.ArgumentType.CHARACTER_NEAR, description = "Character to inspect")
			                            Char character) {
		character.validate(st);
		State target = new State();
		target.setInstance(st.getInstance());
		target.setRegion(st.getRegion());
		target.setCharacter(character);
		return new OKResponse(target.getKV("instance.ViewOtherTemplate").value());
	}


}
