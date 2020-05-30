package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
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

public class Currency extends TableRow {


	private boolean selfvalidated=false;

	public Currency(int id) {super(id);}

	// ---------- STATICS ----------
	@Nonnull
	public static Currency find(State st,
	                            String name) {
		return get(GPHUD.getDB().dqinn("select id from currencies where instanceid=? and name like ?",st.getInstance().getId(),name));
	}

	public static Currency findNullable(State st,
	                                    String name) {
		try { return find(st,name); }
		catch (NoDataException e) { return null; }
	}

	@Nonnull
	public static Currency get(final int id) {
		return (Currency) factoryPut("Currency",id,new Currency(id));
	}

	public static void create(State st,
	                          String name) {
		if (findNullable(st,name)!=null) {
			throw new UserInputDuplicateValueException("A currency named "+name+" already exists");
		}
		GPHUD.getDB().d("insert into currencies(instanceid,name,basecoin,basecoinshort) values(?,?,?,?)",st.getInstance().getId(),name,name,name);
	}

	public static List<Currency> getAll(State state) {
		List<Currency> list=new ArrayList<>();
		for (Attribute attr: Attribute.getAttributes(state.getInstance())) {
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
	public void validate(@Nonnull State st) {
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
	 * Express the sum in short coin name format
	 *
	 * @param st Infers character
	 *
	 * @return
	 */
	public String shortSum(State st) {
		return textSum(st,false);
	}

	/**
	 * Express the sum in long coin name format
	 *
	 * @param st Infers character
	 *
	 * @return
	 */
	public String longSum(State st) {
		return textSum(st,true);
	}

	public void removeCoin(State st,
	                       int basevalue) {
		d("delete from currencycoins where currencyid=? and basemultiple=?",getId(),basevalue);
		Audit.audit(true,st,OPERATOR.AVATAR,null,null,"Delete",getName(),basevalue+"","","Removed coin of value "+basevalue);
	}

	public void setBaseCoinNames(State state,
	                             String basecoinshortname,
	                             String basecoinname) {
		String old=getBaseCoinName()+" ("+getBaseCoinNameShort()+")";
		set("basecoin",basecoinname);
		set("basecoinshort",basecoinshortname);
		Audit.audit(true,state,OPERATOR.AVATAR,null,null,getName(),"BaseCoins",old,basecoinname+" ("+basecoinshortname+")","Set base coin names");
	}

	public void addCoin(final @Nonnull State st,
	                    final int basevalue,
	                    final @Nonnull String coinshortname,
	                    final @Nonnull String coinname) {
		int existsbybase=dqinn("select count(*) from currencycoins where currencyid=? and basemultiple=?",getId(),basevalue);
		if (existsbybase>0) { throw new UserInputDuplicateValueException("You can not create another coin with base value "+basevalue); }
		int existsbynames=dqinn("select count(*) from currencycoins where currencyid=? and coinname like ? or coinnameshort like ?",getId(),coinname,coinshortname);
		if (existsbynames>0) { throw new UserInputDuplicateValueException("You can not create another coin with names "+coinname+" ("+coinshortname+")"); }
		d("insert into currencycoins(currencyid,coinname,coinnameshort,basemultiple) values(?,?,?,?)",getId(),coinname,coinshortname,basevalue);
		Audit.audit(true,st,OPERATOR.AVATAR,null,null,getName(),"Coin","",coinname,"Created coin "+coinname+" ("+coinshortname+") = "+basevalue+" "+getBaseCoinName());
	}

	public String textForm(int ammount,
	                       boolean longform) {
		if (ammount<0) { return "- "+textForm(-ammount,longform); }
		String result="";
		for (ResultsRow row: getCurrencyCoins()) {
			if (ammount >= row.getInt("basemultiple")) {
				if (!result.isEmpty()) { result+=" "; }
				int bycoin=ammount/row.getInt("basemultiple");
				result=result+bycoin;
				if (longform) { result+=" "+row.getString("coinname"); }
				else { result+=row.getString("coinnameshort"); }
				ammount=ammount-(bycoin*row.getInt("basemultiple"));
			}
		}
		if (ammount>0) {
			if (!result.isEmpty()) { result+=" "; }
			result+=ammount;
			if (longform) { result+=" "+getBaseCoinName(); }
			else { result+=getBaseCoinNameShort(); }
		}
		return result;
	}

	public String shortTextForm(int ammount) { return textForm(ammount,false); }

	public String longTextForm(int ammount) { return textForm(ammount,true); }

	public List<Coin> getCoins() {
		List<Coin> list=new ArrayList<>();
		for (ResultsRow row: getCurrencyCoins()) {
			list.add(new Coin(row.getInt("basemultiple"),row.getString("coinname"),row.getString("coinnameshort")));
		}
		return list;
	}

	public int sum(State st) {
		return getPool(st).sum(st);
	}

	public String getBaseCoinName() { return getString("basecoin"); }

	public String getBaseCoinNameShort() { return getString("basecoinshort"); }

	// ----- Internal Instance -----

	@Nonnull
	@Override
	public String getTableName() {
		return "currencies";
	}

	public void delete(State st) {
		if (st.getInstance()!=getInstance()) { throw new SystemConsistencyException("Currency delete instance/state instance mismatch"); }
		getPool(st).delete(st);
		d("delete from currencies where id=?",getId());
	}

	/**
	 * Count the number of entries a character has
	 */
	public int entries(State st,
	                   Char ch) {
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
	public void spawnInAsSystem(State st,
	                            Char character,
	                            int ammount,
	                            String description) {
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
	public void spawnInAsAdmin(State st,
	                           Char character,
	                           int ammount,
	                           String description) {
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
	public void spawnInByChar(State st,
	                          Char character,
	                          int ammount,
	                          String description) {
		getPool(st).addChar(st,character,ammount,description);
	}

	public Instance getInstance() {
		return Instance.get(dqinn("select instanceid from currencies where id=?",getId()));
	}

	public Pool getPool(State st) {
		return Modules.getPool(st,"Currency."+getName());
	}

	@Override
	protected int getNameCacheTime() {
		return 0;
	}

	private String textSum(State st,
	                       boolean longform) {
		return textForm(sum(st),longform);
	}

	private Results getCurrencyCoins() {
		return dq("select basemultiple,coinname,coinnameshort from currencycoins where currencyid=? order by basemultiple desc",getId());
	}

	public class Coin {
		public int value;
		public String basecoinname;
		public String basecoinnameshort;

		public Coin(int value,
		            String basecoinname,
		            String basecoinnameshort) {
			this.value=value;
			this.basecoinname=basecoinname;
			this.basecoinnameshort=basecoinnameshort;
		}
	}
}
