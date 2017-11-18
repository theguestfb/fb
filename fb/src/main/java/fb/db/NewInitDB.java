package fb.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import org.mindrot.jbcrypt.BCrypt;

import fb.Comparators;
import fb.Strings;
import fb.db.DB.DBException;

public class NewInitDB {

	public static void main(String[] args) throws Exception {
		doImport();
	}
	
	public static void doImport() throws DBException {
		initUsers();
		readStory("1", "forum");
	}
	
	private static void initUsers() throws DBException {
		try (Scanner in = new Scanner(System.in)) {
			
			System.out.println("enter root password:");
			String rootpw = in.nextLine();
			
			DB.session.beginTransaction();

			DBUser user = new DBUser();
			DBEmail dbemail = new DBEmail();
			dbemail.setEmail("admin@fictionbranches.net");
			user.setId(DB.ROOT_ID);
			user.setLevel((byte)100);
			user.setAuthor("FB Admin");
			user.setPassword(BCrypt.hashpw(rootpw, BCrypt.gensalt(10)));
			user.setEmail(dbemail);
			dbemail.setUser(user);
			DB.session.save(user);
			DB.session.save(dbemail);
			DB.session.getTransaction().commit();
									
			System.out.println("enter phoenix password:");
			String phoenixID = DB.addUser("root@carolinaphoenix.net", BCrypt.hashpw(in.nextLine(), BCrypt.gensalt(10)), "Phoenix");
			DB.changeUserLevel(phoenixID, (byte)100);
		}
	}

	private static void readStory(String rootID, String story) {
		Strings.log("Importing " + story);
		String dirPath = System.getProperty("user.home") + "/Downloads/scrape.final/" + story + "/";
		
		{ // import root episode
			//Session session = DB.session;
			DBUser rootEpUser = new DBUser();
			rootEpUser.setEmail(null);
			rootEpUser.setLevel((byte) 1);
			String id = genID();
			while (DB.session.get(DBUser.class, id) != null) id = genID();
			rootEpUser.setId(id);
			Strings.log("Loading root of " + story);
			InitEpisode rootCont = readEpisode(new File(dirPath + "root"));
			DBEpisode rootEp = rootCont.toDBEpisode();
			rootEp.setParent(null);
			rootEp.setId(rootID);
			rootEpUser.getEpisodes().add(rootEp);
			rootEp.setAuthor(rootEpUser);
			rootEpUser.setAuthor(rootCont.author);
			rootEpUser.setPassword("disabled");
			DB.session.save(rootEp);
			DB.session.save(rootEpUser);
			DB.session.getTransaction().commit();
		}
		
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
			if (map.containsKey(newId)) {
				Strings.log("Duplicate newId: " + newId + " " + oldId + " " + map.get(newId));
				//System.exit(1);
				passed = false;
			} else map.put(newId, oldId);
		}
		index.close();
		if (!passed) System.exit(1);
		
		Strings.log("Finding missing eps for " + story); 
		HashSet<String> missingEpisodes = new HashSet<>();
		// Figure out where stuff is missing from the scrape
		for (String id : map.keySet()) {
			String parentId = getParentId(id);
			if (parentId.length() > 0 && !map.containsKey(parentId)) {
				missingEpisodes.add(parentId);
			}
		}
		boolean noneMissing = false;
		while (noneMissing == false) {
			noneMissing = true;
			HashSet<String> newMissingEpisodess = new HashSet<>();
			for (String id : missingEpisodes) {
				String parentId = getParentId(id);
				if (parentId.length() > 0 && !missingEpisodes.contains(parentId) && !map.containsKey(parentId)) {
					newMissingEpisodess.add(parentId);
					noneMissing = false;
				}
			}
			missingEpisodes.addAll(newMissingEpisodess);
		}
		Strings.log("Done finding missing eps for " + story + ", building tree"); 
		
		TreeNode rootNode = buildTree(map, missingEpisodes, rootID);
		
		// this line exists to prevent warnings and should be deleted on further development
		System.out.println(rootNode.oldID + " " + rootNode.parent); 
		
	}
	
	private static TreeNode buildTree(TreeMap<String,String> map, HashSet<String> missing, String rootID) {
		HashMap<String, TreeNode> treeMap = new HashMap<>();
		TreeNode rootNode = new TreeNode();
		rootNode.oldID = "root";
		treeMap.put(rootID, rootNode);
		for (Entry<String,String> entry : map.entrySet()) {
			TreeNode parent = treeMap.get(getParentId(entry.getKey()));
			TreeNode child = new TreeNode();
			child.parent = parent;
			child.oldID = entry.getValue();
			parent.children.add(child);
		}
		return rootNode;
	}
	
	private static InitEpisode readEpisode(File f) {
		try (Scanner in = new Scanner(f)){
			InitEpisode ep = new InitEpisode();
			try {
				String oldID = in.nextLine(); // skip old id
				String newID = in.nextLine(); // skip new id
				ep.link = trimTo(in.nextLine(), 254);
				ep.title = trimTo(in.nextLine(), 254);
				ep.author = trimTo(in.nextLine(), 254);
				String dateString = in.nextLine().trim();
				try {
					ep.date = df.parse(dateString);
				} catch (ParseException e) {
					ep.date = badDate;
					Strings.log("Bad header: " + oldID + " " + newID + " " + f.getAbsolutePath());
				}
			} catch (NoSuchElementException e) {
				in.close();
				if (ep.title == null) ep.title ="(Empty)";
				if (ep.link == null) ep.link = "(Empty)";
				if (ep.author == null) ep.author = "(Empty)";
				if (ep.body == null) ep.body = "";
				if (ep.date == null) ep.date = badDate;
				Strings.log("(partially) empty episode: " + f.getName());
				return ep;
			}
			String line = in.nextLine();
			while (in.hasNext() && line.trim().length() == 0) line = in.nextLine();
			StringBuilder body = new StringBuilder();
			body.append(line + "  \n");
			while (in.hasNext()) body.append(in.nextLine() + "  \n");
			ep.body = body.toString().replace('`', '\'');
			in.close();
			return ep;
		} catch (FileNotFoundException e) {
			Strings.log("Error: file not found " + f.getAbsolutePath());
			throw new RuntimeException(e);
		} 
	}
	
	private static Random r = new Random();
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
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
	
	private static ArrayList<Character> idChars = new ArrayList<>();
	static {
		for (char c='a'; c<='z'; ++c) idChars.add(c);
		for (char c='A'; c<='Z'; ++c) idChars.add(c);
		for (char c='0'; c<='9'; ++c) idChars.add(c);
	}
	private static String genID() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<8; ++i) sb.append(idChars.get(r.nextInt(idChars.size())));
		return "legacy" + sb.toString();
	}
	
	private static String trimTo(String s, int l) {
		if (s.length() <= l) return s;
		else return s.substring(0, l);
	}
	
	/**
	 * @param s an ID
	 * @return parent ID, or "" if ID is root
	 */
	private static String getParentId(String s) {
		String[] arr = s.split("-");
		if (arr.length <= 1) return "";
		StringBuilder ret = new StringBuilder();
		for (int i=0; i<arr.length-1; ++i) ret.append(arr[i] + "-");
		return ret.substring(0, ret.length()-1);
	}
	
	private static class TreeNode {
		public String oldID = null;
		public TreeNode parent = null;
		public ArrayList<TreeNode> children = new ArrayList<>();
	}
	
	private static class InitEpisode {
		public String link = null;
		public String title = null;
		public String author = null;
		public Date date = null;
		public String body = null;
		public DBEpisode toDBEpisode() {
			DBEpisode ep = new DBEpisode();
			ep.setLink(link);
			ep.setTitle(title);
			ep.setDate(date);
			ep.setBody(body);
			return ep;
		}
	}
}
