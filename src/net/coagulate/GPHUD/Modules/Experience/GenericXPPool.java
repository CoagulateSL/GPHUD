package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterPool;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public class GenericXPPool extends Pool {
	private final String myname;

	public GenericXPPool(final String name) {
		super();
		myname=name;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() { return true; }

	@Nonnull
	@Override
	public String description() { return myname+" pool"; }

	@Nonnull
	@Override
	public String fullName() { return "Experience."+myname; }

	@Nonnull
	@Override
	public String name() { return myname; }

	public void awardXP(@Nonnull final State st,
	                    @Nonnull final Char target,
	                    @Nullable final String reason,
	                    final int ammount,
	                    final boolean incontext) {
		final State targetstate=State.getNonSpatial(target);
		final float period=targetstate.getKV(fullName()+"XPPeriod").floatValue();
		final int maxxp=targetstate.getKV(fullName()+"XPLimit").intValue();
		final Pool pool=Modules.getPool(targetstate,"Experience."+myname);
		final int awarded=CharacterPool.sumPoolDays(target,pool,period);
		if (awarded >= maxxp) {
			throw new UserInputStateException("This character has already reached their "+pool.name()+" XP limit.  They will next be eligable for a point in "+CharacterPool.poolNextFree(
					target,
					pool,
					maxxp,
					period
			                                                                                                                                                                             ),true);
		}
		if ((awarded+ammount)>maxxp) {
			throw new UserInputStateException("This will push the character beyond their "+pool.name()+" XP limit, they can be awarded "+(maxxp-awarded)+" XP right now",true);
		}
		if (reason==null) { throw new UserInputEmptyException("You must supply a reason",true); }
		// else award xp :P
		int maxLevel=targetstate.getKV("Experience.MaxLevel").intValue();
		if (maxLevel==0) { maxLevel=1000; }
		if (targetstate.getCharacter().getLevel(targetstate)>=maxLevel) { throw new UserInputStateException("This character is already at maximum level and can not be awarded any more experience of any type",true); }
		Audit.audit(st,Audit.OPERATOR.CHARACTER,null,target,"Pool Add",pool.name(),null,ammount+"",reason);
		CharacterPool.addPool(st,target,pool,ammount,reason);
		if (target!=st.getCharacterNullable()) {
			if (incontext) {
				target.hudMessage("You were granted "+ammount+" point"+(ammount==1?"":"s")+" of "+pool.name()+" XP by "+st.getCharacter().getName()+" for "+reason);
			}
			else {
				target.hudMessage("You were granted "+ammount+" point"+(ammount==1?"":"s")+" of "+pool.name()+" XP by (("+st.getAvatar().getName()+")) for "+reason);
			}
		}
	}

}
