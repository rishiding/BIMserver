package org.bimserver.database.actions;

import org.bimserver.database.BimDatabaseException;
import org.bimserver.database.BimDatabaseSession;
import org.bimserver.database.BimDeadlockException;
import org.bimserver.database.CommitSet;
import org.bimserver.database.Database;
import org.bimserver.database.store.User;
import org.bimserver.database.store.UserType;
import org.bimserver.database.store.log.AccessMethod;
import org.bimserver.shared.UserException;
import org.bimserver.utils.Hashers;

public class ChangePasswordDatabaseAction extends BimDatabaseAction<Boolean> {

	private final String oldPassword;
	private final String newPassword;
	private final long uoid;
	private final long actingUoid;

	public ChangePasswordDatabaseAction(AccessMethod accessMethod, long uoid, String oldPassword, String newPassword, long actingUoid) {
		super(accessMethod);
		this.uoid = uoid;
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
		this.actingUoid = actingUoid;
	}

	@Override
	public Boolean execute(BimDatabaseSession bimDatabaseSession) throws UserException, BimDeadlockException, BimDatabaseException {
		if (uoid == actingUoid) {
			User user = bimDatabaseSession.getUserByUoid(uoid);
			if (user.getUserType() == UserType.ANONYMOUS) {
				throw new UserException("Password of anonymous user cannot be changed");
			}
			return changePassword(bimDatabaseSession, false);
		} else {
			User actingUser = bimDatabaseSession.getUserByUoid(actingUoid);
			if (actingUser.getUserType() == UserType.ADMIN) {
				return changePassword(bimDatabaseSession, true);
			} else {
				throw new UserException("Insufficient rights to change the password of this user");
			}
		}
	}

	private boolean changePassword(BimDatabaseSession bimDatabaseSession, boolean skipCheck) throws BimDeadlockException, BimDatabaseException, UserException {
		User user = bimDatabaseSession.getUserByUoid(uoid);
		if (skipCheck || Hashers.getSha256Hash(oldPassword).equals(user.getPassword())) {
			user.setPassword(Hashers.getSha256Hash(newPassword));
			bimDatabaseSession.store(user, new CommitSet(Database.STORE_PROJECT_ID, -1));
			return true;
		} else {
			throw new UserException("Old password does not match user's password");
		}
	}
}