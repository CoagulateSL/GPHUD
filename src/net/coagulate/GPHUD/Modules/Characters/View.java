package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Tools.SystemException;
import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Interfaces.Responses.TabularResponse;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Configuration.ConfigurationHierarchy;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/** A simple command implementation.
 * Used by all interfaces for a general purpose "status" page.
 * Rather technical, like a thing hidden in the "About" menu ;)
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class View {
    
    //TODO - FIX THE "boolean full=" section of all these functions, and decide what data is private as a result of this
    
    @Commands(context = Context.ANY,description = "Get status of this connection")
    public static Response status(State st) {
        TabularResponse t=new TabularResponse();
        t.openRow();
        t.add("Node").addNoNull((GPHUD.DEV?"DEVELOPMENT // ":"Production // ")+Interface.getNode());
        t.openRow();
        t.add("User").addNoNull(st.user);
        t.openRow();
        t.add("Avatar").addNoNull(st.avatar());
        t.openRow();
        t.add("Character").addNoNull(st.getCharacter());
        t.openRow();
        t.add("Instance").addNoNull(st.getInstanceNullable());
        t.openRow();
        t.add("Region").addNoNull(st.getRegionNullable());
        t.openRow();
        t.add("Zone").addNoNull(st.zone);        
        t.openRow();
        t.add("Source").addNoNull(st.sourcename);
        t.openRow();
        t.add("SourceOwner").addNoNull(st.sourceowner);
        t.openRow();
        t.add("SourceDev").addNoNull(st.sourcedeveloper);
        return t;
    }

    @URLs(url = "/characters/view/*")
    public static void viewCharacter(State st,SafeMap values) throws UserException, SystemException {
        //System.out.println(st.uri);
        String split[]=st.getDebasedURL().split("/");
        //System.out.println(split.length);
        if (split.length==4) { 
            String id=split[split.length-1];
            Char c=Char.get(Integer.parseInt(id));
            viewCharacter(st,values,c,false);
            return;
        }
        if (split.length==6) {
            String id=split[3];
            Char c=Char.get(Integer.parseInt(id));
            String attribute=split[4]+"."+split[5];
            st.form.add(new ConfigurationHierarchy(st, st.getKVDefinition(attribute), st.simulate(c)));
            return;
        }
        throw new SystemException("Unknown character view mode (length:"+split.length+" URI:"+st.getDebasedURL());
    }
    
    public static void viewCharacter(State st,SafeMap values,Char c,boolean brief) throws UserException, SystemException {
        boolean full=false;
        State simulated=st.simulate(c);
        String tz=st.avatar().getTimeZone();
        if (st.getCharacterNullable()==c) { full=true; }
        if (st.hasPermission("Characters.ViewAll")) { full=true; }
        Form f=st.form;
        f.add(new TextSubHeader(c.getName()));
        Table kvtable=new Table(); f.add(kvtable);
        kvtable.openRow().add("Owning Avatar").add(c.getOwner());
        String lastplayed=fromUnixTime(c.getLastPlayed(),tz);
        kvtable.openRow().add("Last Played").add(lastplayed).add(tz);
        kvtable.openRow().add("Connected");
        if (c.getURL()==null || c.getURL().isEmpty()) { kvtable.add("No"); } else { kvtable.add("Yes"); }
        kvtable.openRow().add(new Cell("<i>Assuming "+simulated.toString()+"</i>",5));
        kvtable.openRow().add(new Cell(new TextSubHeader("Attributes"),5));
        for (Attribute a:st.getAttributes()) {
            kvtable.openRow();
            kvtable.add(a.getName());
            //System.out.println("About to print attribute "+a.getName());
            kvtable.add(a.getCharacterValue(simulated));
            kvtable.add(a.getCharacterValueDescription(simulated));
        }
        if (brief) { return; }
        f.add(new TextSubHeader("KV Configuration"));
        GenericConfiguration.page(st, values, c,simulated);
        f.add(new TextSubHeader("Audit Trail"));
        f.add(Audit.formatAudit(Audit.getAudit(st.getInstance(),null,null,c),st.avatar().getTimeZone()));
    }
    
    @Commands(context = Context.CHARACTER,description = "Show yourself privately your own character sheet")
    public static Response view(State st)
    {
        return new OKResponse(st.getKV("instance.ViewSelfTemplate").value());
    }
    @Commands(context = Context.CHARACTER,description = "Publicly display your own character sheet")
    public static Response show(State st)
    {
        return new SayResponse(st.getKV("instance.ShowSelfTemplate").value());
    }
    @Commands(context = Context.CHARACTER,description = "Look at another's character sheet")
    public static Response look(State st,
            @Arguments(type = Argument.ArgumentType.CHARACTER_NEAR,description = "Character to inspect")
                Char character)
    {
        character.validate(st);
        State target=new State();
        target.setInstance(st.getInstance());
        target.setRegion(st.getRegion());
        target.setCharacter(character);
        return new OKResponse(target.getKV("instance.ViewOtherTemplate").value());
    }
    

}
