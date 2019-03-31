package net.coagulate.GPHUD.Modules.Configuration.CookBooks;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 *
 * @author Iain Price
 */
public class UserTitlerColor extends CookBook {
    @URL.URLs(url = "/configuration/cookbooks/user-titler-color")
    public static void createForm(State st,SafeMap values) throws UserException, SystemException {
        Form f=st.form;
        f.add(new TextHeader("User Configurable Titler Color Cookbook"));
        boolean act=false;
        f.add(new Paragraph("This cookbook will enable the user to set their own titler color, it will perform the following steps:"));
        Table t=new Table(); f.add(t);
        run(st,t,false);
        if (values.containsKey("ACTIVATE COOKBOOK") && (st.isInstanceOwner()||st.isSuperUser())) {
            f.add(""); f.add(new TextSubHeader("EXECUTING COOKBOOK"));
            Table runt=new Table(); f.add(runt);
            run(st,runt,true);
        } else {
            confirmButton(st,f);
        }
    }

    private static void run(State st,Table t,boolean act) {
        t.add(new HeaderRow().add("Action").add("Verification").add("Description"));
        charAttribute(st,act,t,"TitlerColor","true","COLOR","","FALSE","FALSE","");
        setKV(st,act,t,st.getInstance(),"GPHUDClient.TitlerColor","--TITLERCOLOR--");
        setKV(st,act,t,st.getInstance(),"Characters.TitlerColor","<1,1,1>");
        JSONObject mappings=new JSONObject();
        mappings.put("attribute","TitlerColor");
        mappings.put("value-desc", "Please enter titler color in SL format i.e. <R,G,B> with values between 0.0 and 1.0");
        createAlias(st,act,t,"SetTitlerColor","characters.set",mappings);
        menu(st,act,t,"Titler Color","Alias.SetTitlerColor");
    }

}
