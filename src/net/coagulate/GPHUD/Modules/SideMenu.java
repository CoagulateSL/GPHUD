package net.coagulate.GPHUD.Modules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Wraps a side menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class SideMenu {

    /** Defines a sidemenu section for this module.
     * 
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.PACKAGE)
    public @interface SideMenus {
        String name();
        int priority();
        String url();
        String requiresPermission() default "";
    }
       
    public abstract boolean isGenerated();
    public abstract String name();
    public abstract int priority();
    public abstract String url();
    public abstract String requiresPermission();
}
