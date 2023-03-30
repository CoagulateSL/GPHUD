package net.coagulate.GPHUD.Data;

public final class CacheConfig {
	
	private static final int ONE_SECOND        =1;
	public static final  int MINIMAL           =ONE_SECOND;
	private static final int FIVE_SECONDS      =5;
	public static final  int SHORT             =FIVE_SECONDS;
	private static final int ONE_MINUTE        =60;
	// Not all actually permanent but does imply that caching works properly at least.
	public static final  int MUTABLE           =ONE_MINUTE;
	private static final int FIVE_MINUTES      =ONE_MINUTE*5;
	public static final  int OPERATIONAL_CONFIG=FIVE_MINUTES;
	private static final int FIFTEEN_MINUTES   =ONE_MINUTE*15;
	private static final int ONE_HOUR          =ONE_MINUTE*60;
	public static final  int DURABLE_CONFIG    =ONE_HOUR;
	public static final  int PERMANENT_CONFIG  =ONE_HOUR*24*7; // cachable forever, but frees up memory.
}
