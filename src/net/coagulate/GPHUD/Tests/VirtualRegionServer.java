package net.coagulate.GPHUD.Tests;

import net.coagulate.Core.Database.DBUnexpectedNullValueException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class VirtualRegionServer extends JSONDriver {
	
	public final String regionName;
	// suck the logo from the payload
	public       String logo       ="";
	public       String status     ="";
	public       String statuscolor="";
	public       Region region;
	public       int    gridXY;
	
	public VirtualRegionServer(final String name,
	                           final String regionName,
	                           final TestFramework caller,
	                           final User owner,
	                           final int gridXY) throws IOException {
		super("GPHUD Region Server "+name,caller,owner);
		this.regionName=regionName;
		this.gridXY=gridXY;
	}
	
	public void connect() throws IOException {
		final JSONObject json=new JSONObject();
		String latestVersion="3.10.02";
		try { latestVersion=Region.getLatestVersionString(); }
		catch (final DBUnexpectedNullValueException ignore) {} // might happen if there's zero active instances
		json.put("version",latestVersion);
		json.put("versiondate","Jan 01 2021");
		json.put("versiontime","00:00:00");
		execute("gphudserver.register",json);
		try { region=null; region=Region.find(regionName,false); }
		catch (final UserInputLookupFailureException ignore) {}
	}
	
	public void createInstance(final String name) throws IOException {
		final JSONObject json=new JSONObject();
		json.put("console","createinstance "+name);
		execute("console",json);
	}
	
	public void joinInstance(final String name) throws IOException {
		final JSONObject json=new JSONObject();
		json.put("console","joininstance "+name);
		execute("console",json);
	}
	
	@Override
	protected void addItemSpecificHeaders(final Map<String,String> headers) {
		headers.put("X-SecondLife-Object-Key",objectKey.toString());
		headers.put("X-SecondLife-Region",regionName+" ("+gridXY+", "+gridXY+")");
	}
	
	@Override
	protected boolean inputScraper(final JSONObject json) {
		if (json.has("setlogo")) {
			logo=json.getString("setlogo");
		}
		// might just be a status update
		if (json.keySet().size()==2&&json.has("instancestatus")&&json.has("statuscolor")) {
			status=json.getString("instancestatus");
			statuscolor=json.getString("statuscolor");
			return false;
		}
		return true;
	}
}