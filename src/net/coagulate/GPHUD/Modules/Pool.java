package net.coagulate.GPHUD.Modules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Wraps a Pool.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Pool extends NameComparable {
    

    /** Defines a pool used by a character.
     * 
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.PACKAGE)
    @Repeatable(Poolss.class)
    public @interface Pools {
        String name();
        String description();
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.PACKAGE)
    public @interface Poolss {
        Pools[] value();
    }
    
    
    public abstract boolean isGenerated();
    public abstract String description();
    public abstract String fullName();
    
}
