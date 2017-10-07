package fb.db;

import java.util.Calendar;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class DB {
	private static SessionFactory sessionFactory;
	private static Session session;
	static {
		Configuration configuration = new Configuration().configure();
		configuration.addAnnotatedClass(DBEpisode.class);	
		
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings(configuration.getProperties());
		sessionFactory = configuration.buildSessionFactory(builder.build());
		session = sessionFactory.openSession();
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
	public static DBEpisode addEp(String id, String link, String title, String body, String author) {
		Session session = DB.getSession();
		session.beginTransaction();
		DBEpisode parent = session.get(DBEpisode.class, id);
		DBEpisode child;
		if (parent == null) child = null;
		else {
			child = new DBEpisode();
			child.setTitle(title);
			child.setLink(link);
			child.setBody(body);
			child.setAuthor(author);
			child.setParent(parent);
			child.setId(id+"-"+(1+parent.getChildren().size()));
			parent.getChildren().add(child);
			session.save(child);
			session.merge(parent);
			log(String.format("New: <%s> %s", author, title));
		}
		session.getTransaction().commit();
		return child;
	}
	
	/**
	 * Retrieves an episode from the db by id
	 * @param id
	 * @return
	 */
	public static DBEpisode getEp(String id) {
		Session session = DB.getSession();
		session.beginTransaction();
		DBEpisode ep = session.get(DBEpisode.class, id);
		session.getTransaction().commit();
		return ep;
	}
	
	/**
	 * Prepends message with the current date, and writes it to stdout
	 * @param message
	 */
	private static void log(String message) {
		int y = Calendar.getInstance().get(Calendar.YEAR);
		int mo = Calendar.getInstance().get(Calendar.MONTH);
		int d = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int mi = Calendar.getInstance().get(Calendar.MINUTE);
		int s = Calendar.getInstance().get(Calendar.SECOND);
		System.out.printf("%04d-%02d-%02d %02d:%02d:%02d %s%n", y, mo, d, h, mi, s, message);

	}
}