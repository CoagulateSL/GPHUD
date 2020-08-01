package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Currency extends TableRow {


	private boolean selfvalidated;

	public Currency(final int id) {super(id);}

	// ---------- STATICS ----------
	@Nonnull
	public static Currency find(final State st,
	                            final String name) {
		Cache<Currency> cache=Cache.getCache("GPHUD-currencynames-" + st.getInstance().getId());
		try { return cache.get(name); }
		catch (Cache.CacheMiss e) {
			return cache.put(name,get(GPHUD.getDB().dqinn("select id from currencies where instanceid=? and name like ?", st.getInstance().getId(), name)),300);
		}
	}

	public static Currency findNullable(final State st,
	                                    final String name) {
		try { return find(st,name); }
		catch (final NoDataException e) { return null; }
	}

	@Nonnull
	public static Currency get(final int id) {
		return (Currency) factoryPut("Currency",id,new Currency(id));
	}

	public static void create(final State st,
	                          final String name) {
		if (findNullable(st,name)!=null) {
			throw new UserInputDuplicateValueException("A currency named "+name+" already exists");
		}
		GPHUD.getDB().d("insert into currencies(instanceid,name,basecoin,basecoinshort) values(?,?,?,?)",st.getInstance().getId(),name,name,name);
	}

	public static List<Currency> getAll(final State state) {
		final List<Currency> list=new ArrayList<>();
		for (final Attribute attr: Attribute.getAttributes(state.getInstance())) {
			if (attr.getType()==ATTRIBUTETYPE.CURRENCY) {
				list.add(Currency.find(state,attr.getName()));
			}
		}
		return list;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getIdColumn() {
		return "id";
	}

	@Override
	public void validate(@Nonnull final State st) {
		if (selfvalidated) { return; }
		super.validate();
		if (st.getInstance()!=getInstance()) { throw new SystemConsistencyException("Currency instance/state instance mismatch"); }
		selfvalidated=true;
	}

	@Nullable
	@Override
	public String getNameField() {
		return "name";
	}

	@Nullable
	@Override
	public String getLinkTarget() {
		return null;
	}

	@Nullable
	@Override
	public String getKVTable() {
		return null;
	}

	@Nullable
	@Override
	public String getKVIdField() {
		return null;
	}

	/**
	 * Turn a string into a base coin ammount
	 */
	public int decode(String ammount) {
		// if its just a number, lets bail fast
		if (Pattern.compile("[0-9]*").matcher(ammount).matches()) { return Integer.parseInt(ammount); }

		// right, what can we do.... split it into groups of numbers and non numbers I guess
		final Pattern pattern=Pattern.compile("([0-9]+)([^0-9]*)([0-9]?.*)");  // splits into number, coin, remainder
		int total=0;
		while (!ammount.trim().isEmpty()) {
			final Matcher matcher=pattern.matcher(ammount);
			if (matcher.matches()) {
				//System.out.println("Split gives us ["+matcher.group(1)+"]["+matcher.group(2)+"]["+matcher.group(3)+"]");
				final int qty=Integer.parseInt(matcher.group(1));
				final String coin=matcher.group(2);
				// resolve coin?
				final Coin coinobject=findCoin(coin);
				final int value=coinobject.value*qty;
				total+=value;
				//System.out.println("ACCUMULATE "+qty+" "+coin+" gave us "+value+" totalling "+total);
				ammount=matcher.group(3);
			}
			else {
				//System.out.println("Not a match");
				throw new UserInputValidationParseException("Unable to parse '"+ammount+"' into an ammount of currency");
			}
		}
		return total;
	}

	/**
	 * Express the sum in short coin name format
	 *
	 * @param st Infers character
	 *
	 * @return the current sum using short coin notation
	 */
	public String shortSum(final State st) {
		return textSum(st,false);
	}

	/**
	 * Express the sum in long coin name format
	 *
	 * @param st Infers character
	 *
	 * @return the current sum using long coin notation
	 */
	public String longSum(final State st) {
		return textSum(st,true);
	}

	public void removeCoin(final State st,
	                       final int basevalue) {
		d("delete from currencycoins where currencyid=? and basemultiple=?",getId(),basevalue);
		Audit.audit(true,st,OPERATOR.AVATAR,null,null,"Delete",getName(),basevalue+"","","Removed coin of value "+basevalue);
	}

	public void setBaseCoinNames(final State state,
	                             final String basecoinshortname,
	                             final String basecoinname) {
		final String old=getBaseCoinName()+" ("+getBaseCoinNameShort()+")";
		set("basecoin",basecoinname);
		set("basecoinshort",basecoinshortname);
		Audit.audit(true,state,OPERATOR.AVATAR,null,null,getName(),"BaseCoins",old,basecoinname+" ("+basecoinshortname+")","Set base coin names");
	}

	public void addCoin(@Nonnull final State st,
	                    final int basevalue,
	                    @Nonnull final String coinshortname,
	                    @Nonnull final String coinname) {
		final int existsbybase=dqinn("select count(*) from currencycoins where currencyid=? and basemultiple=?",getId(),basevalue);
		if (existsbybase>0) { throw new UserInputDuplicateValueException("You can not create another coin with base value "+basevalue); }
		final int existsbynames=dqinn("select count(*) from currencycoins where currencyid=? and coinname like ? or coinnameshort like ?",getId(),coinname,coinshortname);
		if (existsbynames>0) { throw new UserInputDuplicateValueException("You can not create another coin with names "+coinname+" ("+coinshortname+")"); }
		if (basevalue<2) {
			throw new UserInputInvalidChoiceException("You should not map a coin to a value less than 2, you should rename basecoins to alter the most basic currency unit");
		}
		d("insert into currencycoins(currencyid,coinname,coinnameshort,basemultiple) values(?,?,?,?)",getId(),coinname,coinshortname,basevalue);
		Audit.audit(true,st,OPERATOR.AVATAR,null,null,getName(),"Coin","",coinname,"Created coin "+coinname+" ("+coinshortname+") = "+basevalue+" "+getBaseCoinName());
	}

	public String textForm(int ammount,
	                       final boolean longform) {
		if (ammount<0) { return "- "+textForm(-ammount,longform); }
		final StringBuilder result=new StringBuilder();
		for (final ResultsRow row: getCurrencyCoins()) {
			if (ammount >= row.getInt("basemultiple")) {
				if (result.length()>0) { result.append(" "); }
				final int bycoin=ammount/row.getInt("basemultiple");
				result.append(bycoin);
				if (longform) { result.append(" ").append(row.getString("coinname")); }
				else { result.append(row.getString("coinnameshort")); }
				ammount=ammount-(bycoin*row.getInt("basemultiple"));
			}
		}
		if (ammount>0) {
			if (result.length()>0) { result.append(" "); }
			result.append(ammount);
			if (longform) { result.append(" ").append(getBaseCoinName()); }
			else { result.append(getBaseCoinNameShort()); }
		}
		return result.toString();
	}

	public String shortTextForm(final int ammount) { return textForm(ammount,false); }

	public String longTextForm(final int ammount) { return textForm(ammount,true); }

	public List<Coin> getCoins() {
		final List<Coin> list=new ArrayList<>();
		for (final ResultsRow row: getCurrencyCoins()) {
			list.add(new Coin(row.getInt("basemultiple"),row.getString("coinname"),row.getString("coinnameshort")));
		}
		return list;
	}

	public int sum(final State st) {
		return getPool(st).sum(st);
	}

	public String getBaseCoinName() { return getString("basecoin"); }

	public String getBaseCoinNameShort() { return getString("basecoinshort"); }

	public Coin findCoin(String coin) {
		// trim the coin down
		coin=coin.replaceAll(",","");
		coin=coin.trim();

		// check base coin names
		if (coin.isEmpty() || coin.equalsIgnoreCase(getBaseCoinName()) || coin.equalsIgnoreCase(getBaseCoinNameShort())) {
			return new Coin(1,getBaseCoinName(),getBaseCoinNameShort());
		}
		// check coin long names
		final Results longname=dq("select * from currencycoins where coinname like ? and currencyid=?",coin,getId());
		if (longname.size()==1) {
			return new Coin(longname.first().getInt("basemultiple"),longname.first().getString("coinname"),longname.first().getString("coinnameshort"));
		}
		// no? ok
		final Results shortname=dq("select * from currencycoins where coinnameshort like ? and currencyid=?",coin,getId());
		if (shortname.size()==1) {
			return new Coin(shortname.first().getInt("basemultiple"),shortname.first().getString("coinname"),shortname.first().getString("coinnameshort"));
		}
		throw new UserInputLookupFailureException("Unable to resolve a coin named "+coin);
	}

	public void delete(final State st) {
		if (st.getInstance()!=getInstance()) { throw new SystemConsistencyException("Currency delete instance/state instance mismatch"); }
		getPool(st).delete(st);
		d("delete from currencies where id=?",getId());
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "currencies";
	}

	/**
	 * Count the number of entries a character has
	 */
	public int entries(final State st,
	                   final Char ch) {
		return getPool(st).entries(st,ch);
	}

	/**
	 * Spawn currency transaction into this account from the SYSTEM.
	 *
	 * @param st          - State
	 * @param character   - Target character
	 * @param ammount     - Ammount of base currency
	 * @param description - Description for the transaction
	 */
	public void spawnInAsSystem(final State st,
	                            final Char character,
	                            final int ammount,
	                            final String description) {
		getPool(st).addSystem(st,character,ammount,description);
	}

	/**
	 * Spawn currency transaction into this account from an administrator.
	 *
	 * @param st          - State - infers the avatar (admin)
	 * @param character   - Target character
	 * @param ammount     - Ammount of base currency
	 * @param description - Description for the transaction
	 */
	public void spawnInAsAdmin(final State st,
	                           final Char character,
	                           final int ammount,
	                           final String description) {
		getPool(st).addAdmin(st,character,ammount,description);
		Audit.audit(true,st,OPERATOR.AVATAR,null,character,"Create",getName(),null,ammount+"","Admin spawned in currency: "+description);
	}

	/**
	 * Spawn currency transaction into this account from a character (does not debit them).
	 *
	 * @param st          - State - infers the source character
	 * @param character   - Target character
	 * @param ammount     - Ammount of base currency
	 * @param description - Description for the transaction
	 */
	public void spawnInByChar(final State st,
	                          final Char character,
	                          final int ammount,
	                          final String description) {
		getPool(st).addChar(st,character,ammount,description);
	}

	public Pool getPool(final State st) {
		return Modules.getPool(st,"Currency."+getName());
	}

	public Instance getInstance() {
		return Instance.get(dqinn("select instanceid from currencies where id=?",getId()));
	}

	public boolean tradable() {
		return getBool("tradable");
	}

	@Override
	protected int getNameCacheTime() { return 60*60; }

	private String textSum(final State st,
	                       final boolean longform) {
		return textForm(sum(st),longform);
	}

	private Results getCurrencyCoins() {
		return dq("select basemultiple,coinname,coinnameshort from currencycoins where currencyid=? order by basemultiple desc",getId());
	}

	public static class Coin {
		public final int value;
		public final String basecoinname;
		public final String basecoinnameshort;

		public Coin(final int value,
		            final String basecoinname,
		            final String basecoinnameshort) {
			this.value=value;
			this.basecoinname=basecoinname;
			this.basecoinnameshort=basecoinnameshort;
		}
	}

	public void tradable(@Nonnull final State state,
	                     final boolean tradable) {
		final boolean oldvalue=tradable();
		if (tradable==oldvalue) { return; }
		set("tradable",tradable);
		Audit.audit(true,state,OPERATOR.AVATAR,null,null,"Currency","Tradable",oldvalue+"",tradable+"","Admin set currency to tradable status "+tradable);
	}

	// ----- Internal Instance -----

}
