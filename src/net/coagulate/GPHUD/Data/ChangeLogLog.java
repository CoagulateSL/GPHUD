package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.GPHUD;

public class ChangeLogLog {
	public static boolean empty() {
		return 0==GPHUD.getDB().dqiNotNull("select count(*) from changelogged");
	}
	
	public static void store(final String s) {
		GPHUD.getDB().d("insert into changelogged(message) values(?)",s);
	}
	
	public static boolean notLogged(final String dbindex) {
		return 0==GPHUD.getDB().dqiNotNull("select count(*) from changelogged where message=?",dbindex);
	}
}
