package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.EXPERIENCE;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.*;
import static net.coagulate.GPHUD.Modules.Command.Context.AVATAR;

/**
 * Dynamic events menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ExperienceModule extends ModuleAnnotation {

	public ExperienceModule(final String name, final ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
	}

	@Nonnull
	@Commands(context = AVATAR, description = "Award XP")
	public static Response award(@Nonnull final State st,
	                             @Nonnull @Arguments(description = "Character to award to", type = CHARACTER) final
	                             Char target,
	                             @Nonnull @Arguments(description = "XP type to award", type = TEXT_INTERNAL_NAME, max = 32) final
	                                 String type,
	                             @Nullable @Arguments(description = "Ammount to award", type = INTEGER, max = 999999)
			                             Integer ammount,
	                             @Arguments(description = "Reason for award", type = TEXT_ONELINE, max = 128) final
	                                 String reason

	) {
		boolean incontext = false;
		boolean permitted = false;
		if (ammount == null) { ammount = 1; }
		final Attribute attr = st.getAttribute(type);
		if (attr==null) { return new ErrorResponse("Unable to find experience type "+type); }
		if (attr.getType() != EXPERIENCE) {
			return new ErrorResponse("This attributes is not of type EXPERIENCE, (try omitting XP off the end, if present)");
		}
		if (st.hasPermission("Experience.award" + attr.getName() + "XP")) { permitted = true; }
		if (!permitted) {
			String subtype = attr.getSubType();
			if (subtype == null) { subtype = ""; }
			if (!subtype.isEmpty()) {
				// what is our group of this type then?
				final CharacterGroup group = st.getCharacter().getGroup(subtype);
				// what is their blah
				final CharacterGroup theirgroup = target.getGroup(subtype);
				if (group == null || theirgroup == null) {
					return new ErrorResponse("You lack admin permissions, and you/they are not in a group of type " + subtype);
				}
				if (group.getId() != theirgroup.getId()) {
					return new ErrorResponse("You lack admin permissions, or you are in a different group of type " + subtype);
				}
				if (group.isAdmin(st.getCharacter())) { permitted = true; } else {
					return new ErrorResponse("You lack admin permissions, and are not a group admin/owner for " + group.getName());
				}
				incontext = true;
			}
		}
		if (!permitted) {
			return new ErrorResponse("You require permission Experience.award" + attr.getName() + "XP to execute this command");
		}
		final Pool p = Modules.getPool(st, "Experience." + type + "XP");
		final GenericXPPool gen = (GenericXPPool) p;
		gen.awardXP(st, target, reason, ammount, incontext);
		return new OKResponse("Awarded " + ammount + " " + p.name() + " to " + target.getName());
	}

	@Override
	public void validatePermission(final State st, @Nonnull final String permission) {
		// really can't validate these as they can be dynamic
	}

	@Override
	public Map<String, Permission> getPermissions(@Nonnull final State st) {
		final Map<String, Permission> perms = super.getPermissions(st);
		for (final Attribute a : st.getAttributes()) {
			if (a.getType() == EXPERIENCE) {
				perms.put("award" + a.getName() + "XP", new GenericXPAwardPermission(a.getName()));
			}
		}
		return perms;
	}

	@Override
	public Permission getPermission(@Nonnull final State st, @Nonnull final String itemname) {
		final Map<String, Permission> perms = getPermissions(st);
		for (final Map.Entry<String, Permission> entry : perms.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(itemname)) {
				return entry.getValue();
			}
		}
		return super.getPermission(st, itemname);
	}

	@Nonnull
	@Override
	public Map<String, Pool> getPoolMap(@Nullable final State st) {
		final Map<String, Pool> pools = new HashMap<>(poolmap);
		if (st != null) {
			if (st.getInstanceNullable() == null) { return pools; }
			for (final Attribute attr : st.getAttributes()) {
				if (attr.getType() == EXPERIENCE) {
					pools.put(attr.getName() + "XP", new GenericXPPool(attr.getName()));
				}
			}
		}
		return pools;
	}

	@Override
	public Pool getPool(final State st, @Nonnull final String itemname) {
		final Map<String, Pool> pmap = getPoolMap(st);
		for (final Map.Entry<String, Pool> entry : pmap.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(itemname)) { return entry.getValue(); }
		}
		throw new SystemException("Unable to retrieve pool " + itemname);
	}

	@Override
	public KV getKVDefinition(@Nonnull final State st, @Nonnull final String qualifiedname) throws SystemException {
		if (kvmap.containsKey(qualifiedname.toLowerCase())) {
			return kvmap.get(qualifiedname.toLowerCase());
		}
		final Map<String, KV> map = getKVDefinitions(st);
		for (final Map.Entry<String, KV> entry : map.entrySet()) { if (entry.getKey().equalsIgnoreCase(qualifiedname)) { return entry.getValue(); } }
		throw new SystemException("Invalid KV " + qualifiedname + " in module " + getName());
	}

	@Nonnull
	@Override
	public Map<String, KV> getKVDefinitions(@Nonnull final State st) {
		final Map<String, KV> map = new TreeMap<>(kvmap);
		if (st.getInstanceNullable() == null) { return map; }
		for (final Attribute attr : st.getAttributes()) {
			if (attr.getType() == EXPERIENCE) {
				map.put(attr.getName() + "XPPeriod", new GenericXPPeriodKV(attr.getName() + "XPPeriod"));
				map.put(attr.getName() + "XPLimit", new GenericXPLimitKV(attr.getName() + "XPLimit"));
			}
		}
		return map;
	}

	@Nonnull
	@Override
	public Set<CharacterAttribute> getAttributes(@Nonnull final State st) {
		final Set<CharacterAttribute> ret = new HashSet<>();
		if (st.hasModule("Events")) { ret.add(new EventXP(-1)); }
		ret.add(new VisitXP(-1));
		if (st.hasModule("Faction")) { ret.add(new FactionXP(-1)); }
		for (final Attribute attr : st.getAttributes()) {
			if (attr.getType() == EXPERIENCE) {
				ret.add(new GenericXP(attr.getName() + "XP"));
			}
		}
		return ret;
	}

}
