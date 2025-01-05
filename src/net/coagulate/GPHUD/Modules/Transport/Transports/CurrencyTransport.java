package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

/** Transports currency definitions only.  Not character balances. */
public class CurrencyTransport extends Transporter {
	@Override
	public String description() {
		return "Currency and coins configuration";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Currency.getAll(st).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Currency c=Currency.find(st,element);
		exportTo.put("basecoin",c.getBaseCoinName());
		exportTo.put("basecoinshort",c.getBaseCoinNameShort());
		final JSONArray coins=new JSONArray();
		exportTo.put("coins",coins);
		for (final Currency.Coin coin: c.getCoins()) {
			final JSONObject coinSerial=new JSONObject();
			coinSerial.put("coinname",coin.basecoinname);
			coinSerial.put("coinnameshort",coin.basecoinnameshort);
			coinSerial.put("basemultiple",coin.value);
			coins.put(coinSerial);
		}
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final Currency check=Currency.findNullable(state,name);
		if (check==null) {
			report.info("Currency - Create new currency "+name);
			if (simulation) {
				return;
			}
			Currency.create(state,name);
			Audit.audit(state,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Import Currency",
			            "Create Currency",
			            null,
			            name,
			            "Created new currency via import");
		}
		
		final Currency c=Currency.find(state,name);
		importValue(state,
		            simulation,
		            report,
		            name,
		            "Base Coin Name",
		            c.getBaseCoinName(),
		            element.getString("basecoin"),
		            ()->c.setBaseCoinNames(state,element.getString("basecoinshort"),element.getString("basecoin")));
		importValue(state,
		            simulation,
		            report,
		            name,
		            "Base Coin Name Short",
		            c.getBaseCoinNameShort(),
		            element.getString("basecoinshort"),
		            ()->c.setBaseCoinNames(state,element.getString("basecoinshort"),element.getString("basecoin")));
		final JSONArray coinsArray=element.getJSONArray("coins");
		if (simulation) {
			return;
		}
		for (final Currency.Coin coin: c.getCoins()) {
			c.removeCoin(state,coin.value);
		}
		for (int i=0;i<coinsArray.length();i++) {
			final JSONObject coin=coinsArray.getJSONObject(i);
			final String coinName=coin.getString("coinname");
			final String coinNameShort=coin.getString("coinnameshort");
			final int baseMultiple=coin.getInt("basemultiple");
			c.addCoin(state,baseMultiple,coinNameShort,coinName);
		}
	}
}
