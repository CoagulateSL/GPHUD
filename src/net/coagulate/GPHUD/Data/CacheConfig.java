package net.coagulate.GPHUD.Data;

public final class CacheConfig {

    private static final int ONE_MINUTE=60;
    private static final int FIVE_MINUTES=ONE_MINUTE*5;
    private static final int FIFTEEN_MINUTES=ONE_MINUTE*15;
    private static final int ONE_HOUR=ONE_MINUTE*60;

    public static final int OPERATIONAL_CONFIG = FIVE_MINUTES;
    public static final int PERMANENT_CONFIG = ONE_HOUR; // cachable forever, but frees up memory.
    // Not all actually permanent but does imply that caching works properly at least.
    public static final int MUTABLE = ONE_MINUTE;
    public static final int MINIMAL = 1; // for things that update, but we might want to cache for the remainder of this request

}
