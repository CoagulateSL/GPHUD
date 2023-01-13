package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Table;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;

public class InstanceDevelopers extends Table {
	
	/**
	 * Update accounting details for an external access
	 *
	 * @param instance  Instance
	 * @param developer Developer
	 * @param queries   Number of queries
	 * @param bytes     Byte count
	 */
	public static void accounting(@Nonnull final Instance instance,
	                              @Nonnull final User developer,
	                              final int queries,
	                              final int bytes) {
		db().d("update instancedevelopers set queries=queries+?,bytes=bytes+? where instanceid=? and developerid=?",
		       queries,
		       bytes,
		       instance.getId(),
		       developer.getId());
	}
	
	// ---------- STATICS ----------
	@Nonnull
	public static DBConnection db() {
		return GPHUD.getDB();
	}
	
	/**
	 * Remove a users authorisation
	 *
	 * @param state     State for logging
	 * @param instance  Instance
	 * @param developer Developer
	 */
	public static void deAuthorise(@Nonnull final State state,
	                               @Nonnull final Instance instance,
	                               @Nonnull final User developer) {
		if (!isDeveloper(instance,developer)) {
			return;
		}
		db().d("delete from instancedevelopers where instanceid=? and developerid=?",
		       instance.getId(),
		       developer.getId());
		Audit.audit(true,
		            state,
		            OPERATOR.AVATAR,
		            null,
		            null,
		            "Delete",
		            "External",
		            developer.getName(),
		            "",
		            "User deauthorised developer "+developer.getName());
	}
	
	/**
	 * Checks if a developer is authorised at a given instance
	 *
	 * @param instance  Instance name
	 * @param developer Developer ID
	 * @return true if the developer is authorised at that instance, false if the user is not or has no developer key
	 */
	public static boolean isDeveloper(@Nonnull final Instance instance,@Nonnull final User developer) {
		if (!developer.hasDeveloperKey()) {
			return false;
		}
		return db().dqiNotNull("select count(*) from instancedevelopers where instanceid=? and developerid=?",
		                       instance.getId(),
		                       developer.getId())>=1;
	}
	
	/**
	 * Authorise a developer
	 *
	 * @param state     State for logging
	 * @param instance  Instance
	 * @param developer Developer
	 */
	public static void authorise(@Nonnull final State state,
	                             @Nonnull final Instance instance,
	                             @Nonnull final User developer) {
		if (isDeveloper(instance,developer)) {
			return;
		}
		db().d("insert into instancedevelopers(instanceid,developerid) values(?,?)",instance.getId(),developer.getId());
		Audit.audit(true,
		            state,
		            OPERATOR.AVATAR,
		            null,
		            null,
		            "Add",
		            "External",
		            "",
		            developer.getName(),
		            "User authorised external developer access to "+developer.getName());
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public DBConnection getDatabase() {
		return GPHUD.getDB();
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "instancedevelopers";
	}
}
