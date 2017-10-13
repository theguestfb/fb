package fb.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;

import fb.Strings;

/**
 * Run this class's main() (as a regular Java Application, not on tomcat) to
 * initialize the database
 */
public class InitDB {

	public static void main(String[] args) throws Exception {

		Session session = DB.getSession();
		System.out.println("Starting import");
		long stop, start=System.nanoTime();
		
		readStory(session, "tfog", "4");
		stop = System.nanoTime();
		System.out.println("tfog: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory(session, "af", "3");
		stop = System.nanoTime();
		System.out.println("af: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory(session, "forum", "1");
		stop = System.nanoTime();
		System.out.println("forum: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory(session, "yawyw", "2");
		stop = System.nanoTime();
		System.out.println("yawyw: " + (((double)(stop-start))/1000000000.0));
		
		session.close();
		DB.getSessionFactory().close();
		System.out.println("Fin");
	}
	
	private static void readStory(Session session, String story, String rootId) {
		System.out.println("Importing " + story);
		String dirPath = "/Users/lpreams/Downloads/" + story + "/";
		
		session.beginTransaction();
		
		DBEpisode rootEp4 = readEpisode(new File(dirPath+"/root"));
		rootEp4.setLink("");
		rootEp4.setParent(rootEp4);
		rootEp4.setId(rootId);
		
		session.save(rootEp4);
		session.getTransaction().commit();
		
		TreeMap<String, String> map = new TreeMap<>(Strings.keyStringComparator); // <"1-2-3","01234someguy">
		Scanner index;
		try {
			index = new Scanner(new File(dirPath + "index.txt"));
		} catch (FileNotFoundException e) {
			System.out.println("index.txt  not found");
			return;
		}
		while (index.hasNext()) {
			String oldId = index.next();
			String newId = index.next();
			if (map.put(newId, oldId) != null) {
				System.out.println("Dupicate newId: " + newId);
				System.exit(1);
			}
		}
		index.close();
		
		boolean isFull = false;
		TreeSet<String> missingEpisodes = new TreeSet<>(Strings.keyStringComparator);
		while (!isFull) {
			isFull = true;
			for (String newId : map.keySet()) {
				// Check for missing episode above this one
				int[] key = keyToArr(newId);
				if (key[key.length - 1] != 1) {
					--key[key.length - 1];
					String missingKey = arrToKey(key);
					if (!map.containsKey(missingKey)) {
						missingEpisodes.add(newId);
						isFull = false;
					}
				}
			}
		}
		
		for (String newId : map.keySet()) {
			File f = new File(dirPath + map.get(newId));
			session.beginTransaction();
			String childId = newId;
			String parentId = getParentId(childId);
			DBEpisode child = readEpisode(f);
			if (child == null) {
				session.getTransaction().commit();
				System.out.println("Empty: " + newId);
				continue;
			}
			DBEpisode parent = session.get(DBEpisode.class, parentId);
			child.setId(childId);
			child.setParent(parent);
			
			parent.getChildren().add(child);
			session.save(child);
			session.merge(parent);
			session.getTransaction().commit();
		}
		
		for (String newId : missingEpisodes) {
			session.beginTransaction();
			String childId = newId;
			String parentId = getParentId(childId);
			DBEpisode child = new DBEpisode();
			
			child.setTitle("(Empty)");
			child.setLink("(Empty)");
			child.setAuthor("(Empty)");
			child.setBody("(Empty)");
			child.setDate(new Date());
			System.out.println("Missing: " + childId);
			DBEpisode parent = session.get(DBEpisode.class, parentId);
			child.setId(childId);
			child.setParent(parent);
			
			parent.getChildren().add(child);
			session.save(child);
			session.merge(parent);
			session.getTransaction().commit();
		}
	}
	
	private static int[] keyToArr(String s) {
		String[] arr = s.split("-");
		int[] ret = new int[arr.length];
		for (int i=0; i<arr.length; ++i) ret[i] = Integer.parseInt(arr[i]);
		return ret;
	}
	
	private static String arrToKey(int[] arr) {
		StringBuilder sb = new StringBuilder();
		for (int x : arr) sb.append(x + "-");
		return sb.substring(0, sb.length()-1);
	}
	
	private static String getParentId(String s) {
		String[] arr = s.split("-");
		StringBuilder ret = new StringBuilder();
		for (int i=0; i<arr.length-1; ++i) ret.append(arr[i] + "-");
		return ret.substring(0, ret.length()-1);
	}
	
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	private static DBEpisode readEpisode(File f) {
		try {
			Scanner in = new Scanner(f);
			DBEpisode ep = new DBEpisode();
			try {
				in.nextLine(); // skip oldId, not needed
				String id = in.nextLine();//System.out.println(in.nextLine()); // skit newId, not needed
				ep.setLink(trimTo(in.nextLine(), 254));
				ep.setTitle(trimTo(in.nextLine(), 254));
				ep.setAuthor(trimTo(in.nextLine(), 254));
				String dateString = in.nextLine();
				try {
					ep.setDate(df.parse(dateString));
				} catch (ParseException e) {
					ep.setDate(new Date());
					System.out.println("Bad header: " + id);
				}
			} catch (NoSuchElementException e) {
				in.close();
				if (ep.getTitle() == null) ep.setTitle("(Empty)");
				if (ep.getLink() == null) ep.setLink("(Empty)");
				if (ep.getBody() == null) ep.setBody("");
				if (ep.getAuthor() == null) ep.setAuthor("(Empty)");
				if (ep.getDate() == null) ep.setDate(new Date());
				System.out.println("Empty episode: " + f.getName());
				return ep;
			}
			String line = in.nextLine();
			while (in.hasNext() && line.trim().length() == 0) line = in.nextLine();
			StringBuilder body = new StringBuilder();
			body.append(line + "\n");
			while (in.hasNext()) body.append(in.nextLine() + "\n");
			ep.setBody(body.toString());
			in.close();
			return ep;
		} catch (FileNotFoundException e) {
			System.err.println("Error: file not found " + f.getAbsolutePath());
			throw new RuntimeException();
		} 
	}
	
	private static String trimTo(String s, int l) {
		if (s.length() <= l) return s;
		else return s.substring(0, l);
	}
	
	private static void yawywOld(Session session) {
		System.out.println("Making parent episode");

		// DB.addEp() cannot be used since it requires
		// a parent ID and the root has no parent. Instead,
		// it must be created and added manually
		
		// The following block could later be used
		// to initialize multiple stories

		// Create the episode
		DBEpisode parent = new DBEpisode();
		parent.setTitle("And So It Begins...");
		parent.setBody(Strings.readFile("ep1.txt"));
		parent.setLink("");
		parent.setAuthor("Zach");
		parent.setId("2");
		parent.setDate(new Date());

		// Commit the episode to the DB
		
		session.beginTransaction();
		String parentID = (String) session.save(parent);
		session.getTransaction().commit();

		// Now we can use DB.addEp() for the first child
		DBEpisode child = DB.addEp("1", "episode two", "Enter the Bitch", Strings.readFile("ep2.txt"),
				"Jezzi_Belle_Stewart", new Date());

		System.out.println("Root ID: " + parentID);
		System.out.println("Chld ID: " + child.getId());
	}
	
}
