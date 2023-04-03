package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.Tests.TestFramework;

public abstract class TestTools {
	
	protected static void waitASec(final int secs) {
		for (int i=0;i<secs;i++) {
			waitASec();
		}
	}
	
	protected static void waitASec() {
		try {
			Thread.sleep(1000L);
		} catch (final InterruptedException ignore) {
		}
	}
	
	protected static Char getChar(final TestFramework t) {
		return Char.get(t.primaryHUD.charKey);
	}
	
}
