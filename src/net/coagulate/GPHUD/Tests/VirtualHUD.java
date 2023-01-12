package net.coagulate.GPHUD.Tests;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
public class VirtualHUD extends JSONDriver {
	private final VirtualRegionServer       server;
	public int                       charKey  =-1;
	public Char                      character=null;
	public net.coagulate.GPHUD.State state    =null;
	
	public VirtualHUD(final String name,
	                  final TestFramework caller,
	                  final User owner,
	                  final VirtualRegionServer region) throws IOException {
		super(name,caller,owner);
		server=region;
	}
	
	@Override
	protected void addItemSpecificHeaders(final Map<String,String> headers) {
		headers.put("X-SecondLife-Object-Key",objectKey.toString());
		headers.put("X-SecondLife-Region",server.regionName);
	}
	
	@Override
	protected boolean inputScraper(final JSONObject json) {
		if (json.has("logincomplete")) {
			charKey=json.getInt("logincomplete");
			character=Char.get(charKey);
			state=new net.coagulate.GPHUD.State(master.instance,server.region,null,character);
		}
		return true;
	}
	public void connect() throws IOException {
		final JSONObject json=new JSONObject();
		json.put("version",Region.getLatestVersionString());
		json.put("versiondate","Jan 01 2010");
		json.put("versiontime","00:00:00");
		json.put("characterid","0");
		execute("GPHUDClient.Connect",json);
	}
}
