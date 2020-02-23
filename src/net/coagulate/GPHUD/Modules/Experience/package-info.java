
@ModuleDefinition(description="Enables experience/levels support", implementation="net.coagulate.GPHUD.Modules.Experience.ExperienceModule") 
@Pools(name="VisitXP",description="Experience earned through time spent on "+"sim")
@Permissions(name="ConfigureVisitXP", description="Allowed to reconfigure the visitation XP settings", power=Permission.POWER.LOW) 
@Permissions(name="ConfigureLevels", description="Allowed to reconfigure the leveling curve", power=Permission.POWER.MEDIUM) 
@KVS(name="VisitXPPoints", defaultvalue="1", editpermission="Experience.ConfigureVisitXP", type=KVTYPE.INTEGER, scope=KVSCOPE.INSTANCE, description="How many Visit XP are awarded per 'time unit' spent on sim", template=false) 
@KVS(name="VisitXPDuration", defaultvalue="60", editpermission="Experience.ConfigureVisitXP", type=KVTYPE.INTEGER, scope=KVSCOPE.INSTANCE, description="How long must be spent on the sim in minutes, total, to be awarded the Visit XP", template=false)
@KVS(name="VisitXPPerCycle", defaultvalue="1", editpermission="Experience.ConfigureVisitXP", scope=KVSCOPE.INSTANCE, type=KVTYPE.INTEGER, description="How much total VisitXP may be awarded in a cycle", template=false) 
@KVS(name="LevelXPStep", defaultvalue="6", editpermission="Experience.ConfigureLevels", scope=KVSCOPE.INSTANCE, type=KVTYPE.INTEGER, description="How often the XP to level increases by 1", template=false) 
@KVS(name="XPCycleDays", defaultvalue="6.75", editpermission="Experience.ConfigureXP", scope=KVSCOPE.INSTANCE, type=KVTYPE.FLOAT, description="How long the XP cycle is (traditionally 1 week), as a (possibly fractional) number of days", template=false)
@KVS(defaultvalue="You have --TOTALXP-- XP making you level --LEVEL--.  You have --ABILITYPOINTS-- ability points available to spend.", conveyas="leveltext", description="HUD message for leveling up", editpermission="Experience.ConfigureLevels", hierarchy=KV.KVHIERARCHY.DELEGATING, name="LevelText", scope=KVSCOPE.COMPLETE, template=true, type=KVTYPE.TEXT) 
@Permissions(name="ConfigureXP", description="Configure generic aspects of the XP system", power=Permission.POWER.MEDIUM) 
@Permissions(name="SetAbilityPoints", description="Ability to manage other players ability point count", power=Permission.POWER.LOW) 
@KVS(defaultvalue="0", description="Ability points available to character", editpermission="Experience.SetAbilityPoints", hierarchy=KV.KVHIERARCHY.CUMULATIVE, template=true, type=KVTYPE.INTEGER, scope=KVSCOPE.NONSPATIAL, name="AbilityPoints")

package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.Pool.Pools;

