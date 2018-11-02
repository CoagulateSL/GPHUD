#include "CommsHeader.lsl"

default {
    on_rez(integer n) { llResetScript(); }
    state_entry() {
		if (llGetObjectDesc()=="DEV") { llOwnerSay("Booting in DEV mode"); DEV=1; } else { DEV=0; }
		if (llGetObjectDesc()=="DEV-iain") { llOwnerSay("Booting in Iain DEV mode.  Hi there :) "); DEV=2; }
		setupServers();
		while (llGetListLength(servers)>llGetListLength(servertimeout)) { servertimeout+=[0]; }
		serveractive=0;
    }
    http_response(key id,integer status,list data,string body)
    {
        integer n=llListFindList(txid,[id]);
        if (n==-1) {
            return;
        }
        if (status!=200) {
            integer serverid=llList2Integer(txserver,n);
            servertimeout=llListReplaceList(servertimeout,[llGetUnixTime()+60],serverid,serverid);
            selectServer();
            txid=llListReplaceList(txid,[NULL_KEY],n,n);
            trigger();
        }
        else
        {
			json=body;
			integer process=1;
			if (jsonget("say")!="") {
				string oldname=llGetObjectName();
				string newname=jsonget("sayas");
				if (newname!="") { llSetObjectName(newname); }
				llSay(0,jsonget("say"));
				if (newname!="") { llSetObjectName(oldname); }
			}
			if (jsonget("suspend")!="") {
				reason=jsonget("suspend");
				llOwnerSay("---\nSUSPEND:"+reason+"\n---");
			}
			if (jsonget("terminate")!="") {
				reason=jsonget("terminate");
				llOwnerSay("---\nTERMINATE:"+reason+"\n---");
			}			
			if (jsonget("shutdown")!="") {
				llOwnerSay("---\nSHUTDOWN REQUESTED\n---");
			}						
			if (jsonget("message")!="") {
                llOwnerSay(jsonget("message"));
			}
			if (jsonget("notice")!="") {
                llSay(0,jsonget("notice"));
				process=0;
            }
            if (jsonget("error")!="") {
                llOwnerSay("Server reported error with request:\n"+jsonget("error"));
				process=0;
            }
			if (process==1) {
                //llSay(0,"HTTP:"+body);
                llMessageLinked(LINK_THIS,LINK_RECEIVE,body,id);
            }
            txid=llDeleteSubList(txid,n,n);
            txmessage=llDeleteSubList(txmessage,n,n);
            txserver=llDeleteSubList(txserver,n,n);            
        }
    }
    timer() {
        llSetTimerEvent(0);
        if (serveractive<0) {
            selectServer();
            if (serveractive>=0) { trigger(); } else { llSetTimerEvent(15.0); }
        }
    }
    http_request(key id, string method, string body)
    {
		if (method == URL_REQUEST_DENIED)
		{ llOwnerSay("Error getting callback URL:" + body); }
	 	if (method == URL_REQUEST_GRANTED)
		{
			if (url!="") { llReleaseURL(url); }
			url = body;
			llMessageLinked(LINK_THIS,LINK_CALLBACK_ENABLED,"","");
		}
		if (method=="POST") {
			llMessageLinked(LINK_THIS,LINK_RECEIVE,body,NULL_KEY);
			llHTTPResponse(id,200,"");
		}
    }
    link_message(integer from,integer num,string message,key id) {
        if (num==LINK_SEND) {
            httpcommand((string)id,message); 
        }
		if (num==LINK_ENABLE_CALLBACK) {
			llRequestURL();
		}
		if (num==LINK_DIAGNOSTICS) {
		
		}
    }
}
state terminate {
	state_entry() { 
		llSetTimerEvent(1.0);
	}
	timer() {
		llShout(0,"GPHUD TERMINATED US: "+reason);
		llOwnerSay("GPHUD TERMINATED US: "+reason);
		llSetTimerEvent(300);
	}
}
state suspend {
	state_entry() { llSetTimerEvent(1.0); }
	timer() { 
		llOwnerSay("ERROR - GPHUD server asked us to suspend!!!");
		llOwnerSay("\""+reason+"\"");
		llOwnerSay("Please click me to resume the connection");
		llSetTimerEvent(300);
	}
	touch_start(integer n) { state default; }
}