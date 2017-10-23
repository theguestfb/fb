package fb.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import fb.Strings;

public class DB {
	private static SessionFactory sessionFactory = null;
	private static Session session;
	private static Object dbLock = new Object();
	static {
		synchronized (dbLock) {
			if (sessionFactory == null) {
				Configuration configuration = new Configuration().configure();
				configuration.addAnnotatedClass(DBEpisode.class);
				configuration.addAnnotatedClass(DBRecents.class);
				configuration.addAnnotatedClass(DBLegacyId.class);
				configuration.addAnnotatedClass(DBUser.class);
				configuration.addAnnotatedClass(DBEmail.class);
				configuration.addAnnotatedClass(DBLegacyAuthor.class);

				StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
						.applySettings(configuration.getProperties());
				sessionFactory = configuration.buildSessionFactory(builder.build());
			}
			if (session == null) session = sessionFactory.openSession();
		}
	}

	static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	static Session getSession() {
		return session;
	}
	
	/**
	 * Adds an episode to the story
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return HTML success page
	 */
	public static DBEpisode addEp(String id, String link, String title, String body, DBUser author, Date date) {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBEpisode parent = session.get(DBEpisode.class, id);
			DBRecents recents = session.get(DBRecents.class, 1);
			author = session.get(DBUser.class, author.getId());
			DBEpisode child;
			if (parent == null)
				child = null;
			else {
				child = new DBEpisode();
				child.setTitle(title);
				child.setLink(link);
				child.setBody(body);
				child.setAuthor(author);
				child.setParent(parent);
				child.setDate(date);
				child.setId(id + "-" + (1 + parent.getChildren().size()));
				parent.getChildren().add(child);

				// DBRecentId
				//if (recents.getRecents() == null) recents.setRecents(new ArrayList<DBEpisode>());
				while (recents.getRecents().size() >= 25)
					recents.getRecents().remove(recents.getRecents().size() - 1);
				recents.getRecents().add(0, child);

				session.save(child);
				session.merge(parent);
				session.merge(recents);
				Strings.log(String.format("New: <%s> %s %s", author, title, child.getId()));
			}
			session.getTransaction().commit();
			return child;
		}
	}
	
	/**
	 * Adds an episode to the story
	 * @param id id of episode
	 * @param title new title of new episode
	 * @param body new body of new episode
	 * @param author new author of new episode
	 * @return false if id not found
	 */
	public static boolean modifyEp(String id, String link, String title, String body) {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBEpisode ep = session.get(DBEpisode.class, id);
			if (ep == null) {
				getSession().getTransaction().commit();
				return false;
			}

			ep.setTitle(title);
			ep.setLink(link);
			ep.setBody(body);

			session.merge(ep);
			Strings.log(String.format("Modified: <%s> %s %s", title, ep.getId(), getAuthor(ep)));

			session.getTransaction().commit();
			return true;
		}
	}
	
	
	
	/**
	 * Retrieves an episode from the db by id
	 * @param id
	 * @return
	 */
	public static DBEpisode getEp(String id) {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBEpisode ep = session.get(DBEpisode.class, id);
			session.getTransaction().commit();
			return ep;
		}
	}
	
	/**
	 * Retrieves the newId of an episode by it's oldId
	 * @param id
	 * @return
	 */
	public static String getLegacyId(String oldId) {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBLegacyId legacy = session.get(DBLegacyId.class, oldId);
			session.getTransaction().commit();
			if (legacy == null) return null;
			if (legacy.getNewId() == null) return null;
			return legacy.getNewId();
		}
	}
	
	/**
	 * Retrieves recent episodes from the db
	 * @return
	 */
	public static DBRecents getRecents() {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBRecents recents = session.get(DBRecents.class, 1);
			session.getTransaction().commit();
			return recents;
		}
	}
	
	/**
	 * Adds a new user to the database
	 * @param email
	 * @param password HASHED password (NOT PLAINTEXT)
	 * @param author
	 * @return null if email already exists, else user ID
	 */
	public static String addUser(String email, String password, String author) {
		synchronized (dbLock) {
			getSession().beginTransaction();
			if (getSession().get(DBEmail.class, email) != null) {
				getSession().getTransaction().commit();
				return null;
			}
			String id = newUserId();
			// Make sure id doesn't already exist
			while (getSession().get(DBUser.class, id) != null) id = newUserId();
			DBUser user = new DBUser();
			DBEmail dbemail = new DBEmail();
			dbemail.setEmail(email);
			user.setId(id);
			user.setLevel((byte)1);
			user.setAuthor(author);
			user.setEmail(dbemail);
			user.setPassword(password);
			dbemail.setUser(user);
			getSession().save(user);
			getSession().save(dbemail);
			getSession().getTransaction().commit();
			return id;
		}
	}
	
	/**
	 * Modifies a user in the database
	 * @param password null if unchanged, else HASHED password
	 * @param author null if unchanged
	 * @return false if user id does not exist
	 */
	public static boolean modifyUser(String id, String password, String author) {
		synchronized (dbLock) {
			getSession().beginTransaction();
			DBUser user = getSession().get(DBUser.class, id);
			if (user == null) {
				getSession().getTransaction().commit();
				return false;
			}
			if (author != null) user.setAuthor(author);
			if (password != null) user.setPassword(password);
			getSession().save(user);
			getSession().getTransaction().commit();
			return true;
		}
	}
	
	public static boolean changeUserLevel(String id, byte newLevel) {
		synchronized (dbLock) {
			getSession().beginTransaction();
			DBUser user = getSession().get(DBUser.class, id);
			if (user == null) {
				getSession().getTransaction().commit();
				return false;
			}
			user.setLevel(newLevel);
			getSession().getTransaction().commit();
			return true;
		}
	}

	/**
	 * Get a DBUser by id
	 * @param id
	 * @return null if id does not exist
	 */
	public static DBUser getUser(String id) {
		synchronized (dbLock) {
			getSession().beginTransaction();
			DBUser user = getSession().get(DBUser.class, id);
			getSession().getTransaction().commit();
			return user;
		}
	}
	
	/**
	 * Get a DBUser by email address
	 * @param email
	 * @return null if email does not exist
	 */
	public static DBUser getUserByEmail(String email) {
		synchronized (dbLock) {
			getSession().beginTransaction();
			DBEmail dbemail = getSession().get(DBEmail.class, email);
			getSession().getTransaction().commit();
			if (dbemail == null) return null;
			return dbemail.getUser();
		}
	}
	
	/**
	 * Gets the author name of an episode, whether it's a legacy episode or not
	 * @param ep
	 * @return
	 */
	public static String getAuthor(DBEpisode ep) {
		if (!ep.getAuthor().getId().equals("fictionbranches1")) synchronized (dbLock) {
			return getSession().get(DBLegacyAuthor.class, ep.getId()).getAuthor();
		} else return ep.getAuthor().getAuthor();
	}
	
	public static boolean changeAuthor(String userId, String author) {
		synchronized (dbLock) {
			session.beginTransaction();
			DBUser user = session.get(DBUser.class, userId);
			if (user == null) return false;
			user.setAuthor(author);
			session.merge(user);
			session.getTransaction().commit();
			return true;
		}
	}
	
	public static boolean changePassword(String userId, String newpass) {
		synchronized (dbLock) {
			session.beginTransaction();
			DBUser user = session.get(DBUser.class, userId);
			if (user == null) return false;
			user.setPassword(newpass);
			session.merge(user);
			session.getTransaction().commit();
			return true;
		}
	}
	
	public static boolean changeEmail(String userId, String email) {
		synchronized (dbLock) {
			session.beginTransaction();
			DBUser user = session.get(DBUser.class, userId);
			if (user == null) return false;
			DBEmail oldEmail = user.getEmail();
			if (oldEmail == null) return false;
			DBEmail newEmail = new DBEmail();
			newEmail.setUser(user);
			newEmail.setEmail(email);
			user.setEmail(newEmail);
			
			session.delete(oldEmail);
			session.save(newEmail);
			session.merge(user);
			session.getTransaction().commit();
			return true;
		}
	}
	
	/**
	 * Sets an account as an admin
	 * @param userId
	 * @return true if user exists
	 */
	public static boolean setAdmin(String userId) {
		synchronized (dbLock) {
			session.beginTransaction();
			DBUser user = session.get(DBUser.class, userId);
			if (user == null)
				return false;
			user.setLevel((byte) 100);
			session.merge(user);
			session.getTransaction().commit();
			return true;
		}
	}
	
	/**
	 * Sets an account as a mod
	 * @param userId
	 * @return true if user exists
	 */
	public static boolean setMod(String userId) {
		synchronized (dbLock) {
			session.beginTransaction();
			DBUser user = session.get(DBUser.class, userId);
			if (user == null)
				return false;
			user.setLevel((byte) 10);
			session.merge(user);
			session.getTransaction().commit();
			return true;
		}
	}
	
	/**
	 * Sets an account as a normal user (not mod or admin)
	 * @param userId
	 * @return true if user exists
	 */
	public static boolean setNormalUser(String userId) {
		synchronized (dbLock) {
			session.beginTransaction();
			DBUser user = session.get(DBUser.class, userId);
			if (user == null)
				return false;
			user.setLevel((byte) 1);
			session.merge(user);
			session.getTransaction().commit();
			return true;
		}
	}
	
	private static ArrayList<Character> userIdChars = new ArrayList<>();
	public static Random r = new Random();
	static {
		for (char c='a'; c<='z'; ++c) userIdChars.add(c);
		for (char c='0'; c<='9'; ++c) userIdChars.add(c);
	}
	private static String newUserId() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<16; ++i) sb.append(userIdChars.get(r.nextInt(userIdChars.size())));
		return sb.toString();
	}
}