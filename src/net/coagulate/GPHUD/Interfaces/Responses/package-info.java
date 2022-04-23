/** Contains a variety of Response objects that can be returned to the HUD - various JSON packaged structures
 *
 */

@Classes.Change(date="2022-04-23",type=ChangeLogging.CHANGETYPE.Fix,component=Classes.COMPONENT.Protocol,message="Block empty messages from being sent in a JSON payload (a secondary fix for things saying 's' in blank contexts etc)")

package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Classes;
import net.coagulate.SL.ChangeLogging;