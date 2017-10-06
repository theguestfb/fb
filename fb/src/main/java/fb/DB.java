package fb;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class DB {
	private static SessionFactory sessionFactory;
	private static Session session;
	static {
		Configuration configuration = new Configuration().configure();
		
		configuration.addAnnotatedClass(FBEpisode.class);
		
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings(configuration.getProperties());
		sessionFactory = configuration.buildSessionFactory(builder.build());
		session = sessionFactory.openSession();
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	public static Session getSession() {
		return session;
	}
}