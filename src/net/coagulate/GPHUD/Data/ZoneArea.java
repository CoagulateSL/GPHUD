package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** A 'zone' - an area of a region.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ZoneArea extends TableRow {
    
    /** Factory style constructor
     * 
     * @param id the ID number we want to get
     * @return A zone representation
     */
    public static ZoneArea get(int id) {
        return (ZoneArea)factoryPut("ZoneArea",id,new ZoneArea(id));
    }


    protected ZoneArea(int id) { super(id); }

    @Override
    public String getTableName() {
        return "zoneareas";
    }

    @Override
    public String getIdField() {
        return "zoneareaid";
    }

    @Override
    public String getNameField() {
        String[] vectors = getVectors();
        return vectors[0]+" - "+vectors[1];
    }

    @Override
    public String getLinkTarget() {
           return "/configuration/zone/"+getId();
    }

    /** Convert a string vector into an int array.
     * 
     * @param s String vector, x,y,z format with optional angle brackets (SL format)
     * @return 3 part int array [x,y,z]
     */
    public static int[] parseVector(String s) {
        s=s.replaceAll("<","");
        s=s.replaceAll(">","");
        s=s.replaceAll("\\(","");
        s=s.replaceAll("\\)","");        
        String parts[]=s.split(",");
        if (parts.length!=3) { throw new UserException("Could not decompose co-ordinates properly"); }
        int pos[]=new int[3];
        try { pos[0]=(int)Float.parseFloat(parts[0]); } catch (NumberFormatException e) { throw new UserException("Error processing X number "+parts[0]+" - "+e.getMessage()); }
        try { pos[1]=(int)Float.parseFloat(parts[1]); } catch (NumberFormatException e) { throw new UserException("Error processing Y number "+parts[1]+" - "+e.getMessage()); }
        try { pos[2]=(int)Float.parseFloat(parts[2]); } catch (NumberFormatException e) { throw new UserException("Error processing Z number "+parts[2]+" - "+e.getMessage()); }
        return pos;
    }
    
    /** Set the position for this zone area.
     * 
     * @param loc1 Corner 1 as vector string
     * @param loc2 Corner 2 as vector string
     */
    public void setPos(String loc1, String loc2) {
        int[] one=parseVector(loc1);
        int[] two=parseVector(loc2);
        d("update zoneareas set x1=?,y1=?,z1=?,x2=?,y2=?,z2=? where zoneareaid=?",one[0],one[1],one[2],two[0],two[1],two[2],getId());
    }
    
    /** Return the two corners.
     * The corners are sorted so v1.x is less than or equal to v2.x, and similarly for y and z, simplifying the code logic at the HUD end.
     * @return Array pair of vectors as strings representing the corners of the bounding box.
     */
    public String[] getVectors() {
        ResultsRow r=dqone(true,"select * from zoneareas where zoneareaid=?",getId());
        Integer x1=r.getInt("x1");
        Integer y1=r.getInt("y1");
        Integer z1=r.getInt("z1");
        Integer x2=r.getInt("x2");
        Integer y2=r.getInt("y2");
        Integer z2=r.getInt("z2");
        if (    x1==null || x2==null ||
                y1==null || y2==null ||
                z1==null || z2==null) { return null; }
        String vec1="<"; String vec2="<";
        if (x1<=x2) { vec1+=x1; vec2+=x2; } else { vec1+=x2; vec2+=x1; }
        vec1+=","; vec2+=",";
        if (y1<=y2) { vec1+=y1; vec2+=y2; } else { vec1+=y2; vec2+=y1; }
        vec1+=","; vec2+=",";
        if (z1<=z2) { vec1+=z1; vec2+=z2; } else { vec1+=z2; vec2+=z1; }
        vec1+=">"; vec2+=">";
        String vectors[]=new String[2];
        vectors[0]=vec1;
        vectors[1]=vec2;
        return vectors;
    }

    /** Get the region associated with this zone area.
     * 
     * @return Region
     */
    public Region getRegion() {
        Integer id=getInt("regionid");
        if (id==null) { throw new SystemException("Zone Area "+getId()+" has no associated region?"); }
        return Region.get(id);
    }

    /** Get the zone object that owns this area.
     * 
     * @return Zone
     */
    public Zone getZone() {
        return Zone.get(dqi(true,"select zoneid from zoneareas where zoneareaid=?",getId()));
    }

    /** Delete this zone area.
     * 
     */
    public void delete() {
        d("delete from zoneareas where zoneareaid=?",getId());
    }
    public String getKVTable() { return null; }
    public String getKVIdField() { return null; }
    public void flushKVCache(State st) {}

    @Override
    public void validate(State st) throws SystemException {
        if (validated) { return; }
        validate();
        if (st.getInstance()!=getZone().getInstance()) { throw new SystemException("ZoneArea / State Instance mismatch"); }
    }

    @Override
    public String getName() {
        String vectors[]=getVectors();
        return getRegion().getName()+"@"+vectors[0]+"-"+vectors[1];
    }

    @Override
    protected int getNameCacheTime() { return 0; }

    
    
}

