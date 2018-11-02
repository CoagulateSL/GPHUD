package net.coagulate.GPHUD.Modules.Introspection;

import static java.util.logging.Level.SEVERE;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.HTTPListener;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.UserException;

/** Controlled shutdown of a server.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ShutDown {
    
    @Commands(context = Context.USER,description = "Shut down this server (SUPERADMIN ONLY",requiresPermission = "instance.owner")
    public static Response shutdown(State st,
            @Arguments(description = "The word CONFIRM to confirm",type = ArgumentType.TEXT_ONELINE,mandatory = false)
            String confirm) {
        if (st.user==null) { throw new UserException("No user state found"); }
        if (!st.isSuperUser()) { throw new UserException("YOU ARE NOT PERMITTED TO SHUT DOWN THE ENTIRE SERVER (...)"); }
        if (confirm==null || !confirm.equals("CONFIRM")) { return new ErrorResponse("Pass CONFIRM as parameter to confirm, we are shutting down : "+GPHUD.environment()); }
        st.logger().log(SEVERE,"SHUTDOWN HAS BEEN INITIATED");
        HTTPListener.shutdown();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        st.logger().log(SEVERE,"TERMINATING EXECUTION NOW");
        System.exit(0);
        return new OKResponse("Shutdown :P"); // unreachable code, but apparently the compiler doesn't realise this :)
    }
    @URLs(url="/introspection/cleanshutdown",requiresPermission = "instance.owner")
    public static void shutdownForm(State st,SafeMap values) { Modules.simpleHtml(st, "introspection.shutdown", values); }
}
