package net.coagulate.GPHUD.Modules.Notes;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.AdminNotes;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.DateTime;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import java.util.List;

public class ViewNotes {

	public static void viewNotes(State st, User targetuser, Char targetchar, boolean top3only) {
		boolean isadmin=st.hasPermission("Notes.View");
		if (!isadmin) {
			// if not an admin, can only view our OWN public notes
			if (st.getCharacter()!=targetchar) { return; } // empty return, the view page continues without this section
		}
		List<AdminNotes.AdminNote> notes = AdminNotes.get(st.getInstance(), targetuser, targetchar, isadmin, top3only);
		// if we're not an admin, and there's no notes, then there's no point displaying the section
		if (!isadmin && notes.isEmpty()) { return; }
		//
		// we either have notes, or the user is an admin, in which case the 'add note' button makes this section barely worth rendering :P
		//
		Form f=st.form;
		f.add(new TextSubHeader("Administrator Notes (Last three only)"));
		if (!notes.isEmpty()) {
			f.add(formatNotes(notes,st.getAvatar().getTimeZone()));
			Table buttons=new Table();
			if (st.hasPermission("Notes.Add")) {
				f.noForm();
				Form charnote = new Form();
				charnote.setAction("/GPHUD/Notes/AddCharacterNote");
				charnote.add(new Hidden("character",targetchar.getName()));
				charnote.add(new Hidden("okreturnurl", st.getFullURL()));
				charnote.add(new Button("Add Character Note", true));
				buttons.add(charnote);
				Form usernote = new Form();
				usernote.setAction("/GPHUD/Notes/AddAvatarNote");
				usernote.add(new Hidden("target",targetuser.getName()));
				usernote.add(new Hidden("okreturnurl", st.getFullURL()));
				usernote.add(new Button("Add Avatar Note", true));
				buttons.add(usernote);
			}
			buttons.add("<a href=\"/GPHUD/Notes/ViewChar/"+targetchar.getId()+"\">View all character notes</a>");
			buttons.add("<a href=\"/GPHUD/Notes/ViewUser/"+targetchar.getOwner().getId()+"\">View all user notes</a>");
			f.add(buttons);
		}
	}
	@URLs(url="/Notes/AddCharacterNote",requiresPermission = "Notes.Add")
	public static void addCharacterNote(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Notes.Character", values);
	}
	@URLs(url="/Notes/AddAvatarNote",requiresPermission = "Notes.Add")
	public static void addAvatarNote(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Notes.Avatar", values);
	}
	@URLs(url="/Notes/ViewChar/*")
	public static void viewChar(State st,SafeMap values) throws UserException,SystemException {
		Integer targetid=null;
		String[] parts=st.getDebasedURL().split("\\/");
		try { targetid=Integer.parseInt(parts[parts.length-1]); } catch (NumberFormatException e) {}
		if (targetid==null) { throw new UserException("Failed to extract character id from "+parts[parts.length-1]); }
		Char target=Char.get(targetid);
		if (st.getInstance()!=target.getInstance()) { throw new UserException("State instance/target mismatch"); }
		boolean admin=false;
		if (st.hasPermission("Notes.View")) { admin=true; }
		if (!admin) {
			if (st.getAvatar()!=target.getOwner()) {
				throw new UserException("You can only view your own character");
			}
		}
		Form f=st.form;
		f.add(new TextHeader((admin?"Admin ":"User ")+" view of admin notes for "+target));
		f.add(formatNotes(AdminNotes.get(st.getInstance(),target.getOwner(),target,admin,false),st.getAvatar().getTimeZone()));
	}
	@URLs(url="/Notes/ViewUser/*")
	public static void viewUser(State st,SafeMap values) throws UserException,SystemException {
		Integer targetid=null;
		String[] parts=st.getDebasedURL().split("\\/");
		try { targetid=Integer.parseInt(parts[parts.length-1]); } catch (NumberFormatException e) {}
		if (targetid==null) { throw new UserException("Failed to extract user id from "+parts[parts.length-1]); }
		User target=User.get(targetid);

		boolean admin=false;
		if (st.hasPermission("Notes.View")) { admin=true; }
		if (!admin) {
			if (st.getAvatar()!=target) {
				throw new UserException("You can only view your own character");
			}
		}
		Form f=st.form;
		f.add(new TextHeader((admin?"Admin ":"User ")+" view of admin notes for "+target));
		f.add(formatNotes(AdminNotes.get(st.getInstance(),target,admin,false),st.getAvatar().getTimeZone()));
	}
	@URLs(url ="/Notes/ViewAll",requiresPermission = "Notes.View")
	public static void viewAll(State st, SafeMap values)  throws UserException, SystemException {
		Form f=st.form;
		List<AdminNotes.AdminNote> notes = AdminNotes.get(st.getInstance());
		f.add(new TextHeader("Admin Notes Log"));
		f.add(formatNotes(notes,st.getAvatar().getTimeZone()));
	}

	static Table formatNotes(List<AdminNotes.AdminNote> notes,String timezone) {
		Table nt = new Table();
		nt.add(new HeaderRow().add("Time Date ("+timezone+")").add("Note Publicity Level").add("Admin").add("").add("Target").add("Note"));
		for (AdminNotes.AdminNote note:notes) {
			nt.openRow();
			nt.add(new Cell(DateTime.fromUnixTime(note.tds,timezone)).align("center"));
			nt.add(new Cell(note.adminonly?"Admin Only":"Shared").align("center"));
			nt.add(note.admin.getGPHUDLink());
			nt.add("&rarr;");
			nt.add(note.targetuser.getGPHUDLink()+
					(note.targetchar==null?"":" / "+note.targetchar.asHtml(null, true)));
			nt.add(note.note);
		}
		return nt;
	}
}