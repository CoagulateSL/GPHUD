local VERSION="4.0.0";
local slcore=require("../../SLCore/SLua/SecondLifeConstants")
local COMPILEDATE = __DATE__
local COMPILETIME = __TIME__

local function banner_server() 
    ll.OwnerSay("GPHUD Region Server "..VERSION.." "..COMPILEDATE.." (C) secondlife:///app/agent/"..slcore.SLCORE_CREATOR.."/about / Iain Price, Coagulate")
end

return {
    -- not all of these link messages are used any more
    -- notably since slua has higher memory caps a lot of slaved off script content got folded into the main script
    LINK_CAN_GO=-365745998,
    LINK_GO=-365745999,
    LINK_DIAGNOSTICS=-365745000,
    LINK_RECEIVE=-365745001,
    LINK_SET_USER_LIST=-365745005,
    LINK_UPDATE_USER_LIST=-365745006,
    LINK_GET_DISPENSER_CONFIG=-365745007,
    LINK_DISPENSER_CONFIG=-365745008,
    LINK_DISPENSE=-365745011,
    LINK_DISPENSE_TITLER=-365745012,
    LINK_LEGACY_PACKAGE=-365746003, -- not really legacy :P
    LINK_STOP=-365746002,
    LINK_SET_STAGE=-365746004,
    LINK_SHUTDOWN=-365746005, -- stop everything
    LINK_DIALOG=-365746006, -- UIX to dialog menu this
    LINK_STARTUP=-365746007, -- stop everything
    LINK_INSTANT_MESSAGE_SEND=-365746008,
    banner_server = banner_server,
    VERSION = VERSION,
    COMPILEDATE = COMPILEDATE,
    COMPILETIME = COMPILETIME
}
