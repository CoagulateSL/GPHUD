package net.coagulate.GPHUD.Data;

import java.util.ArrayList;
import java.util.List;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NullInteger;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.Interfaces.Outputs.ToolTip;
import net.coagulate.GPHUD.State;

/** Non instantiable class with static methods for auditing things.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Audit {
    
    public static Results getAudit(Instance instance,User user,Avatar avatar,Char character) {
        List<Object> parameters=new ArrayList<>();
        String sql="select * from audit where 1=1 ";
        if (instance!=null) {
            sql+="and instanceid=? ";
            parameters.add(instance.getId());
        }
        if (user!=null) {
            sql+="and (sourceuserid=? or destuserid=?) ";
            parameters.add(user.getId());
            parameters.add(user.getId());
        }
        if (avatar!=null) {
            sql+="and (sourceavatarid=? or destavatarid=?) ";
            parameters.add(avatar.getId());
            parameters.add(avatar.getId());
        }
        if (character!=null) {
            sql+="and (sourcecharacterid=? or destcharacterid=?) ";
            parameters.add(character.getId());
            parameters.add(character.getId());
        }        
        sql+=" order by timedate desc limit 0,500";
        Object[] objectarray=new Object[0];
        return GPHUD.getDB().dq(sql,parameters.toArray(objectarray));
    }
    public static enum OPERATOR {USER,AVATAR,CHARACTER};
    public static void audit(State st,OPERATOR op,User targetuser,Avatar targetavatar,Char targetcharacter,String changetype,String changeditem,String oldvalue,String newvalue,String note) {
        audit(true,st,op,targetuser,targetavatar,targetcharacter,changetype,changeditem,oldvalue,newvalue,note);
    }
    
    public static void audit(boolean log,State st,OPERATOR op,User targetuser,Avatar targetavatar,Char targetcharacter,String changetype,String changeditem,String oldvalue,String newvalue,String note) {
        User user=st.user;
        Avatar avatar=st.avatar();
        Char character=st.getCharacterNullable();
        if (op==OPERATOR.USER) {st.setAvatar(null); character=null; }
        if (op==OPERATOR.AVATAR) { user=null; character=null; }
        if (op==OPERATOR.CHARACTER) { user=null; }
        Instance stinstance=st.getInstanceNullable();
        if (log) {
            String instance="NoInstance";
            if (stinstance!=null) { instance=stinstance.getName(); }
            String actor="";
            if (user!=null) { actor+="U:"+user.getName(); }
            if (avatar!=null) { if (!actor.isEmpty()) { actor+=" "; } actor+="A:"+avatar.getName(); }
            if (character!=null) { if (!actor.isEmpty()) { actor+=" "; } actor+="C:"+character.getName()+"#"+character.getId(); }
            String facility="";
            if (changetype!=null) { facility+=changetype; }
            if (changeditem!=null) { facility+="/"+changeditem; }
            String target="";
            if (targetuser!=null) { target+="U:"+targetuser.getName(); }
            if (targetavatar!=null) { if (!target.isEmpty()) { target+=" "; } target+="A:"+targetavatar.getName(); }
            if (targetcharacter!=null) { if (!target.isEmpty()) { target+=" "; } target+="C:"+targetcharacter.getName()+"#"+targetcharacter.getId(); }
            String message="Change from '";
            if (oldvalue==null) { message+="<null>"; } else { message+=oldvalue; }
            message+="' to '";
            if (newvalue==null) { message+="<null>"; } else { message+=newvalue; }
            message+="' on "+target+" : "+note;
            st.logger().info("<"+actor+"> in "+facility+" - "+message);
        }
        try { 
            GPHUD.getDB().d("insert into audit(timedate," +
                "instanceid," +
                "sourceuserid," +
                "sourceavatarid," +
                "sourcecharacterid," +
                "destuserid," +
                "destavatarid," +
                "destcharacterid," +
                "changetype," +
                "changeditem," +
                "oldvalue," +
                "newvalue," +
                "notes," +
                "sourcename," +
                "sourceowner," +
                "sourcedeveloper," +
                "sourceregion," +
                "sourcelocation) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    getUnixTime(),
                    getId(stinstance),
                    getId(user),
                    getId(avatar),
                    getId(character),
                    getId(targetuser),
                    getId(targetavatar),
                    getId(targetcharacter),
                    changetype,
                    changeditem,
                    oldvalue,
                    newvalue,
                    note,
                    st.sourcename,
                    getId(st.sourceowner),
                    getId(st.sourcedeveloper),
                    getId(st.sourceregion),
                    st.sourcelocation );
        } catch (DBException ex) {
            st.logger().log(WARNING,"Audit logging failure",ex);
        }
    }

    private static Object getId(TableRow r) {
        if (r==null) { return new NullInteger(); }
        return r.getId();
    }
  public static Table formatAudit(Results rows,String timezone)
    {
        Table table=new Table();
        table.nowrap();
        table.border(false);
        NameCache cache=new NameCache();
        net.coagulate.GPHUD.Interfaces.Outputs.Row headers=new HeaderRow();
        String tzheader=timezone;
        String[] tzparts=tzheader.split("/");
        if (tzparts.length==2) { tzheader=tzparts[1]; }
        headers.add(tzheader).add("").add("Source").add("").add("Target").add("Change").add("Old Value").add("").add("New Value").add("Notes");
        table.add(headers);
        String olddate="";
        for (ResultsRow r:rows) {
            String datetime[]=fromUnixTime(r.getString("timedate"),timezone).split(" ");
            if (!olddate.equals(datetime[0])) {
                net.coagulate.GPHUD.Interfaces.Outputs.Row t=new net.coagulate.GPHUD.Interfaces.Outputs.Row();
                t.align("center");
                table.add(t);
                t.add(new Cell("<table width=100%><tr width=100%><td width=50%><hr></td><td><span style=\"display: inline-block; white-space: nowrap;\">"+datetime[0]+"</span></td><td width=50%><hr></td></tr></table>", 99999));
                olddate=datetime[0];
            }
            net.coagulate.GPHUD.Interfaces.Outputs.Row t=new net.coagulate.GPHUD.Interfaces.Outputs.Row();
            table.add(t);
            t.add(datetime[1]);
            
            String sourcename=cleanse(r.getString("sourcename"));
            String sourceowner=formatavatar(cache,r.getInt("sourceowner"));
            String sourcedev=formatuser(cache,r.getInt("sourcedeveloper"));
            String sourceregion=formatregion(cache,r.getInt("sourceregion"));
            String sourceloc=trimlocation(cleanse(r.getString("sourcelocation")));
            
            if (!(sourcename.isEmpty() && sourceowner.isEmpty() && sourcedev.isEmpty() && sourceregion.isEmpty() && sourceloc.isEmpty())) {
                Table internal = new Table();
                internal.nowrap();
                internal.add(new Cell("Source name:").th()).add(sourcename).closeRow();
                internal.add(new Cell("Source owner:").th()).add(sourceowner).closeRow();
                internal.add(new Cell("Source developer:").th()).add(sourcedev).closeRow();
                internal.add(new Cell("Source region:").th()).add(sourceregion).closeRow();
                internal.add(new Cell("Source location:").th()).add(sourceloc).closeRow();
                t.add(new ToolTip("[Via]",internal));
            } else { t.add(""); }

            String srcav=formatavatar(cache,r.getInt("sourceavatarid"));
            String srcch=formatchar(cache,r.getInt("sourcecharacterid"));
            String dstav=formatavatar(cache,r.getInt("destavatarid"));
            String dstch=formatchar(cache,r.getInt("destcharacterid"));
            t.add(new Cell(srcav+(srcav.isEmpty() || srcch.isEmpty()?"":"/")+srcch).align("right"));
            // if we have nothing on one side
            if ((srcav.isEmpty() && srcch.isEmpty()) || (dstav.isEmpty() && dstch.isEmpty())) {
                t.add("");
            } else { 
                t.add("&rarr;");
            }
            t.add(dstav+(dstav.isEmpty() || dstch.isEmpty()?"":"/")+dstch);
            String changetype=cleanse(r.getString("changetype"));
            String changeitem=cleanse(r.getString("changeditem"));
            t.add(changetype+(changetype.isEmpty()||changeitem.isEmpty()?"":" - ")+changeitem);
            
            String oldvaluestr=cleanse(r.getString("oldvalue"));
            String newvaluestr=cleanse(r.getString("newvalue"));
            Renderable oldvalue=notate(oldvaluestr,10);
            Renderable newvalue=notate(newvaluestr,10);
            Renderable notes=new Text(cleanse(r.getString("notes")));
            t.add(new Cell(oldvalue).align("right"));
            if (oldvaluestr.isEmpty() && newvaluestr.isEmpty()) { t.add(""); } else { t.add("&rarr;"); }
            t.add(newvalue);
            t.add(notes);
            /*
            net.coagulate.GPHUD.Interfaces.Outputs.Row t2=new net.coagulate.GPHUD.Interfaces.Outputs.Row();
            t2.setbgcolor("#f0f0f0");
            t2.add(new Cell(cleanse(r.getString("oldvalue")),3));
            t2.add(new Cell(cleanse(r.getString("newvalue")),2));
            t2.add(new Cell(cleanse(r.getString("notes")),7));
            table.add(t2);
            */
        }
        if (table.rowCount()==1) { table=new Table(); table.add("No audit events"); }
        return table;
    }
    private static Renderable notate(String s,int size) {
        if (s.length()>size) {
            return new ToolTip(s.substring(0,size),s);
        }
        return new Text(s);
    }
    private static String cleanse(String s) { if (s==null) { return ""; } return s; }
    private static String formatuser(NameCache cache,Integer userid) {
        if (userid!=null) { return cache.lookup(User.get(userid)); }
        return "";
    }
    private static String formatavatar(NameCache cache,Integer avatarid) {
        if (avatarid!=null) { return cache.lookup(Avatar.get(avatarid)); }
        return "";
    }
    private static String formatchar(NameCache cache,Integer charid) {
        if (charid!=null) { return cache.lookup(Char.get(charid)); }
        return "";
    }
    private static String formatregion(NameCache cache,Integer charid) {
        if (charid!=null) { return cache.lookup(Region.get(charid)); }
        return "";
    }
    private static String trimlocation(String s) {
        String olds=s;
        s=s.replaceAll("\\(","");
        s=s.replaceAll("\\)","");
        s=s.replaceAll(" ","");
        String xyz[]=s.split(",");
        if (xyz.length!=3) { return olds; }
        try {
            float x=Float.parseFloat(xyz[0]);
            float y=Float.parseFloat(xyz[1]);
            float z=Float.parseFloat(xyz[2]);
            return ((int)x)+","+((int)y)+","+((int)z);
        }
        catch (NumberFormatException e) { return olds; }
    }
}
