integer LINK_CAN_GO=-365745998;
integer LINK_GO=-365745999;
integer LINK_DIAGNOSTICS=-365745000;
integer LINK_RECEIVE=-365745001;
integer LINK_SEND=-365745002;
integer LINK_ENABLE_CALLBACK=-365745003;
integer LINK_CALLBACK_ENABLED=-365745004;
integer LINK_SET_USER_LIST=-365745005;
integer LINK_UPDATE_USER_LIST=-365745006;
integer LINK_GET_DISPENSER_CONFIG=-365745007;
integer LINK_DISPENSER_CONFIG=-365745008;
integer LINK_SET_ZONING=-365745009;
integer LINK_ZONE_TRANSITION=-365745010;
integer LINK_DISPENSE=-365745011;
string VERSION="3.4.2";
string COMPILEDATE=__DATE__;
string COMPILETIME=__TIME__;
integer LINK_LEGACY_SET=-365746000;
integer LINK_LEGACY_FIRE=-365746001;
integer LINK_LEGACY_RUN=-365746002;
integer LINK_LEGACY_PACKAGE=-365746003; // not really legacy :P
integer LINK_STOP=-365746003;
string json="";
key IAIN_MALTZ="8dc52677-bea8-4fc3-b69b-21c5e2224306";
string jsonget(string attribute)
{
	string ret=llJsonGetValue(json,[attribute]);
	if (ret=="" || ret==JSON_INVALID || ret==JSON_NULL || ret==NULL_KEY) { return ""; }
	return ret;
}

banner() {
	llOwnerSay(COMPILEDATE+" "+COMPILETIME+"\n \nGPHUD Version: "+VERSION+"\n(C) secondlife:///app/agent/8dc52677-bea8-4fc3-b69b-21c5e2224306/about / Iain Price, Coagulate\n");
}