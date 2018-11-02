package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Contains the data related to an attribute defined for an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class CharacterAttribute extends Attribute {

    public abstract String getName();
    
    public abstract ATTRIBUTETYPE getType();
    
    public abstract String getSubType();
    
    public abstract boolean usesAbilityPoints();
    
    public abstract boolean getRequired();
    
    public abstract String getDefaultValue();
    
    public abstract boolean getSelfModify();

    public abstract boolean isKV();

    public abstract KV.KVTYPE getKVType();
    
    public abstract String getKVDefaultValue();
    
    public abstract KV.KVHIERARCHY getKVHierarchy();

    @Override
    public String getLinkTarget() {throw new SystemException("Not valid on auto generated attribute");}
    
    protected CharacterAttribute(int id)  {super(id); } //throw new SystemException("Not valid on auto generated attribute");}
    
    public Instance getInstance()  {throw new SystemException("Not valid on auto generated attribute");}

    @Override
    public String getTableName()  {throw new SystemException("Not valid on auto generated attribute");}

    @Override
    public String getIdField()  {throw new SystemException("Not valid on auto generated attribute");}

    @Override
    public String getNameField()  {throw new SystemException("Not valid on auto generated attribute");}
    @Override
    public String getKVTable() { return null; }

    @Override
    public String getKVIdField() { return null; }
    
    public void validate(State st) throws SystemException {
        validate();
    }

    /** Set the uses abilitypoints flag.
     * 
     * @param usesabilitypoints Flags new value
     */
    public void setUsesAbilityPoints(Boolean usesabilitypoints) {throw new SystemException("Not valid on auto generated attribute");}
    
    /** Sets the required flag.
     * 
     * @param required New required flag state.
     */
    public void setRequired(Boolean required) {throw new UserException("Not valid on auto generated attribute");}

    /** Set the self modify flag.
     * 
     * @param selfmodify Character can self modify the attribute
     */
    public void setSelfModify(Boolean selfmodify) {throw new UserException("Not valid on auto generated attribute");}

    /** Set the default value for this attribute.
     * 
     * @param defaultvalue New default value
     */
    public void setDefaultValue(String defaultvalue) {throw new UserException("Not valid on auto generated attribute");}

    /** Deletes this attribute, and its data. */
    public void delete() {throw new SystemException("Not valid on auto generated attribute");}
    
    public boolean readOnly() {
        return true;
    }
    
    
}

