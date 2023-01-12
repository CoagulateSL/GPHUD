/**
 * The System Interface package is responsible for all Second Life input outputs.
 * <p>
 * This protocol is essentially JSON over HTTP.
 */
@Classes.Change(component=Classes.COMPONENT.Protocol,
                date="2022-04-21",
                type=ChangeLogging.CHANGETYPE.Change,
                message="Re-enable purging of disconnected URLs, but only on 404/Malformed, not on all IO errors")
package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.GPHUD.Classes;
import net.coagulate.SL.ChangeLogging;