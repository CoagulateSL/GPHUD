default {
	state_entry() {
		integer i=1;
		for (i=1;i<=37;i++) {
			string out="| llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_LINK_TARGET,"+((string)i)+",";
			list data=llGetLinkPrimitiveParams(i,[PRIM_NAME,PRIM_NAME,PRIM_DESC,PRIM_POS_LOCAL,PRIM_SIZE,PRIM_TEXTURE,0,PRIM_TEXTURE,1,PRIM_TEXTURE,2,PRIM_TEXTURE,3,PRIM_TEXTURE,4,PRIM_TEXTURE,5]);
			out+="PRIM_NAME,\""+llList2String(data,0)+"\",";
			out+="PRIM_DESC,\""+llList2String(data,2)+"\",";
			if (i!=1) { out+="PRIM_POS_LOCAL,"+((string)llList2Vector(data,3))+","; }
			out+="PRIM_SIZE,"+llList2String(data,4)+",";
			out+="PRIM_TEXTURE,0,\""+llList2String(data,5)+"\","+((string)llList2Vector(data,6))+","+((string)llList2Vector(data,7))+","+((string)llList2Float(data,8))+",";
			out+="PRIM_TEXTURE,1,\""+llList2String(data,9)+"\","+((string)llList2Vector(data,10))+","+((string)llList2Vector(data,11))+","+((string)llList2Float(data,12))+",";
			out+="PRIM_TEXTURE,2,\""+llList2String(data,13)+"\","+((string)llList2Vector(data,14))+","+((string)llList2Vector(data,15))+","+((string)llList2Float(data,16))+",";
			out+="PRIM_TEXTURE,3,\""+llList2String(data,17)+"\","+((string)llList2Vector(data,18))+","+((string)llList2Vector(data,19))+","+((string)llList2Float(data,20))+",";
			out+="PRIM_TEXTURE,4,\""+llList2String(data,21)+"\","+((string)llList2Vector(data,22))+","+((string)llList2Vector(data,23))+","+((string)llList2Float(data,24))+",";
			out+="PRIM_TEXTURE,5,\""+llList2String(data,25)+"\","+((string)llList2Vector(data,26))+","+((string)llList2Vector(data,27))+","+((string)llList2Float(data,28))+"]);";
			llOwnerSay(out);
		}
	}
}