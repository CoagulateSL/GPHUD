package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.GPHUD.Tests.TestFramework;
import net.coagulate.SL.Data.SystemManagement;
import net.coagulate.SL.TestFrameworkPrototype;

import java.util.concurrent.ThreadLocalRandom;

/**
 * series of tests to affirm cache operation (and its various operation modes)
 */
public class TestCache extends TestTools {
	
	@TestFramework.Test(name="Test cache write through to DB")
	public static TestFrameworkPrototype.TestOutput testWriteThroughCacheToDB(final TestFramework t) {
		final Char target=getChar(t);
		target.setProtocol(999);
		return new TestFrameworkPrototype.TestOutput(getDBProtocol(t)==999,
		                                             "Verify written last active 999 matches DB get ("+getDBProtocol(t)+
		                                             ")");
	}
	

	private static int getDBProtocol(final TestFramework t) {
		return GPHUD.getDB()
		            .dqOne("select protocol from characters where characterid=?",getChar(t).getId())
		            .getInt("protocol");
	}
	
	@TestFramework.Test(name="Test cache write through to cache")
	public static TestFrameworkPrototype.TestOutput testWriteThroughCacheToCache(final TestFramework t) {
		final Char target=getChar(t);
		target.setProtocol(998);
		return new TestFrameworkPrototype.TestOutput(target.getProtocol()==998,
		                                             "Verify written last active 998 matches cached get ("+
		                                             target.getProtocol()+")");
	}
	
	@TestFramework.Test(name="Test Cache blindness to DB update")
	public static TestFrameworkPrototype.TestOutput testCacheBlindness(final TestFramework t) {
		final Char target=getChar(t);
		target.setProtocol(997);
		if (target.getProtocol()!=997) {
			throw new SystemConsistencyException("Applied last active not reflected in cache");
		}
		if (getDBProtocol(t)!=997) {
			throw new SystemConsistencyException("Applied last active not reflected in DB");
		}
		// fudge the DB
		setDBProtocol(t,996);
		return new TestFrameworkPrototype.TestOutput(target.getProtocol()==997,
		                                             "Caching is blind to database update, wrote 996, cached "+
		                                             target.getProtocol()+", db "+getDBProtocol(t));
	}
	
	private static void setDBProtocol(final TestFramework t,final int newProtocol) {
		GPHUD.getDB().d("update characters set protocol=? where characterid=?",newProtocol,getChar(t).getId());
	}
	
	@TestFramework.Test(name="Test Cache disablement")
	public static TestFrameworkPrototype.TestOutput testCacheDisable(final TestFramework t) {
		Char target=getChar(t);
		target.setProtocol(995);
		if (target.getProtocol()!=995) {
			throw new SystemConsistencyException("Applied last active not reflected in cache");
		}
		if (getDBProtocol(t)!=995) {
			throw new SystemConsistencyException("Applied last active not reflected in DB");
		}
		// fudge the DB
		SystemManagement.restrictCaches();
		setDBProtocol(t,994);
		waitASec(1); // might need tuning if you change the cache minimum refresh period
		final int intermediateProtocol=target.getProtocol();
		waitASec(5);
		target=getChar(t);
		final TestFrameworkPrototype.TestOutput output=new TestFrameworkPrototype.TestOutput(target.getProtocol()==994,
		                                                                                     "Cache disable causes fast refresh, wrote 994, cached initial "+
		                                                                                     intermediateProtocol+
		                                                                                     " became "+
		                                                                                     target.getProtocol()+
		                                                                                     ", db "+getDBProtocol(t));
		SystemManagement.unrestrictCaches();
		return output;
	}

	
	@TestFramework.Test(name="Test Cache flush")
	public static TestFrameworkPrototype.TestOutput testCacheFlush(final TestFramework t) {
		Char target=getChar(t);
		target.setProtocol(993);
		if (target.getProtocol()!=993) {
			throw new SystemConsistencyException("Applied last active not reflected in cache");
		}
		if (getDBProtocol(t)!=993) {
			throw new SystemConsistencyException("Applied last active not reflected in DB");
		}
		// fudge the DB
		setDBProtocol(t,992);
		// did you know, enabling the cache purges them?
		SystemManagement.unrestrictCaches();
		target=getChar(t);
		return new TestFrameworkPrototype.TestOutput(target.getProtocol()==992,
		                                             "Cache enable purges cache, wrote 992, cached "+
		                                             target.getLastPlayed()+", db "+getDBProtocol(t));
	}
	
	
	@TestFramework.Test(name="KV Write Test")
	public static TestFrameworkPrototype.TestOutput testKVWrite(final TestFramework t) {
		final Char target=getChar(t);
		final State state=new State(target);
		final String kvtext=state.getKV("GPHUDClient.HUDText").value();
		final String targetValue="NewValue:"+ThreadLocalRandom.current().nextInt();
		state.setKV(target,"GPHUDClient.HUDText",targetValue);
		final String cachecheck=state.getKV("GPHUDClient.HUDText").value();
		return new TestFrameworkPrototype.TestOutput(targetValue.equals(cachecheck),"KV was "+kvtext+" set to "+targetValue+" cache updated to "+cachecheck);
	}
	
	@TestFramework.Test(name="Case insensitive cache test")
	public static TestFrameworkPrototype.TestOutput testInsensitiveCache(final TestFramework t) {
		final Cache<String,String> test=Cache.getCache("test/testInsensitive",CacheConfig.MUTABLE,true);
		test.set("Test","CACHED");
		final String result=test.get("test",()->"UNCACHED");
		return new TestFrameworkPrototype.TestOutput("CACHED".equals(result),"Stored Test and retrieved test keys on insensitive cache, got "+result);
	}
	
	@TestFramework.Test(name="Case sensitive cache test")
	public static TestFrameworkPrototype.TestOutput testSensitiveCache(final TestFramework t) {
		final Cache<String,String> test=Cache.getCache("test/testSensitive",CacheConfig.MUTABLE,false);
		test.set("Test","CACHED");
		final String result=test.get("test",()->"UNCACHED");
		return new TestFrameworkPrototype.TestOutput("UNCACHED".equals(result),"Stored Test and retrieved test keys on sensitive cache, got "+result);
	}

	@TestFramework.Test(name="Null KV write test")
	public static TestFrameworkPrototype.TestOutput testNullKV(final TestFramework t) {
		final String original=t.primaryHUD.state.getKV("GPHUDClient.logo").toString();
		t.primaryHUD.character.setKV(t.primaryHUD.state,"GPHUDClient.logo","NOT-REAL");
		final String intermediate=t.primaryHUD.state.getKV("GPHUDCLIENT.LOGO").toString();
		t.primaryHUD.character.setKV(t.primaryHUD.state,"GPHUDClient.LOGO",null);
		final String last=t.primaryHUD.state.getKV("GPHUDClient.logo").toString();
		boolean okay=original.equals(last);
		if (original.equals(intermediate)) { okay=false; }
		return new TestFrameworkPrototype.TestOutput(okay,"Test KV Set then null, value went "+original+" -> "+intermediate+" -> "+last+", last and first should match");
	}
}