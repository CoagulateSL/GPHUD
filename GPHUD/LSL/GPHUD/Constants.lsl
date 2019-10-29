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
#define LINK_STOP -365746003
#define LINK_SET_STAGE -365746004
string VERSION="3.7.2";
string COMPILEDATE=__DATE__;
string COMPILETIME=__TIME__;
string IAIN_MALTZ="8dc52677-bea8-4fc3-b69b-21c5e2224306";

banner_hud() {
	llOwnerSay("GPHUD HUD "+VERSION+" "+COMPILEDATE+" (C) secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about / Iain Price, Coagulate");
}
banner_server() {
	llOwnerSay("GPHUD Region Server "+VERSION+" "+COMPILEDATE+" (C) secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about / Iain Price, Coagulate");
}
#endif
