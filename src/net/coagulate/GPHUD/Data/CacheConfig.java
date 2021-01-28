package net.coagulate.GPHUD.Data;

public final class CacheConfig {

    private static final int ONE_MINUTE=60;
    private static final int FIVE_MINUTES=ONE_MINUTE*5;
    private static final int FIFTEEN_MINUTES=ONE_MINUTE*15;
    private static final int ONE_HOUR=ONE_MINUTE*60;

    public static final int ALIAS = FIVE_MINUTES;
    public static final int INSTANCE = ONE_HOUR; // cachable forever, but frees up memory
    public static final int NAME = ONE_MINUTE;
    public static final int KV = ONE_MINUTE;
    public static final int ATTRIBUTES = FIVE_MINUTES;
    public static final int CHAR_OWNER = ONE_HOUR;
    public static final int PROTOCOL_LEVEL = ONE_HOUR;

}
