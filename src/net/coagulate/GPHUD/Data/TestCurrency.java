package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.Tests.TestFramework;
import net.coagulate.SL.TestFrameworkPrototype;

public class TestCurrency extends TestTools {
	
	private static void createCurrency(final TestFramework t) {
		// enable currency module
		t.primaryHUD.state.setKV(t.instance,"Currency.Enabled", "true");
		Currency.create(t.primaryHUD.state,"Gold");
		Attribute.create(t.primaryHUD.state,"Gold",false,Attribute.ATTRIBUTETYPE.CURRENCY,null,false,false, "0");
		Currency gold=getCurrency(t);
		gold.setBaseCoinNames(t.primaryHUD.state, "c", "copper");
		gold.addCoin(t.primaryHUD.state,100, "s","silver");
		gold.addCoin(t.primaryHUD.state,10000, "g","gold");
	}
	
	private static Currency getCurrency(final TestFramework t) {
		return Currency.find(t.primaryHUD.state,"Gold");
	}
	
	private static void deleteCurrency(final TestFramework t) {
		Attribute.find(t.primaryHUD.state.getInstance(),"gold").delete(t.primaryHUD.state);
	}
	
	@TestFramework.Test(name="Test currency")
	public static TestFrameworkPrototype.TestOutput testCurrencyCreation(final TestFramework t) {
		createCurrency(t);
		deleteCurrency(t);
		return new TestFrameworkPrototype.TestOutput(true, "Created and deleted currency");
	}
	@TestFramework.Test(name="Test currency deletion")
	public static TestFrameworkPrototype.TestOutput testCurrencyAfterDelete(final TestFramework t) {
		createCurrency(t);
		Currency currency=getCurrency(t);
		deleteCurrency(t);
		Currency postDelete=null;
		try { postDelete=getCurrency(t); } catch (Exception ignore) {}
		return new TestFrameworkPrototype.TestOutput(postDelete==null, "Currency find() fails (expectedly) after deletion");
	}
	@TestFramework.Test(name="Test currency attribute deletion")
	public static TestFrameworkPrototype.TestOutput testCurrencyAttributeAfterDelete(final TestFramework t) {
		createCurrency(t);
		Currency currency=getCurrency(t);
		Attribute attr=Attribute.find(t.primaryHUD.character.getInstance(),"gold");
		deleteCurrency(t);
		Attribute postDelete=null;
		try { postDelete=Attribute.find(t.primaryHUD.character.getInstance(),"gold"); } catch (Exception ignore) {}
		return new TestFrameworkPrototype.TestOutput(postDelete==null, "Currency attribute find() fails (expectedly) after deletion");
	}
	
	@TestFramework.Test(name="Test currency and formatting")
	public static TestFrameworkPrototype.TestOutput testCurrencyFormatting(final TestFramework t) {
		createCurrency(t);
		Currency currency=getCurrency(t);
		Pool pool=currency.getPool(t.primaryHUD.state);
		pool.addSystem(t.primaryHUD.state,t.primaryHUD.character,10000,"Test entry");
		pool.addSystem(t.primaryHUD.state,t.primaryHUD.character,204,"Test addition");
		pool.addSystem(t.primaryHUD.state,t.primaryHUD.character,-1,"Test subtraction");
		int sum=pool.sum(t.primaryHUD.state);
		String shortform=currency.shortTextForm(sum);
		String longform=currency.longTextForm(sum);
		deleteCurrency(t);
		return new TestFrameworkPrototype.TestOutput("1g 2s 3c".equals(shortform) && "1 gold 2 silver 3 copper".equals(longform), "Pool sum (1g2s3c) of "+sum+" short presents as "+shortform+" long form "+longform);
	}
	
	@TestFramework.Test(name="Test currency recoining and formatting")
	public static TestFrameworkPrototype.TestOutput testCurrencyRecoining(final TestFramework t) {
		createCurrency(t);
		Currency currency=getCurrency(t);
		Pool pool=currency.getPool(t.primaryHUD.state);
		pool.addSystem(t.primaryHUD.state,t.primaryHUD.character,124,"Test addition");
		pool.addSystem(t.primaryHUD.state,t.primaryHUD.character,-1,"Test subtraction");
		int sum=pool.sum(t.primaryHUD.state);
		currency.setBaseCoinNames(t.primaryHUD.state, "b","base" );
		currency.removeCoin(t.primaryHUD.state,100);
		currency.removeCoin(t.primaryHUD.state,10000);
		currency.addCoin(t.primaryHUD.state,10,"t","tens");
		currency.addCoin(t.secondaryHUD.state,100,"h","hundreds");
		String shortform=currency.shortTextForm(sum);
		String longform=currency.longTextForm(sum);
		deleteCurrency(t);
		return new TestFrameworkPrototype.TestOutput("1h 2t 3b".equals(shortform) && "1 hundreds 2 tens 3 base".equals(longform), "Pool sum (123) of "+sum+" short presents as "+shortform+" long form "+longform);
	}
}
