package net.coagulate.GPHUD;

import java.util.TreeMap;

/**  A map that never returns nulls.
 *
 * @author iain
 */
public class SafeMap extends TreeMap<String,String>{
    
    private static String nonull(String s) { if (s==null) { return ""; } return s; }
    public boolean submit() { return (get("Submit").equals("Submit")); }
    public String get(String key) { return nonull(super.get(key)); }

    public void debugDump() {
        for (String k:keySet()) {
            System.out.println("DEBUG DUMP SAFEMAP: "+k+"="+get(k));
        }
    }
}
