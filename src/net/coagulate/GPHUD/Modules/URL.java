package net.coagulate.GPHUD.Modules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/** Wraps a URL.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class URL {

   
    /** Defines an exposed command.
     * That is, something the user can call through web, SL or other user interfaces.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.METHOD)
    public @interface URLs {
        String url();
        String requiresPermission() default "";
    }
    
    
    public abstract boolean isGenerated();
    
    public abstract String url();
    public abstract String requiresPermission();
    public abstract String getFullName();
    public abstract String getName();
    
    public abstract String getMethodName(); 
    public abstract void run(State st, SafeMap values);
}
