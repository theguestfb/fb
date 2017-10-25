package fb;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Cookie;

import org.apache.commons.validator.routines.EmailValidator;
import org.mindrot.jbcrypt.BCrypt;

import com.google.common.html.HtmlEscapers;

import fb.db.DB;
import fb.db.DBEpisode;
import fb.db.DBUser;

public class Accounts {
	private static ConcurrentHashMap<String,UserSession> active = new ConcurrentHashMap<>(); //<loginToken>, user>
	private static ConcurrentHashMap<String,PotentialUser> createQueue = new ConcurrentHashMap<>(); //<createToken, user>
	private static ConcurrentHashMap<String,EmailChange> emailChangeQueue = new ConcurrentHashMap<>(); //<changeToken, EmailChange>
	
	/*
	 * Scan the active sessions and createQueue maps for expired
	 */
	static {
		new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000 * 60 * 60);// run every hour
					} catch (InterruptedException e) {
						Strings.log("session prunning thread interrupted " + e.getMessage());
					}
					ArrayList<String> deleteTheseTokens = new ArrayList<>();
					for (String createToken : createQueue.keySet()) {
						Date now = new Date();
						Date then = createQueue.get(createToken).date;
						double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
						if (hours > 24) deleteTheseTokens.add(createToken); // expires after 24 hours
					}
					for (String createToken : deleteTheseTokens) createQueue.remove(createToken);

					deleteTheseTokens = new ArrayList<>();
					for (String loginToken : active.keySet()) {
						Date now = new Date();
						Date then = active.get(loginToken).lastActive();
						double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
						if (hours > 24*7) deleteTheseTokens.add(loginToken); // expires after 7 days
					}
					for (String loginToken : deleteTheseTokens) active.remove(loginToken);
					
					deleteTheseTokens = new ArrayList<>();
					for (String changeToken : emailChangeQueue.keySet()) {
						Date now = new Date();
						Date then = emailChangeQueue.get(changeToken).date;
						double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
						if (hours > 24) deleteTheseTokens.add(changeToken); // expires after 24 hours
					}
					for (String changeToken : deleteTheseTokens) emailChangeQueue.remove(changeToken);
				}
			}
		}.start();
	}
	
	public static class UserSession {
		public final String userID;
		private Date lastActive;
		public UserSession(String userID) {
			this.userID = userID;
			this.lastActive = new Date();
		}
		public Date lastActive() {
			return lastActive;
		}
		public void ping() {
			lastActive = new Date();
		}
	}
	
	private static class EmailChange {
		public final String userID;
		public final String newEmail;
		public final Date date;
		public EmailChange(String userID, String newEmail) {
			this.userID = userID;
			this.newEmail = newEmail;
			date = new Date();
		}
	}
	
	public static class PotentialUser {
		public final String email;
		public final String passwordHash;
		public final String author;
		public final Date date;
		public PotentialUser(String email, String passwordHash, String author) {
			this.email = email;
			this.passwordHash = passwordHash;
			this.author = author;
			this.date = new Date();
		}
	}
	
	/**
	 * Get HTML account stuff (click here to log in, or go to your user page)
	 * @param token
	 * @return HTML
	 */
	public static String getAccount(Cookie fbtoken) {
		String token;
		if (fbtoken==null) token = "asdf";
		else token = fbtoken.getValue();
		String notLoggedIn = "<a href=/fb/createaccount>Create account</a><br/><a href=/fb/login>Log in</a>";
		if (token == null) return notLoggedIn;
		if (token.length() == 0) return notLoggedIn;
		UserSession sesh = active.get(token);
		if (sesh == null) return notLoggedIn;
		
		DBUser user = DB.getUser(sesh.userID);
		
		String response = "Logged in as <a href=/fb/useraccount>" + HtmlEscapers.htmlEscaper().escape(user.getAuthor()) + "</a><br/><a href=/fb/logout>Log out</a>";
		if (user.getLevel()>=(byte)100) response +="<br/><a href=/fb/admin>Admin stuff</a>";
		
		return response;
	}
	/**
	 * 
	 * @param id user id
	 * @param fbtoken
	 * @return HTML user page for id
	 */
	public static String getUserPage(String id, Cookie fbtoken) {
		if (id.equals(DB.LEGACY_ID)) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "User UD " + id + " is not available"); 
		DBUser user = DB.getUser(id);
		if (user == null) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "User UD " + id + " does not exist");
		StringBuilder sb = new StringBuilder();
		for (DBEpisode ep : user.getEpisodes()) {
			String story = "";
			switch (ep.getId().split("-")[0]) {
			case "1":
				story = "(Forum)";
				break;
			case "2":
				story = "(You Are What You Wish)";
				break;
			case "3":
				story = "(Altered Fates)";
				break;
			case "4":
				story = "(The Future of Gaming)";
				break;
			}
			sb.append("<a href=/fb/get/" + ep.getId() + ">" + HtmlEscapers.htmlEscaper().escape(ep.getTitle()) + "</a> " + Story.outputDate.format(ep.getDate()) + " " + story + "<br/>");
		}
		
		return Strings.getFile("profilepage.html", fbtoken).replace("$AUTHOR", user.getAuthor()).replace("$EPISODES", sb.toString());
	}
	
	/**
	 * Get DBUser object from session token, or null if sesh does not exist
	 * @param token
	 * @return
	 */
	public static DBUser getUser(Cookie token) {
		if (token == null) return null;
		UserSession sesh = active.get(token.getValue());
		if (sesh == null) return null;
		return DB.getUser(sesh.userID);
	}
	
	/**
	 * Return whether fbtoken is an active login session
	 * 
	 * Also updates active session with current time
	 * 
	 * @param fbtoken token from cookie
	 * @return true if active user session, else false
	 */
	public static boolean isLoggedIn(Cookie fbtoken) {
		if (fbtoken == null) return false;
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) return false;
		sesh.ping();
		return true;
	}
	
	/**
	 * Log in using email and password
	 * @param email email address
	 * @param password plaintext password
	 * @return login token to be added as cookie
	 * @throws FBLoginException if email or password is wrong (e.getMessage() contains HTML)
	 */
	public static String login(String email, String password) throws FBLoginException {
		DBUser user = DB.getUserByEmail(email);
		if (user == null) throw new FBLoginException(Strings.getFile("loginform.html", null).replace("$EXTRA", "Incorrect email or password, or email does not exist"));
		if (!BCrypt.checkpw(password, user.getPassword())) throw new FBLoginException(Strings.getFile("loginform.html", null).replace("$EXTRA", "Incorrect email or password, or email does not exist"));
		
		String newToken = newToken(active);
		active.put(newToken, new UserSession(user.getId()));
		
		return newToken;
	}
	
	public static void logout(Cookie fbtoken) {
		if (fbtoken == null) return;
		active.remove(fbtoken.getValue());
	}
	
	public static class FBLoginException extends Exception {
		/** */
		private static final long serialVersionUID = -3144814684721361990L;
		public FBLoginException(String message) {
			super(message);
		}
	}
	
	/**
	 * Verify a new account
	 * @param createToken 
	 * @return HTML account confirmed, or plaintext error
	 */
	public static String verify(String createToken) {
		StringBuilder sb = new StringBuilder("Verifying token " + createToken + " against ");
		for (String key : createQueue.keySet()) sb.append(key + " ");
		Strings.log(sb.toString());
		if (createQueue.containsKey(createToken)) {
			PotentialUser createUser = createQueue.remove(createToken);
			if (DB.addUser(createUser.email, createUser.passwordHash, createUser.author) == null) return Strings.getFile("generic.html",null).replace("$EXTRA", "Email address associated with that confirmation token is already verified");
			return Strings.getFile("accountconfirmed.html", null);
		} else return Strings.getFile("generic.html",null).replace("$EXTRA", "Confirmation link is expired, invalid, or has already been used");
	}
	
	/**
	 * Create a new account
	 * @param email email address
	 * @param password password
	 * @param password2 confirm password
	 * @param author author name
	 * @return Success as plaintext or form with error 
	 */
	public static String create(String email, String password, String password2, String author) {
		{
			if (email == null || email.length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Email address is required");
			DBUser user = DB.getUserByEmail(email);
			if (user != null) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Email address " + email + " is already in use");
			if (!EmailValidator.getInstance().isValid(email)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Invalid email address " + email);
			if (!password.equals(password2)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Passwords do not match");
			if (password.length() < 8) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Password must be at least 8 characters long");
			if (author == null || author.trim().length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Author name is required"); 
		}
		String createToken = newToken(createQueue);
		PotentialUser createUser = new PotentialUser(email, BCrypt.hashpw(password, BCrypt.gensalt(10)), author);
		createQueue.put(createToken, createUser);
		if (!sendEmail(email, "Confirm your Fiction Branches account", 
				"Please click the following link (or copy/paste it into your browser) to verify your account: https://test.fictionbranches.net/fb/confirmaccount/" + createToken + " (This link is only good for 24 hours.)")) {
			return Strings.getFile("generic.html",null).replace("$EXTRA", "Unable to send verification email, talk to Phoenix about it");
		}
		return Strings.getFile("generic.html",null).replace("$EXTRA", "Check your email (and your spam folder) for a confirmation email from noreply@fictionbranches.net");
	}
	
	/**
	 * 
	 * @param fbtoken
	 * @param author
	 * @return null on success
	 */
	public static String changeAuthor(Cookie fbtoken, String author) {
		if (fbtoken == null) return null;
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) return Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		DBUser user = DB.getUser(sesh.userID);
		if (user == null) return "Invalid user";
		if (user.getId().equals(DB.LEGACY_ID) || user.getId().equals(DB.ROOT_ID)) return "This user account may not be modified";
		if (author.length() == 0) return Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "Author cannot be empty");
		if (author.length() > 200) return Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "Author cannot be longer than 200 character");
		DB.changeAuthor(user.getId(), author);
		return null;
	}
	
	/**
	 * 
	 * @param fbtoken
	 * @param newpass
	 * @param newpass2
	 * @param password
	 * @return null on success
	 */
	public static String changePassword(Cookie fbtoken, String newpass, String newpass2, String password) {
		if (fbtoken == null) return null;
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) return Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		DBUser user = DB.getUser(sesh.userID);
		if (user == null) return "Invalid user";
		if (user.getId().equals(DB.LEGACY_ID) || user.getId().equals(DB.ROOT_ID)) return "This user account may not be modified";
		if (newpass.length() < 8) return Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Password cannot be shorter than 8 characters");
		if (!newpass.equals(newpass2)) return Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Passwords do not match");
		if (!BCrypt.checkpw(password, user.getPassword())) return Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Incorrect current password");
		DB.changePassword(user.getId(), BCrypt.hashpw(newpass, BCrypt.gensalt(10)));
		return null;
	}
	
	public static String changeEmail(Cookie fbtoken, String email, String password) {
		if (fbtoken == null) return null;
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		DBUser user = DB.getUser(sesh.userID);
		if (user == null) return "Invalid user";
		if (user.getId().equals(DB.LEGACY_ID) || user.getId().equals(DB.ROOT_ID)) return "This user account may not be modified";
		if (!BCrypt.checkpw(password, user.getPassword())) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "Incorrect current password");
		
		if (!EmailValidator.getInstance().isValid(email)) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "Invalid email address " + email);
		
		if (DB.getUserByEmail(email) != null) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", email + " is already in use by another account");
		
		String changeToken = newToken(emailChangeQueue);
		EmailChange emailChange = new EmailChange(user.getId(), email);
		emailChangeQueue.put(changeToken, emailChange);
		if (!sendEmail(email, "Confirm your new Fiction Branches account email", 
				"Please click the following link (or copy/paste it into your browser) to verify your new email address: https://test.fictionbranches.net/fb/confirmemailchange/" + changeToken + " (This link is only good for 24 hours.)\nAfter taking this action, you will have to use your new email address to log in.")) {
			return "Unable to send verification email, talk to Phoenix about it";
		}
		return Strings.getFile("generic.html",null).replace("$EXTRA", "Check your email (and your spam folder) for a confirmation email from noreply@fictionbranches.net");
	}
	
	/**
	 * Verify a new account
	 * @param createToken 
	 * @return HTML account confirmed, or plaintext error
	 */
	public static String verifyNewEmail(String changeToken, Cookie fbtoken) {
		StringBuilder sb = new StringBuilder("Verifying token " + changeToken + " against ");
		for (String key : emailChangeQueue.keySet()) sb.append(key + " ");
		Strings.log(sb.toString());
		if (emailChangeQueue.containsKey(changeToken)) {
			EmailChange changeEmail = emailChangeQueue.remove(changeToken);
			if (!DB.changeEmail(changeEmail.userID, changeEmail.newEmail)) return "Unable to change email address";
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "Email address successfully changed");
		} else return Strings.getFile("generic.html",fbtoken).replace("$EXTRA", "Confirmation link is expired, invalid, or has already been used");
	}
	
	/**
	 * Changes the level of a user, or errors if no permission or bad userID
	 * @param userID
	 * @param newLevel
	 * @param fbtoken
	 * @return HTML response
	 */
	public static String changeLevel(String userID, byte newLevel, Cookie fbtoken) {
		DBUser admin = Accounts.getUser(fbtoken);
		if (admin == null) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be logged in to do that");
		if (admin.getId().equals(DB.LEGACY_ID) || admin.getId().equals(DB.ROOT_ID)) return "This user account may not be modified";
		if (admin.getLevel()<100) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be an admin to do that");
		if (!DB.changeUserLevel(userID, newLevel)) return Strings.getFile("adminform.html", fbtoken).replace("$EXTRA","Invalid user ID " + userID);
		return Strings.getFile("adminform.html", fbtoken).replace("$EXTRA", userID + " is now " + ((newLevel==((byte)100))?("an admin"):((newLevel==((byte)10))?("a mod"):("a normal user"))));
	}
	
	/**
	 * Generate a new 32-char token, ensuring that the map does not contain it as a key
	 * @param map map to check for collision
	 * @return token
	 */
	private static String newToken(ConcurrentHashMap<String,?> map) {
		StringBuilder token = new StringBuilder();
		for (int i=0; i<32; ++i) token.append((char)('a'+DB.r.nextInt(26)));
		while (map.containsKey(token.toString())) {
			token = new StringBuilder();
			for (int i=0; i<32; ++i) token.append((char)('a'+DB.r.nextInt(26)));
		}
		return token.toString();
	}
	
	/**
	 * Send an email
	 * @param toAddress
	 * @param subject
	 * @param body
	 * @return whether it sent successfully or not
	 */
	private static boolean sendEmail(String toAddress, String subject, String body) {
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.zoho.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("noreply@fictionbranches.net", Strings.SMTP_PASSWORD);
			}
		};
		Session session = Session.getInstance(props, auth);
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress("noreply@fictionbranches.net", "Fiction Branches"));
			msg.setReplyTo(InternetAddress.parse("noreply@fictionbranches.net", false));
			msg.setSubject(subject, "UTF-8");
			msg.setText(body, "UTF-8");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress, false));
			Transport.send(msg);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
