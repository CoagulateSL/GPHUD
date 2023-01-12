package net.coagulate.GPHUD.Data;

/**
 * series of tests to affirm cache operation (and its various operation modes)
 */
public class CacheTests {
	/*
	@TestFramework.Test(name="Test cache write through to DB")
	public static TestFrameworkPrototype.TestOutput testWriteThroughCacheToDB(final TestFramework t) {
		final Char target=getChar(t);
		target.setLastActive(999);
		return new TestFrameworkPrototype.TestOutput(getDBLastActive(t)==999,
		                                             "Verify written last active 999 matches DB get ("+
		                                             getDBLastActive(t)+")");
	}
	
	private static Char getChar(final TestFramework t) {
		return Char.get(t.primaryHUD.charKey);
	}
	
	private static int getDBLastActive(final TestFramework t) {
		return GPHUD.getDB()
		            .dqOne("select lastactive from characters where characterid=?",getChar(t).getId())
		            .getInt("lastactive");
	}
	
	@TestFramework.Test(name="Test cache write through to cache")
	public static TestFrameworkPrototype.TestOutput testWriteThroughCacheToCache(final TestFramework t) {
		final Char target=getChar(t);
		target.setLastActive(998);
		return new TestFrameworkPrototype.TestOutput(target.getLastPlayed()==998,
		                                             "Verify written last active 998 matches cached get ("+
		                                             target.getLastPlayed()+")");
	}
	
	@TestFramework.Test(name="Test Cache blindness to DB update")
	public static TestFrameworkPrototype.TestOutput testCacheBlindness(final TestFramework t) {
		final Char target=getChar(t);
		target.setLastActive(997);
		if (target.getLastPlayed()!=997) {
			throw new SystemConsistencyException("Applied last active not reflected in cache");
		}
		if (getDBLastActive(t)!=997) {
			throw new SystemConsistencyException("Applied last active not reflected in DB");
		}
		// fudge the DB
		setDBLastActive(t,996);
		return new TestFrameworkPrototype.TestOutput(target.getLastPlayed()==997,
		                                             "Caching is blind to database update, wrote 996, cached "+
		                                             target.getLastPlayed()+", db "+getDBLastActive(t));
	}
	
	private static void setDBLastActive(final TestFramework t,final int newLastActive) {
		GPHUD.getDB().d("update characters set lastactive=? where characterid=?",newLastActive,getChar(t).getId());
	}
	
	@TestFramework.Test(name="Test Cache disablement")
	public static TestFrameworkPrototype.TestOutput testCacheDisable(final TestFramework t) {
		Char target=getChar(t);
		target.setLastActive(995);
		if (target.getLastPlayed()!=995) {
			throw new SystemConsistencyException("Applied last active not reflected in cache");
		}
		if (getDBLastActive(t)!=995) {
			throw new SystemConsistencyException("Applied last active not reflected in DB");
		}
		// fudge the DB
		RowCachingTableRow.disableCache();
		setDBLastActive(t,994);
		waitASec(); // might need tuning if you change the cache minimum refresh period
		target=getChar(t);
		final TestFrameworkPrototype.TestOutput output=
				new TestFrameworkPrototype.TestOutput(target.getLastPlayed()==994,
				                                      "Cache disable causes fast refresh, wrote 994, cached "+
				                                      target.getLastPlayed()+", db "+getDBLastActive(t));
		RowCachingTableRow.enableCache();
		return output;
	}
	
	private static void waitASec() {
		try {
			Thread.sleep(1000L);
		} catch (final InterruptedException ignore) {
		}
	}
	
	@TestFramework.Test(name="Test Cache flush")
	public static TestFrameworkPrototype.TestOutput testCacheFlush(final TestFramework t) {
		Char target=getChar(t);
		target.setLastActive(993);
		if (target.getLastPlayed()!=993) {
			throw new SystemConsistencyException("Applied last active not reflected in cache");
		}
		if (getDBLastActive(t)!=993) {
			throw new SystemConsistencyException("Applied last active not reflected in DB");
		}
		// fudge the DB
		setDBLastActive(t,992);
		// did you know, enabling the cache purges them?
		RowCachingTableRow.enableCache();
		target=getChar(t);
		return new TestFrameworkPrototype.TestOutput(target.getLastPlayed()==992,
		                                             "Cache enable purges cache, wrote 992, cached "+
		                                             target.getLastPlayed()+", db "+getDBLastActive(t));
	}
	*/
}