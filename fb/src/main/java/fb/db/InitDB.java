package fb.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeMap;

import org.hibernate.Session;

import fb.Comparators;
import fb.Strings;

/**
 * Run this class's main() (as a regular Java Application, not on tomcat) to
 * initialize the database
 */
public class InitDB {

	public static void main(String[] args) throws Exception {

		Session session = DB.getSession();
				
		Strings.log("Starting import");
		long stop, start=System.nanoTime();
		
		readStory(session, "tfog", "4");
		stop = System.nanoTime();
		Strings.log("finished tfog: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory(session, "af", "3");
		stop = System.nanoTime();
		Strings.log("finished af: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory(session, "forum", "1");
		stop = System.nanoTime();
		Strings.log("finished forum: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory(session, "yawyw", "2");
		stop = System.nanoTime();
		Strings.log("finished yawyw: " + (((double)(stop-start))/1000000000.0));
		
		// Recents HAS TO be initialized like this for it to work, otherwise null pointers will happen!!
		Strings.log("Initializing recents");
		DBRecents recents = new DBRecents();
		recents.setId(1);
		recents.getRecents().add(DB.getEp("1"));
		recents.getRecents().add(DB.getEp("2"));
		recents.getRecents().add(DB.getEp("3"));
		recents.getRecents().add(DB.getEp("4"));
		session.beginTransaction();
		session.save(recents);
		session.getTransaction().commit();
		Strings.log("Added recents");
		
		session.close();
		DB.getSessionFactory().close();
		Strings.log("Fin");
	}
	
	private static void readStory(Session session, String story, String rootId) {
		Strings.log("Importing " + story);
		String dirPath = "/Users/lpreams/Downloads/scrape10/" + story + "/";
		
		session.beginTransaction();
		
		Strings.log("Loading root of " + story);
		DBEpisode rootEp = readEpisode(new File(dirPath+"/root"));
		rootEp.setParent(null);
		rootEp.setId(rootId);
		
		session.save(rootEp);
		session.getTransaction().commit();
		
		Strings.log("Loading index of " + story);
		TreeMap<String, String> map = new TreeMap<>(Comparators.keyStringComparator); // <"1-2-3","01234someguy">
		Scanner index;
		try {
			index = new Scanner(new File(dirPath + "index.txt"));
		} catch (FileNotFoundException e) {
			Strings.log("index.txt  not found for " + story + " " + dirPath + "index.txt");
			return;
		}
		while (index.hasNext()) {
			String oldId = index.next();
			String newId = index.next();
			if (map.put(newId, oldId) != null) {
				Strings.log("Dupicate newId: " + newId + " " + oldId);
				System.exit(1);
			}
		}
		index.close();
		
		Thread legacyThread = new Thread() {
			public void run() {
				Strings.log("Persisting legacy IDs for " + story);
				for (String newId : map.keySet()) {
					session.beginTransaction();
					String oldId = map.get(newId);
					DBLegacyId legacy = new DBLegacyId();
					legacy.setId(oldId);
					legacy.setNewId(newId);
					session.save(legacy);
					session.getTransaction().commit();
				}
			}
		};
		legacyThread.start();
		
		
		Strings.log("Finding missing eps for " + story); 
		
		HashSet<String> missingEpisodes = new HashSet<>();
		// Figure out where stuff is missing from the scrape
		for (String id : map.keySet()) {
			String parentId = getParentId(id);
			String olderSiblingId = getOlderSiblingId(id);
			if (keyToArr(parentId).length > 1 && !map.containsKey(parentId)) {
				missingEpisodes.add(parentId);
			}
			if (olderSiblingId != null) if (!map.containsKey(olderSiblingId)) {
				missingEpisodes.add(olderSiblingId);
			}
		}
		
		boolean noneMissing = false;
		while (noneMissing == false) {
			noneMissing = true;
			HashSet<String> newMissingEpisodess = new HashSet<>();
			for (String id : missingEpisodes) {
				String parentId = getParentId(id);
				String olderSiblingId = getOlderSiblingId(id);
				if (keyToArr(parentId).length > 1 && !missingEpisodes.contains(parentId) && !map.containsKey(parentId)) {
					newMissingEpisodess.add(parentId);
					noneMissing = false;
				}
				if (olderSiblingId != null) if (!missingEpisodes.contains(olderSiblingId) && !map.containsKey(olderSiblingId)) {
					newMissingEpisodess.add(olderSiblingId);
					noneMissing = false;
				}
			}
			missingEpisodes.addAll(newMissingEpisodess);
		}
		
		Strings.log("Done finding missing eps for " + story + ", waiting for legacy persistence to finish"); 
		try {
			legacyThread.join();
		} catch (InterruptedException e) {
			Strings.log("Error joining legacyThread");
			System.exit(2);
		}
		
		for (String id : missingEpisodes) {
			if (map.put(id, null) != null) {
				throw new RuntimeException(id + " was marked missing, but exists in map");
			}
		}
		
		Strings.log("Persisting episodes for " + story); 
		for (String newId : map.keySet()) {
			if (missingEpisodes.contains(newId)) { // episode needs to exist but doesn't, so make it from scratch
				session.beginTransaction();
				String childId = newId;
				String parentId = getParentId(childId);
				DBEpisode child = new DBEpisode();
				
				child.setTitle("(Empty)");
				child.setLink("(Empty)");
				child.setAuthor("(Empty)");
				child.setBody("(Empty)");
				child.setDate(new Date());
				Strings.log("ID must exist, but doesn't: " + childId);
				DBEpisode parent = session.get(DBEpisode.class, parentId);
				child.setId(childId);
				child.setParent(parent);
				
				parent.getChildren().add(child);
				session.save(child);
				session.merge(parent);
				session.getTransaction().commit();
			} else { // otherwise, load the episode from file
				File f = new File(dirPath + map.get(newId));
				session.beginTransaction();
				String childId = newId;
				String parentId = getParentId(childId);
				DBEpisode child = readEpisode(f);
				if (child == null) {
					throw new RuntimeException("Null child: " + newId + " " + f.getName());
					/*session.getTransaction().commit();
					Strings.log("Episode exists but is (partially) empty: " + newId + " " + f.getName());
					continue;*/
				}
				DBEpisode parent = session.get(DBEpisode.class, parentId);
				child.setId(childId);
				child.setParent(parent);

				parent.getChildren().add(child);
				session.save(child);
				session.merge(parent);
				session.getTransaction().commit();
			}
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
	
	private static String getOlderSiblingId(String s) {
		int[] arr = keyToArr(s);
		arr[arr.length-1]--;
		return (arr[arr.length-1] >= 1)?(arrToKey(arr)):(null);
	}
	
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	private static DBEpisode readEpisode(File f) {
		try {
			Scanner in = new Scanner(f);
			DBEpisode ep = new DBEpisode();
			try {
				in.nextLine(); // skip oldId, not needed
				String id = in.nextLine();//Strings.log(in.nextLine()); // skit newId, not needed
				ep.setLink(trimTo(in.nextLine(), 254));
				ep.setTitle(trimTo(in.nextLine(), 254));
				ep.setAuthor(trimTo(in.nextLine(), 254));
				String dateString = in.nextLine();
				try {
					ep.setDate(df.parse(dateString));
				} catch (ParseException e) {
					ep.setDate(new Date());
					Strings.log("Bad header: " + id + " " + f.getAbsolutePath());
				}
			} catch (NoSuchElementException e) {
				in.close();
				if (ep.getTitle() == null) ep.setTitle("(Empty)");
				if (ep.getLink() == null) ep.setLink("(Empty)");
				if (ep.getBody() == null) ep.setBody("");
				if (ep.getAuthor() == null) ep.setAuthor("(Empty)");
				if (ep.getDate() == null) ep.setDate(new Date());
				Strings.log("(partially) empty episode: " + f.getName());
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
			Strings.log("Error: file not found " + f.getAbsolutePath());
			throw new RuntimeException();
		} 
	}
	
	private static String trimTo(String s, int l) {
		if (s.length() <= l) return s;
		else return s.substring(0, l);
	}
	
	private static void yawywOld(Session session) {
		Strings.log("Making parent episode");

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

		Strings.log("Root ID: " + parentID);
		Strings.log("Chld ID: " + child.getId());
	}
	
}
