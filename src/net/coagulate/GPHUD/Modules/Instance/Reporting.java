package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.HTML.Elements.Raw;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.SL.Data.SystemManagement;
import net.coagulate.SL.HTTPPipelines.PlainTextMapper;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Reporting extends Thread {
	
	public static final int SLEEP_WHEN_NO_REPORTS_IN_MILLIS=5000;
	public static final int REPORTING_TICK_IN_MILLIS =20;
	
	@SuppressWarnings("InfiniteLoopStatement")
	public void run() {
		final Logger log=GPHUD.getLogger("Reporting");
		Thread.currentThread().setName("GPHUD Reporting Thread");
		while (true) {
			try {
				if (SystemManagement.primaryNode()) {
					try {
						final long tickstart=System.currentTimeMillis();
						Instance.reportingTick(log);
						final long sleep=REPORTING_TICK_IN_MILLIS-(System.currentTimeMillis()-tickstart);
						if (sleep>0) {
							try { //noinspection BusyWait
								Thread.sleep(REPORTING_TICK_IN_MILLIS);
							} catch (final InterruptedException ignore) {
							}
						}
					} catch (final RuntimeException e) {
						log.log(Level.SEVERE,"Reporting Tick exceptioned",e);
					}
				} else {
					try {
						//noinspection BusyWait
						Thread.sleep(SLEEP_WHEN_NO_REPORTS_IN_MILLIS);
					} catch (final InterruptedException ignore) {
					}
				}
			} catch (final RuntimeException e) {
				// dont let me exit
				log.log(Level.SEVERE,"Exception leaked to outermost Reporting handler",e);
			}
		}
	}
	
	@URL.URLs(url="/reporting", requiresPermission="Instance.Reporting")
	public static void reportingPage(@Nonnull final net.coagulate.GPHUD.State state,@Nonnull final SafeMap parameters) {
		final String tz=state.getAvatar().getTimeZone();
		final Form f=state.form();
		f.add(new TextHeader("Report Generation"));
		f.add(new Paragraph(
				"Your instance currently has "+state.getInstance().reportCredits()+" reporting credits and "+
				state.getInstance().downloadCredits()+" download credits."));
		
		if (state.getInstance().reporting()) {
			f.add(new Paragraph("Report generation is currently in progress"));
			final int starttime=state.getInstance().reportingStartTime();
			f.add(new Paragraph("Reporting started at: "+UnixTime.fromUnixTime(starttime,tz)));
			final int characters=state.getInstance().countCharacters();
			final int reportedon=state.getInstance().countReportedCharacters();
			f.add(new Paragraph("Report has generated "+reportedon+" characters out of "+characters));
			final int secondstaken=UnixTime.getUnixTime()-starttime;
			if (characters>0&&secondstaken>0) {
				final double percentdone=100.0*(reportedon/((double)characters));
				final double charspersecond=reportedon/((double)secondstaken);
				final int timeleft=(int)Math.round((characters-reportedon)*(1.0/charspersecond));
				final int eta=UnixTime.getUnixTime()+timeleft;
				f.add(new Paragraph(
						"Report is "+Math.round(percentdone)+"% done in "+UnixTime.duration(secondstaken,true)+" at "+
						Math.round(charspersecond)+" chars per second, and should complete in "+
						UnixTime.duration(timeleft,true)+" estimated to finish at "+UnixTime.fromUnixTime(eta,tz)));
			}
		} else {
			if (state.getInstance().reportCredits()>0||state.isSuperUser()) {
				f.add(new Paragraph(new Link("Generate New Report","/GPHUD/reporting/generate")));
			}
			final int reportStart=state.getInstance().reportingStartTime();
			final int reportEnd=state.getInstance().reportingEndTime();
			if (reportStart!=0&&reportEnd!=0) {
				f.add(new Paragraph("Last report started at "+UnixTime.fromUnixTime(reportStart,tz)+" and finished at "+
				                    UnixTime.fromUnixTime(reportEnd,tz)));
				if (reportEnd==reportStart) {
					f.add(new Paragraph("Reporting took less than 1 second"));
				} else {
					f.add(new Paragraph("Reporting took "+UnixTime.duration(reportEnd-reportStart)));
				}
				if (state.isSuperUser()||state.getInstance().downloadCredits()>0) {
					f.add(new Paragraph(new Link("Download this report","/GPHUD/reporting/download")));
				}
			}
		}
	}
	
	@URL.URLs(url="/reporting/generate", requiresPermission="Instance.Reporting")
	public static void webGenerateReport(@Nonnull final net.coagulate.GPHUD.State state,
	                                     @Nonnull final SafeMap parameters) {
		final Form f=state.form();
		final Response success=generateReport(state);
		if (success instanceof ErrorResponse) {
			f.add(new Paragraph(new TextError(success.asHtml(state,true))));
		} else {
			f.add(new Paragraph(new TextOK(success.asHtml(state,true))));
		}
		f.add(new Paragraph(new Link("Return to reporting","/GPHUD/reporting")));
	}
	
	@Command.Commands(description="Generates an up to date report ready for download",
	                  notes="This may take a while, intentionally",
	                  requiresPermission="Instance.Reporting",
	                  permitObject=false,
	                  permitScripting=false,
	                  context=Command.Context.AVATAR)
	public static Response generateReport(@Nonnull final net.coagulate.GPHUD.State state) {
		if (state.getInstance().reporting()) {
			return new ErrorResponse("Already generating a report!");
		}
		if (!state.isSuperUser()) {
			if (!state.getInstance().spendReportCredit()) {
				return new ErrorResponse("Sorry, you have no report generation credits left");
			}
		}
		state.getInstance().startReport();
		return new OKResponse("Report generation in progress");
	}
	
	@URL.URLs(url="/reporting/download", requiresPermission="Instance.Reporting")
	public static void webDownloadReport(@Nonnull final net.coagulate.GPHUD.State state,
	                                     @Nonnull final SafeMap parameters) {
		if (!state.isSuperUser()) {
			if (!state.getInstance().spendDownloadCredit()) {
				state.form().add(new Paragraph(new TextError("Sorry, you are out of download credits!")));
				return;
			}
		}

		Page.page().resetRoot();
		//noinspection deprecation
		Page.page().add(new Raw(state.getInstance().getReport()));
		state.suppressOutput(true);
		Page.page().template(new PlainTextMapper.PlainTextTemplate());
		Page.page()
		    .addHeader("content-disposition",
		               "attachment; filename=\""+state.getInstance().getName()+" - "+
		               UnixTime.fromUnixTime(state.getInstance().reportingEndTime(),state.getAvatar().getTimeZone())+" "+
		               state.getAvatar().getTimeZone()+".csv\"");
		Page.page().contentType(ContentType.create("text/csv"));
	}
	
}
