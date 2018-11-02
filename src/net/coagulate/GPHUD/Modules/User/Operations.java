package net.coagulate.GPHUD.Modules.User;

import net.coagulate.GPHUD.Data.Audit;
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
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Set/change a users passwords.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Operations {
    @URLs(url="/users/changepassword")
    public static void changePasswordForm(State st,SafeMap values) throws UserException, SystemException {
        Modules.simpleHtml(st, "User.ChangePassword", values);
    }
    
    @Commands(context = Context.USER,description = "Change your USER password")
    public static Response changePassword(State st,
            @Arguments(description = "Old password",type = ArgumentType.PASSWORD)
                String oldPassword,
            @Arguments(description = "New password",type = ArgumentType.PASSWORD)
                String newPassword,
            @Arguments(description = "Confirm new password",type = ArgumentType.PASSWORD)
                String confirmNewPassword) throws SystemException {
        // null guard, shouldn't be important but...
        if (oldPassword==null || newPassword==null || confirmNewPassword==null ||
                oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) { return new ErrorResponse("You may not omit any values or use blank passwords"); }
        // passwords match?
        if (!newPassword.equals(confirmNewPassword)) { return new ErrorResponse("New passwords do not match"); }
        if (!st.user.verifyPassword(oldPassword)) { return new ErrorResponse("Old password does not match"); }
        try { st.user.setPassword(newPassword); }
        catch (UserException e) { return new ErrorResponse(e.getMessage()); }
        Audit.audit(st, Audit.OPERATOR.USER, st.user, null, null, "Change", "Password", "[CENSORED]", "[CENSORED]", "User completed password change.");
        return new OKResponse("Password changed successfully.");
    }
    
    @Commands(context=Context.AVATAR,description = "Set your USER password (via authorised SL login ONLY)",permitUserWeb = false)
    public static Response setPassword(State st,
            @Arguments(description = "New password",type = ArgumentType.PASSWORD)
                String password) throws SystemException, UserException {
        if (st.sourcedeveloper!=null && st.sourcedeveloper.getId()!=1) { throw new SystemException("RESTRICTED COMMAND"); }
        if (st.user==null) { return new ErrorResponse("Not connected to any user account?  Perhaps you need to register (via User.Register).  Session did not derive a USER context."); }
        try { st.user.setPassword(password); }
        catch (UserException e) { return new ErrorResponse(e.getMessage()); }
        Audit.audit(st, Audit.OPERATOR.AVATAR, st.user, null, null, "Replace", "Password", "[CENSORED]", "[CENSORED]", "User set password.");
        return new OKResponse("Password set successfully.");
        
    }


    @Commands(context = Context.AVATAR,description = "Create a new USER account for this avatar (via authorised SL login ONLY)",permitUserWeb = false)
    public static Response register(State st,
            @Arguments(description = "User name to register",type = ArgumentType.TEXT_ONELINE)
                String username,
            @Arguments(description = "Password for new user",type=ArgumentType.PASSWORD)
                String password) throws SystemException, UserException
    {
        return new ErrorResponse("Registration is currently disabled");
    }
}
