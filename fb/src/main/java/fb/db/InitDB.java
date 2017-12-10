package fb.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;

import fb.db.DB.DBException;
import fb.json.TempUser;
import fb.json.TempUser.TempEpisode;
import fb.util.Comparators;
import fb.util.Strings;

/**
 * Run this class's main() (as a regular Java Application, not on tomcat) to
 * initialize the database
 */
public class InitDB {

	
	private static final boolean PRINT_EPISODES_ADDED = true; // enable to print each episode's newid to stdout when it is added to the db
	
	
	private static Random r = new Random();
	
	
	public static void countDB()  {
			/***** Count episodes in DB ******/
			long stop, start = System.nanoTime();

			int c = count("4");
			stop = System.nanoTime();
			System.out.println("finished tfog: " + c + " " + (((double) (stop - start)) / 1000000000.0));
			start = System.nanoTime();

			c = count("3");
			stop = System.nanoTime();
			System.out.println("finished af: " + c + " " + (((double) (stop - start)) / 1000000000.0));
			start = System.nanoTime();

			c = count("1");
			stop = System.nanoTime();
			System.out.println("finished forum: " + c + " " + (((double) (stop - start)) / 1000000000.0));
			start = System.nanoTime();

			c = count("2");
			stop = System.nanoTime();
			System.out.println("finished yawyw: " + c + " " + (((double) (stop - start)) / 1000000000.0));
			start = System.nanoTime();
	}
	

	public static void doImport() throws DBException  {
		
		
		try (Scanner in = new Scanner(System.in)) {
			
			System.out.println("enter root password:");
			String rootpw = in.nextLine();
			
			DB.session.beginTransaction();

			DBUser user = new DBUser();
			user.setEmail("admin@fictionbranches.net");
			user.setId(DB.ROOT_ID);
			user.setLevel((byte)100);
			user.setAuthor("FB Admin");
			user.setPassword(BCrypt.hashpw(rootpw, BCrypt.gensalt(10)));
			user.setBio("Fiction Branches Admin Account");
			DB.session.save(user);
			DB.session.getTransaction().commit();
									
			System.out.println("enter phoenix password:");
			String phoenixID = DB.addUser("root@carolinaphoenix.net", BCrypt.hashpw(in.nextLine(), BCrypt.gensalt(10)), "Phoenix");
			DB.changeUserLevel(phoenixID, (byte)100);
		}
				
		Strings.log("Starting import");
		long stop, start=System.nanoTime();
		
		readStory("tfog", "4");
		stop = System.nanoTime();
		Strings.log("finished tfog: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory("af", "3");
		stop = System.nanoTime();
		Strings.log("finished af: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory("forum", "1");
		stop = System.nanoTime();
		Strings.log("finished forum: " + (((double)(stop-start))/1000000000.0));
		start = System.nanoTime();
		
		readStory("yawyw", "2");
		stop = System.nanoTime();
		Strings.log("finished yawyw: " + (((double)(stop-start))/1000000000.0));
		
		start = System.nanoTime();
		System.out.println("Adding temp users/episodes");
		importTemp();
		stop = System.nanoTime();
		Strings.log("finished temp import: " + (((double)(stop-start))/1000000000.0));
		
		generateChildCounts();
		
		
		DB.closeSession();
		Strings.log("Fin");
		System.exit(0);
	}
	
	public static void exportTemp() throws DBException {
		String dirPath = System.getProperty("user.home") + "/fbscrape/temp/";
		for (TempUser tempUser : DB.getTempUsers()) {
			String json = new Gson().toJson(tempUser);
			try (BufferedWriter out = new BufferedWriter(new FileWriter(dirPath + tempUser.getId()))) {
				out.write(json);
				out.flush();
			} catch (IOException e) {
				System.out.println("Error writing to file " + dirPath + tempUser.getId());
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void importTemp() throws DBException {
		String dirPath = System.getProperty("user.home") + "/fbscrape/temp/";
		if (!(new File(dirPath).exists())) {
			System.out.println("Temp directory not found: " + dirPath);
			return;
		} else if (!(new File(dirPath).isDirectory())) {
			System.out.println("Temp exists but is not a directory: " + dirPath);
			return;
		}
		ArrayList<TempUser.TempEpisode> episodes = new ArrayList<>();
		
		for (File f : new File(dirPath).listFiles()) {
			String json = Strings.readFile(f);
			
			TempUser tempUser = new Gson().fromJson(json, TempUser.class);
			String userId;
			if (DB.emailInUse(tempUser.getEmail())) userId = DB.getUserByEmail(tempUser.getEmail()).id;
			else userId = DB.addUser(tempUser.getEmail(), tempUser.getHashedPassword(), tempUser.getAuthor());

			DBUser user = DB.session.get(DBUser.class, userId);
			user.setBio(tempUser.getBio());
			user.setLevel(tempUser.getLevel());
			user.setTheme(tempUser.getTheme());
			DB.session.beginTransaction();
			DB.session.merge(user);
			DB.session.getTransaction().commit();
			
			for (TempUser.TempEpisode tempEp : tempUser.getEpisodes()) {
				tempEp.setAuthorId(userId);
				episodes.add(tempEp);
			}
		}
		
		Collections.sort(episodes, new Comparator<TempUser.TempEpisode>() {
			@Override
			public int compare(TempEpisode a, TempEpisode b) {
				return Comparators.keyStringComparator.compare(a.getId(), b.getId());
			}
		});
		
		for (TempUser.TempEpisode tempEp : episodes) {
			boolean skipEp;
			try {
				DB.getEp(tempEp.getId());
				skipEp = true;
			} catch (DBException e) {
				System.out.println("Skipping existing tempEp: " + tempEp.getId());
				skipEp = false;
			}
			if (skipEp) continue;
			DBUser user = DB.getUserById(tempEp.getAuthorId());
			DBEpisode ep = new DBEpisode();
			System.out.println("Adding temp ep: " + tempEp.getId());
			DBEpisode parent;
			try {
				String parentId = getParentId(tempEp.getId());
				parent = DB.getEpById(parentId);
			} catch (StringIndexOutOfBoundsException e) {
				parent = ep;
			}
			ep.setBody(tempEp.getBody());
			ep.setChildCount(0);
			ep.setDate(tempEp.getDate());
			ep.setEditDate(tempEp.getEditDate());
			ep.setEditor(user);
			ep.setId(tempEp.getId());
			ep.setLegacyId(null);
			ep.setLink(tempEp.getLink());
			ep.setTitle(tempEp.getTitle());
			ep.setParent(parent);
			parent.getChildren().add(ep);
			ep.setAuthor(user);
			ep.setChildren(new ArrayList<DBEpisode>());
			ep.setDepth(tempEp.getDepth());
			user.getEpisodes().add(ep);
			DB.session.beginTransaction();
			DB.session.save(ep);
			DB.session.merge(parent);
			DB.session.merge(user);
			DB.session.getTransaction().commit();
		}
	}
	
	private static ArrayList<String> rootIdList = new ArrayList<>();
	
	public static void generateChildCounts() throws DBException {
		if (rootIdList.size() == 0) {
			if (DB.getEpById("4") != null) rootIdList.add("4");
			if (DB.getEpById("3") != null) rootIdList.add("3");
			if (DB.getEpById("1") != null) rootIdList.add("1");
			if (DB.getEpById("2") != null) rootIdList.add("2");
		}
		for (String rootId : rootIdList) {
			DB.session.beginTransaction();
			long start = System.nanoTime();
			generateChildCounts(rootId);
			long stop = System.nanoTime();
			Strings.log("Generated child counts: " + ((((double)(stop-start))/1000000000.0)) + " " + rootId);
			start = System.nanoTime();
			DB.session.getTransaction().commit();
			stop = System.nanoTime();
			Strings.log("Persisted child counts: " + ((((double)(stop-start))/1000000000.0)) + " " + rootId);
		}
	}
		
	/**
	 * Count episodes in tree
	 * @param id of root of tree
	 * @return number of episodes (including root) in tree
	 * @throws IOException 
	 */
	static int count(String id) {
		DBEpisode ep;
		try {
			ep = DB.getEpById( id);
		} catch (DBException e) {
			throw new RuntimeException(e);
		}
		if (ep == null) System.err.println("null");
		int sum = 1; // count this episode
		if (ep.getChildren() != null) for (DBEpisode child : ep.getChildren()) sum+=count(child.getId());
		return sum;
	}
	
	private static int generateChildCounts(String id) {
		DBEpisode ep;
		try {
			ep = DB.getEpById(id);
		} catch (DBException e) {
			throw new RuntimeException(e);
		}
		if (ep == null) System.err.println("null");
		int sum = 1; // count this episode
		if (ep.getChildren() != null) for (DBEpisode child : ep.getChildren()) {
			int x = generateChildCounts(child.getId());
			sum+=x;
		}
		//DB.session.beginTransaction();
		ep.setChildCount(sum);
		DB.session.merge(ep);
		//DB.session.getTransaction().commit();
		if (PRINT_EPISODES_ADDED) System.out.println("Generated child counts: " + id);
		return sum;
	}
	
	private static void readStory(String story, String rootId) {
		Strings.log("Importing " + story);
		rootIdList.add(rootId);
		String dirPath = System.getProperty("user.home") + "/fbscrape/" + story + "/";
		
		DB.session.beginTransaction();
		
		DBUser newUser = new DBUser();
		newUser.setEmail(null);
		newUser.setLevel((byte)1);
		{
			String id = genLegacyID();
			while (DB.session.get(DBUser.class, id) != null) id = genLegacyID();
			newUser.setId(id);
		}
		Strings.log("Loading root of " + story);
		LegacyEpisodeContainer rootCont = readEpisode(new File(dirPath+"/root"));
		DBEpisode rootEp = rootCont.ep;
		rootEp.setParent(null);
		rootEp.setId(rootId);
		rootEp.setDepth(keyToArr(rootId).length);
		newUser.getEpisodes().add(rootEp);
		rootEp.setAuthor(newUser);
		rootEp.setEditor(newUser);
		newUser.getEditors().add(rootEp);
		newUser.setAuthor(rootCont.author);
		newUser.setPassword("disabled");
		newUser.setBio("");
				
		DB.session.save(rootEp);
		DB.session.save(newUser);
		DB.session.getTransaction().commit();
		
		Strings.log("Loading index of " + story);
		TreeMap<String, String> map = new TreeMap<>(Comparators.keyStringComparator); // <"1-2-3","01234someguy">
		Scanner index;
		try {
			index = new Scanner(new File(dirPath + "index.txt"));
		} catch (FileNotFoundException e) {
			Strings.log("index.txt  not found for " + story + " " + dirPath + "index.txt");
			return;
		}
		boolean passed = true;
		while (index.hasNext()) {
			String oldId = index.next();
			String newId = index.next();
			if (oldId.equals("root") || keyToArr(newId).length == 1) continue;
			if (map.containsKey(newId)) {
				Strings.log("Duplicate newId: " + newId + " " + oldId + " " + map.get(newId));
				//System.exit(1);
				passed = false;
			} else map.put(newId, oldId);
		}
		index.close();
		if (!passed) System.exit(1);
		
		/*Thread legacyThread = new Thread() {
			public void run() {
				Strings.log("Persisting legacy IDs for " + story);
				for (String newId : map.keySet()) {
					DB.session.beginTransaction();
					String oldId = map.get(newId);
					DBLegacyId legacy = new DBLegacyId();
					legacy.setId(oldId);
					legacy.setNewId(newId);
					DB.session.save(legacy);
					DB.session.getTransaction().commit();
					if (PRINT_EPISODES_ADDED) System.out.println("Added legacyid " + newId);
				}
			}
		};
		legacyThread.start();*/
		
		
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
		
		Strings.log("Done finding missing eps for " + story); 
		/*try {
			legacyThread.join();
		} catch (InterruptedException e) {
			Strings.log("Error joining legacyThread");
			System.exit(2);
		}*/
		
		for (String id : missingEpisodes) {
			if (map.put(id, null) != null) {
				throw new RuntimeException(id + " was marked missing, but exists in map");
			}
		}
		
		
		Strings.log("Persisting episodes for " + story); 
		for (String newId : map.keySet()) {
			if (missingEpisodes.contains(newId)) { // episode needs to exist but doesn't, so make it from scratch
				DB.session.beginTransaction();
				String childId = newId;
				String parentId = getParentId(childId);
				DBEpisode child = new DBEpisode();
				DBUser user = new DBUser();
				
				{
					String id = genLegacyID();
					while (DB.session.get(DBUser.class, id) != null) id = genLegacyID();
					user.setId(id);
				}
				child.setTitle("(Empty)");
				child.setLink("(Empty)");
				child.setAuthor(user);
				child.setBody("(Empty)");
				child.setDate(badDate);
				child.setEditor(user);
				user.getEditors().add(child);
				child.setEditDate(badDate);
				user.getEpisodes().add(child);
				user.setAuthor("(Empty)");
				user.setEmail(null);
				user.setLevel((byte)1);
				user.setPassword("disabled");
				user.setBio("");
				Strings.log("ID must exist, but doesn't: " + childId);
				DBEpisode parent;
				try {
					parent = DB.getEpById( parentId);
				} catch (DBException e) {
					throw new RuntimeException(e);
				}
				child.setId(childId);
				child.setParent(parent);
				
				child.setDepth(keyToArr(child.getId()).length);
				
				parent.getChildren().add(child);
				DB.session.save(user);
				DB.session.save(child);
				DB.session.merge(parent);
				DB.session.getTransaction().commit();
				if (PRINT_EPISODES_ADDED) System.out.println("Added episode " + child.getId());
			} else { // otherwise, load the episode from file
				File f = new File(dirPath + map.get(newId));
				DB.session.beginTransaction();
				String childId = newId;
				String parentId = getParentId(childId);
				LegacyEpisodeContainer epCont = readEpisode(f);
				DBEpisode child = epCont.ep;
								
				DBEpisode parent;
				try {
					parent = DB.getEpById( parentId);
				} catch (DBException e) {
					throw new RuntimeException(e);
				}
				
				DBUser user = new DBUser();
				{
					String id = genLegacyID();
					while (DB.session.get(DBUser.class, id) != null) id = genLegacyID();
					user.setId(id);
				}
				user.setAuthor(epCont.author);
				user.setEmail(null);
				user.setLevel((byte)1);
				user.setPassword("disabled");
				user.setBio("");
								
				child.setId(childId);
				child.setLegacyId(map.get(childId));
				child.setDepth(keyToArr(childId).length);
				child.setParent(parent);
				child.setAuthor(user);
				child.setEditor(user);
				user.getEditors().add(child);
				user.getEpisodes().add(child);
				user.getEditors().add(child);
				
				parent.getChildren().add(child);
				DB.session.save(user);
				DB.session.save(child);
				DB.session.merge(parent);
				DB.session.getTransaction().commit();
				if (PRINT_EPISODES_ADDED) System.out.println("Added episode " + child.getId());
			}
		}
	}
	
	private static final Date badDate;
	static {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, 1999);
		c.set(Calendar.MONTH, 12);
		c.set(Calendar.DAY_OF_MONTH, 31);
		c.set(Calendar.HOUR, 12);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		badDate = c.getTime();
		System.out.println(badDate);
	}
	
	private static class LegacyEpisodeContainer {
		public final DBEpisode ep;
		public final String author;
		public LegacyEpisodeContainer(DBEpisode ep, String author) {
			this.ep = ep;
			this.author = author;
		}
	}
	
	public static int[] keyToArr(String s) {
		String[] arr = s.split("-");
		int[] ret = new int[arr.length];
		for (int i=0; i<arr.length; ++i) ret[i] = Integer.parseInt(arr[i]);
		return ret;
	}
	
	public static String arrToKey(int[] arr) {
		StringBuilder sb = new StringBuilder();
		for (int x : arr) sb.append(x + "-");
		return sb.substring(0, sb.length()-1);
	}
	
	public static String getParentId(String s) {
		String[] arr = s.split("-");
		StringBuilder ret = new StringBuilder();
		for (int i=0; i<arr.length-1; ++i) ret.append(arr[i] + "-");
		return ret.substring(0, ret.length()-1);
	}
	
	public static String getOlderSiblingId(String s) {
		int[] arr = keyToArr(s);
		arr[arr.length-1]--;
		return (arr[arr.length-1] >= 1)?(arrToKey(arr)):(null);
	}
	
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	/**
	 * Sets title, link, author*, body, and date
	 * Does not set id or parent
	 * *author is in separate field in return object
	 * @param f
	 * @return
	 */
	private static LegacyEpisodeContainer readEpisode(File f) {
		try {
			Scanner in = new Scanner(f);
			DBEpisode ep = new DBEpisode();
			String author = null;
			try {
				in.nextLine(); // Don't need oldId now
				String id = in.nextLine(); // Only used for logging here
				ep.setLink(trimTo(in.nextLine(), 254).replace("%3f", "?"));
				ep.setTitle(trimTo(in.nextLine(), 254));
				author = trimTo(in.nextLine(), 254);
				String dateString = in.nextLine();
				try {
					Date date = df.parse(dateString);
					ep.setDate(date);
					ep.setEditDate(date);
				} catch (ParseException e) {
					ep.setDate(badDate);
					ep.setEditDate(badDate);
					Strings.log("Bad header: " + id + " " + f.getAbsolutePath());
				}
			} catch (NoSuchElementException e) {
				in.close();
				if (ep.getTitle() == null) ep.setTitle("(Empty)");
				if (ep.getLink() == null) ep.setLink("(Empty)");
				if (author == null) author = "(Empty)";
				if (ep.getBody() == null) ep.setBody("");
				if (ep.getDate() == null) {
					ep.setDate(badDate);
					ep.setEditDate(badDate);
				}
				Strings.log("(partially) empty episode: " + f.getName());
				return new LegacyEpisodeContainer(ep, author);
			}
			String line = in.nextLine();
			while (in.hasNext() && line.trim().length() == 0) line = in.nextLine();
			StringBuilder body = new StringBuilder();
			body.append(line + "  \n");
			while (in.hasNext()) body.append(in.nextLine() + "  \n");
			ep.setBody(body.toString().replace('`', '\''));
			in.close();
			return new LegacyEpisodeContainer(ep, author);
		} catch (FileNotFoundException e) {
			Strings.log("Error: file not found " + f.getAbsolutePath());
			throw new RuntimeException(e);
		} 
	}
	
	private static String trimTo(String s, int l) {
		if (s.length() <= l) return s;
		else return s.substring(0, l);
	}
	
	private static ArrayList<Character> idChars = new ArrayList<>();
	static {
		for (char c='a'; c<='z'; ++c) idChars.add(c);
		for (char c='A'; c<='Z'; ++c) idChars.add(c);
		for (char c='0'; c<='9'; ++c) idChars.add(c);
	}
	private static String genLegacyID() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<8; ++i) sb.append(idChars.get(r.nextInt(idChars.size())));
		return "legacy" + sb.toString();
	}
}
