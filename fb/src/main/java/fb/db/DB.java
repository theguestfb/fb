package fb.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.mindrot.jbcrypt.BCrypt;

import fb.json.TempUser;
import fb.objects.Episode;
import fb.objects.FlaggedEpisode;
import fb.objects.User;
import fb.util.Comparators;
import fb.util.Strings;

public class DB {
	
	public static final String ROOT_ID = "fbadministrator1";
	
	static final SessionFactory sessionFactory;
	//static final Session session;
	//private static final Connection con;
	private static Object dbLock = new Object();
	static {
		synchronized (dbLock) {
			
			Configuration configuration = new Configuration().configure();
			
			configuration.addAnnotatedClass(DBEpisode.class);
			configuration.addAnnotatedClass(DBUser.class);
			configuration.addAnnotatedClass(DBFlaggedEpisode.class);

			StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
			sessionFactory = configuration.buildSessionFactory(builder.build());
			//session = sessionFactory.openSession();
			/*try { // I wish this didn't have to be a separate connection, but idk how to do it differently without using HQL instead of SQL, which I don't care to learn
				con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/fictionbranches", "fictionbranches", "");
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}*/
			Strings.log("Database success");
		}
	}
	
	public static void closeSession() {
		synchronized (dbLock) {
		//session.close();
		sessionFactory.close();
		/*try {
			con.close();
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}*/
		}
	}
	
	public static class DBException extends Exception {
		/** */
		private static final long serialVersionUID = -1610662405195508706L;

		public DBException(String message) {
			super(message);
		}

		public DBException(Exception e) {
			super(e);
		}
	}
	
	/**
	 * 
	 * @param id
	 * @return null if episodes does not exist
	 */
	static DBEpisode getEpById(Session session, String id) {
		//synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);
			query.select(root).where(cb.equal(root.get("id"), id));
			DBEpisode result = session.createQuery(query).uniqueResult();
			return result;
		//}
	}
	
	/**
	 * Returns null if id does not exist
	 * @param id
	 * @return
	 */
	static DBUser getUserById(Session session, String id) {
		//synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.equal(root.get("id"), id));
			DBUser result = session.createQuery(query).uniqueResult();
			return result;
		//}
	}
	
	static DBUser getUserByEmail(Session session, String email) {
		//synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.equal(root.get("email"), email));
			DBUser result = session.createQuery(query).uniqueResult();
			return result;
		//}
	}
	
	/**
	 * 
	 * @param email
	 * @return
	 * @throws DBException if email not in use
	 */
	public static User getUserByEmail(String email) throws DBException {
		try (Session session = sessionFactory.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.equal(root.get("email"), email));
			DBUser result = session.createQuery(query).uniqueResult();
			if (result == null) throw new DBException("Email not found: " + email);
			else return new User(result);
		}
	}
	
	static TempUser[] getTempUsers() {
		try (Session session = sessionFactory.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.isNotNull(root.get("email")));
			List<DBUser> result = session.createQuery(query).list();

			TempUser[] list = new TempUser[result.size()];
			for (int i = 0; i < result.size(); ++i) list[i] = new TempUser(new User(result.get(i)));

			return list;
		}
	}
	
	/**
	 * Adds an episode to the story
	 * 
	 * This method checks that the parent episode and author exist, and that the new child id will not be longer than the allowed 4096 characters
	 * 
	 * This method DOES NOT check that link/title/body are within acceptable limits
	 * 
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return child DBEpisode object
	 * @throws DBException if parent ep or author does not exist, or if new keystring is too long
	 */
	public static Episode addEp(String id, String link, String title, String body, String authorId, Date date) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()){
			DBEpisode parent = getEpById(session, id);
			DBUser author = getUserById(session, authorId);

			if (parent == null) throw new DBException("Not found: " + id);
			if (author == null) throw new DBException("Author does not exist");

			DBEpisode child;
			child = new DBEpisode();
			
			String childId = id + "-" + (parent.getChildren().size()+1);
			child.setId(childId);
			child.setDepth(InitDB.keyToArr(childId).length);
			
			child.setTitle(title);
			child.setLink(link);
			child.setBody(body);
			child.setAuthor(author);
			child.setParent(parent);
			child.setDate(date);
			child.setChildCount(1);
			child.setEditDate(date);
			child.setEditor(author);
			author.getEditors().add(child);
			
			author.getEpisodes().add(child);
			
			parent.getChildren().add(child);
			
			ArrayList<DBEpisode> parents = new ArrayList<>(child.getDepth());
			parent.setChildCount(parent.getChildCount()+1);
			parents.add(parent);
			while (parents.get(parents.size()-1).getParent() != null && !parents.get(parents.size()-1).getParent().equals(parents.get(parents.size()-1))) {
				parent = parent.getParent();
				parent.setChildCount(parent.getChildCount()+1);
				parents.add(parent);
			}
			
			try {
				session.beginTransaction();
				session.save(child);
				for (DBEpisode parEp : parents) session.merge(parEp);
				session.merge(author);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
			Strings.log(String.format("New: <%s> %s %s", author, title, child.getId()));
			Episode ret = new Episode(child);
			return ret;
		}
		}
	}
	
	/**
	 * Adds a new root episode to the site
	 * 
	 * This method checks that the author exists, and that the new child id will not be longer than the allowed 4096 characters
	 * 
	 * This method DOES NOT check that link/title/body are within acceptable limits
	 * 
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return child DBEpisode object
	 * @throws DBException if parent ep or author does not exist, or if new keystring is too long
	 */
	public static Episode addRootEp(String link, String title, String body, String authorId, Date date) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser author = getUserById(session, authorId);
			
			Episode[] roots = getRoots();

			if (author == null) throw new DBException("Author does not exist");

			DBEpisode child;
			child = new DBEpisode();
			
			String childId = "" + (Integer.parseInt(roots[roots.length-1].id) + 1);

			child.setId(childId);
			child.setDepth(InitDB.keyToArr(childId).length);
			
			child.setTitle(title);
			child.setLink(link);
			child.setBody(body);
			child.setAuthor(author);
			child.setParent(null);
			child.setDate(date);
			child.setChildCount(1);
			child.setEditDate(date);
			child.setEditor(author);
			author.getEditors().add(child);
			
			author.getEpisodes().add(child);
		
			try {
				session.beginTransaction();
				session.save(child);
				session.merge(author);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
			Strings.log(String.format("New: <%s> %s %s", author, title, child.getId()));
			Episode ret = new Episode(child);
			return ret;
		}
		}
	}
	
	/**
	 * Modifies an episode of the story
	 * @param id id of episode
	 * @param title new title of new episode
	 * @param body new body of new episode
	 * @param author new author of new episode
	 * @throws DBException if id not found
	 */
	public static void modifyEp(String id, String link, String title, String body, String editorId) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBEpisode ep = getEpById(session, id);
			if (ep == null) throw new DBException("Not found: " + id);
			DBUser editor = getUserById(session, editorId);
			if (editor == null) throw new DBException("Editor not found: " + editorId);
			DBUser oldEditor = ep.getEditor();
			ep.setTitle(title);
			ep.setLink(link);
			ep.setBody(body);
			ep.setEditDate(new Date());
			ep.setEditor(editor);
			editor.getEditors().add(ep);
			oldEditor.getEditors().remove(ep);
			try {
				session.beginTransaction();
				session.merge(ep);
				session.merge(oldEditor);
				session.merge(editor);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
			Strings.log(String.format("Modified: <%s> %s", title, ep.getId()));
		}
		}
	}
	
	
	
	/**
	 * Retrieves an episode from the db by id
	 * @param id
	 * @return 
	 * @throws DBException if episode id does not exist
	 */
	public static Episode getEp(String id) throws DBException {
		try (Session session = sessionFactory.openSession()) {
			DBEpisode ep = getEpById(session, id);
			if (ep == null) throw new DBException("Not found: " + id);
			Episode ret = new Episode(ep);
			return ret;
		}
	}
	
	/**
	 * Retrieves an episode by its oldId
	 * @param id
	 * @return
	 * @throws DBException if id not found
	 */
	public static Episode getEpByLegacyId(String oldId) throws DBException {	
		try (Session session = sessionFactory.openSession()) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
		Root<DBEpisode> root = query.from(DBEpisode.class);
		
		query.select(root).where(cb.equal(root.get("legacyId"), oldId));
		
		List<DBEpisode> result = session.createQuery(query).list();
		
		if (result.size() == 0) return null;
		else if (result.size() > 1) {
			StringBuilder sb = new StringBuilder();
			for (DBEpisode ep : result) sb.append(ep.getGeneratedId() + " ");
			throw new RuntimeException("Multiple episodes have matching id: " + oldId + " " + sb);
		} else
			return new Episode(result.get(0));
		}
	}
	
	/**
	 * Get num most recent episodes of a particular story, or of all stories
	 * @param rootId root id for story, or 0 to get all stories
	 * @param num number of episodes to get
	 * @return
	 * @throws DBException
	 */
	public static Episode[] getRecents(int rootId, int num) throws DBException {
		try (Session session = sessionFactory.openSession()) {

			if (rootId != 0) DB.getEp(""+rootId);
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);

			query.select(root).orderBy(cb.desc(root.get("date")));
			
			if (rootId != 0) {
				Predicate idPredicate = cb.or(
						cb.like(root.get("id"), rootId + "-%"), 
						cb.equal(root.get("id"), rootId+""));
				query = query.where(idPredicate);
			}
			
			List<DBEpisode> result = session.createQuery(query).setMaxResults(num).list();
			Episode[] list = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) list[i] = new Episode(result.get(i));
			
			return list;
		}
	}
	
	public static Episode[] getOutline(String rootId, int maxDepth) throws DBException {
		try (Session session = sessionFactory.openSession()) {
			
			int minDepth = DB.getEp(rootId).depth;
			maxDepth += minDepth;
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);
			
			Predicate idPredicate = cb.or(
					cb.like(root.get("id"), rootId + "-%"), 
					cb.equal(root.get("id"), rootId+""));
			
			Predicate depthPredicate = cb.lt(root.get("depth"), maxDepth);
			
			
			query.select(root).where(cb.and(idPredicate,depthPredicate));
			
			List<DBEpisode> result = session.createQuery(query).list();
			Episode[] list = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) list[i] = new Episode(result.get(i));
			
			return list;
		}
	}
	
	public static Episode[] getPath(String id) throws DBException {
		Episode[] episodeList;
		try (Session session = sessionFactory.openSession()) {
			if (getEpById(session, id) == null) throw new DBException("Not found: " + id);
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);
			
			String[] ids = getPathIds(id);
			Predicate[] preds = new Predicate[ids.length];
			for (int i=0; i<ids.length; ++i) preds[i] = cb.equal(root.get("id"), ids[i]);
			
			query.select(root).where(cb.or(preds));
			
			List<DBEpisode> result = session.createQuery(query).list();
			
			episodeList = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) episodeList[i] = new Episode(result.get(i));
		}
		Arrays.sort(episodeList, Comparators.episodeKeyComparator);
		return episodeList;
	}
	
	private static String[] getPathIds(String id) {
		String[] arr = id.split("-");
		String[] ret = new String[arr.length];
		for (int i=0; i<arr.length; ++i) {
			StringBuilder sb = new StringBuilder();
			for (int j=0; j<=i; ++j) sb.append(arr[j] + "-");
			ret[i] = sb.substring(0, sb.length()-1);
		}
		return ret;
	}
	
	public static Episode[] getRoots() throws DBException {
		try (Session session = sessionFactory.openSession()) {
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);			
						
			query.select(root).where(cb.isNull(root.get("parent")));
			
			List<DBEpisode> result = session.createQuery(query).list();
			Episode[] list = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) list[i] = new Episode(result.get(i));
			
			Arrays.sort(list, new Comparator<Episode>() {
				public int compare(Episode o1, Episode o2) {
					return Comparators.keyStringComparator.compare(o1.id, o2.id);
				}
			});
			
			return list;
		}
	}
	
	/**
	 * Adds a new user to the database
	 * @param email
	 * @param password HASHED password (NOT PLAINTEXT!)
	 * @param author
	 * @return user ID of new user
	 * @throws DBException 
	 */
	public static String addUser(String username, String email, String password, String author) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
		
			if (getUserByEmail(session, email) != null) throw new DBException("Email " + email + " is already in use");
			
			DBUser user = new DBUser();
		
			user.setLevel((byte) 1);
			user.setAuthor(author);
			user.setEmail(email);
			user.setBio("");
			user.setPassword(password);
			user.setId(username);
			
			try {
				session.beginTransaction();
				session.save(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
			
			return username;
		}
		}
	}
	
	/**
	 * Change a user's author name
	 * @param id id of user
	 * @param newAuthor new author name
	 * @throws DBException if id not found
	 */
	public static void changeAuthorName(String id, String newAuthor) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setAuthor(newAuthor);
			try {
				session.beginTransaction();
				session.merge(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
	}
	
	/**
	 * Change a user's theme
	 * @param id id of user
	 * @param newTheme new theme (HTML name, not file name)
	 * @throws DBException if id not found
	 */
	public static void changeTheme(String id, String newTheme) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setTheme(newTheme);
			try {
				session.beginTransaction();
				session.merge(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
	}
	}
	
	/**
	 * Change a user's bio
	 * @param id id of user
	 * @param newBio new bio 
	 * @throws DBException if id not found
	 */
	public static void changeBio(String id, String newBio) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setBio(newBio);
			try {
				session.beginTransaction();
				session.merge(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
	}
	

	/**
	 * Change a user's author name
	 * @param id id of user
	 * @param newPassword new HASHED password (NOT PLAINTEXT)
	 * @throws DBException if id not found
	 */
	public static void changePassword(String id, String newPassword) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setPassword(newPassword);
			try {
				session.beginTransaction();
				session.merge(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
	}
	
	/**
	 * Change a user's user level (1=user, 10=mod, 100=admin)
	 * @param id user id
	 * @param newLevel new user level
	 * @throws DBException if user id not found
	 */
	public static void changeUserLevel(String id, byte newLevel) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setLevel(newLevel);
			try {
				session.beginTransaction();
				session.merge(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
	}

	/**
	 * Get a DBUser by id
	 * @param id id of user
	 * @throws DBException if id does not exist
	 */
	public static User getUser(String id) throws DBException {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User id does not exist");
			return new User(user);
		}
	}
	
	public static boolean emailInUse(String email) {
		try {
			getUserByEmail(email);
		} catch (DBException e) {
			return false;
		}
		return true;
	}
	
	public static boolean usernameInUse(String username) {
		try (Session session = sessionFactory.openSession()) {
			return getUserById(session, username) != null;
		}
	}
	
	/**
	 * Changes a user's email address in the db
	 * @param userId
	 * @param email
	 * @throws DBException
	 */
	public static void changeEmail(String userId, String email) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, userId);
			if (user == null) throw new DBException("User id does not exist");
			if (emailInUse(email)) throw new DBException("New email " + email + " already in use");

			user.setEmail(email);
			try {
				session.beginTransaction();
				session.merge(user);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
	}
	
	/**
	 * Checks a plaintext password against the stored hash
	 * @param id id of user
	 * @param password plaintext possible password
	 * @return true if password matches, else false
	 * @throws DBException if id not found
	 */
	public static boolean checkPassword(String id, String password) throws DBException {
		String hashedPassword;
		try (Session session = sessionFactory.openSession()) {
			DBUser user = getUserById(session, id);
			if (user == null) throw new DBException("User does not exist");
			if (user.getEmail() == null) throw new DBException("You may not log in to a legacy account");
			hashedPassword = user.getPassword();
		}
		boolean result;
		try {
			result = BCrypt.checkpw(password, hashedPassword);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}
	
	public static void flagEp(String episodeId, String authorId, String flagText) throws DBException {
		Date flagDate;
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBEpisode parent = getEpById(session, episodeId);
			DBUser author = getUserById(session, authorId);

			if (parent == null) throw new DBException("Episode not found: " + episodeId);
			if (author == null) throw new DBException("Author does not exist");

			DBFlaggedEpisode flag = new DBFlaggedEpisode();
			
			flag.setText(flagText);
			flag.setDate(new Date());
			flag.setEpisode(parent);
			flag.setUser(author);
			
			
			author.getFlags().add(flag);
			parent.getFlags().add(flag);
			
			flagDate = flag.getDate();
			
			try {
				session.beginTransaction();
				session.save(flag);
				session.merge(parent);
				session.merge(author);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
		Strings.log(String.format("Flag: <%s> %s %s", authorId, episodeId, flagDate));
	}
	
	public static FlaggedEpisode[] getFlags() {
		FlaggedEpisode[] ret;
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBFlaggedEpisode> query = cb.createQuery(DBFlaggedEpisode.class);
			Root<DBFlaggedEpisode> root = query.from(DBFlaggedEpisode.class);
			query.select(root);
			
			List<DBFlaggedEpisode> result = session.createQuery(query).list();
			ret = new FlaggedEpisode[result.size()];
			for (int i=0; i<ret.length; ++i) ret[i] = new FlaggedEpisode(result.get(i));
		}
		}
		Arrays.sort(ret, new Comparator<FlaggedEpisode>() {
			public int compare(FlaggedEpisode a, FlaggedEpisode b) {
				return a.date.compareTo(b.date);
			}
		});
		return ret;
	}
	
	public static FlaggedEpisode getFlag(long id) throws DBException {
		try (Session session = sessionFactory.openSession()) {
			DBFlaggedEpisode flag = session.get(DBFlaggedEpisode.class, id);
			if (flag == null) throw new DBException("Flag not found: " + id);
			return new FlaggedEpisode(flag);
		}
	}
	
	public static void clearFlag(long id) throws DBException {
		synchronized (dbLock) {
		try (Session session = sessionFactory.openSession()) {
			DBFlaggedEpisode flag = session.get(DBFlaggedEpisode.class, id);
			if (flag == null) throw new DBException("Flag not found: " + id);
			DBEpisode ep = flag.getEpisode();
			DBUser user = flag.getUser();
			ep.getFlags().remove(flag);
			user.getFlags().remove(flag);
			try {
				session.beginTransaction();
				session.delete(flag);
				session.merge(ep);
				session.merge(user);
			session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException("Database error");
			}
		}
		}
	}
	
	public static Random r = new Random();
}