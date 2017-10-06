package fb;

import java.util.Arrays;
import java.util.Random;

import org.hibernate.Session;

public class InitDB {

	/*public static void main(String[] args) {
		
		System.out.println("Making parent episode");
		
		FBEpisode parent = new FBEpisode();
		parent.setTitle("This is the root episode");
		parent.setBody("This is the body of the root episode");
		parent.setAuthor("Phoenix");
		parent.setId("1");
		
		Session session = DB.getSession();
		session.beginTransaction();
		String parentID = (String) session.save(parent);
		session.getTransaction().commit();
		
		FBEpisode child = new FBEpisode();
		child.setTitle("This is a child episode");
		child.setBody("This is the body of a child episode");
		child.setAuthor("Phoenix");
		
		session = DB.getSession();
		session.beginTransaction();
		
		parent = session.get(FBEpisode.class, parentID);
		child.setId(parent.getId()+"-"+(1+parent.getChildren().size()));
		child.setParent(parent);
		parent.getChildren().add(child);
		
		String childID = (String) session.save(child);
		session.merge(parent);
		session.getTransaction().commit();
		session.close();
		
		System.out.println("Root ID: " + parentID);
		System.out.println("Chld ID: " + childID);
		
		session.close();
		DB.getSessionFactory().close();
		System.out.println("Fin");
	}*/
}
