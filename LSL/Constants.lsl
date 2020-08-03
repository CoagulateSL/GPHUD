#ifndef _INCLUDE_GPHUD_CONSTANTS
#define _INCLUDE_GPHUD_CONSTANTS
#define LINK_CAN_GO -365745998
#define LINK_GO -365745999
#define LINK_DIAGNOSTICS -365745000
#define LINK_RECEIVE -365745001
#define LINK_SET_USER_LIST -365745005
#define LINK_UPDATE_USER_LIST -365745006
#define LINK_GET_DISPENSER_CONFIG -365745007
#define LINK_DISPENSER_CONFIG -365745008
#define LINK_DISPENSE -365745011
#define LINK_LEGACY_PACKAGE -365746003 // not really legacy :P
#define LINK_STOP -365746002
#define LINK_SET_STAGE -365746004
#define LINK_SHUTDOWN -365746005 // stop everything
#define LINK_DIALOG -365746006 // UIX to dialog menu this
#define LINK_STARTUP -365746007 // stop everything
#define LINK_INSTANT_MESSAGE_SEND -365746008
#define LINK_IM_SLAVE_0 -365746010
#define LINK_IM_SLAVE_1 -365746011
#define LINK_IM_SLAVE_2 -365746012
#define LINK_IM_SLAVE_3 -365746013
string VERSION="3.10.3";
string COMPILEDATE=__DATE__;
string COMPILETIME=__TIME__;

banner_hud() {
	llOwnerSay("GPHUD HUD "+VERSION+" "+COMPILEDATE+" (C) secondlife:///app/agent/"+SLCORE_CREATOR+"/about / Iain Price, Coagulate");
}
banner_server() {
	llOwnerSay("GPHUD Region Server "+VERSION+" "+COMPILEDATE+" (C) secondlife:///app/agent/"+SLCORE_CREATOR+"/about / Iain Price, Coagulate");
}
banner_object() {
	llOwnerSay("GPHUD Object Driver "+VERSION+" "+COMPILEDATE+" (C) secondlife:///app/agent/"+SLCORE_CREATOR+"/about / Iain Price, Coagulate");
}
#endif
