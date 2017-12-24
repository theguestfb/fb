package fb;

import static fb.util.Strings.escape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import javax.ws.rs.core.Cookie;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import fb.Accounts.FBLoginException;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.objects.Episode.ChildEpisode;
import fb.objects.User;
import fb.util.Comparators;
import fb.util.Strings;

/**
 * Contains the actual logic that controls how the site works
 */
public class Story {
	
	/////////////////////////////////////// function to get episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

	
	/**
	 * Gets an episode by its id, with children in sorted order
	 * Sort orders:
	 * 
	 * 0: oldest first (by keystring/submission date) DEFAULT
	 * 1: newest first (reverse keystring/submission date)
	 * 2: number of children (most to least)
	 * 3: number of children (least to most)
	 * 4: random shuffle
	 * 
	 * @param id id of episode
	 * @return HTML episode
	 */
	public static String getHTML(String id, int sort, Cookie token) {
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id);
		}
		StringBuilder sb = new StringBuilder();
		ArrayList<ChildEpisode> children = new ArrayList<>(Arrays.asList(ep.children));
		String sortOrder;
		switch (sort) {
		case 0:
		default:
			Collections.sort(children, Comparators.keyComparator);
			sortOrder = "Oldest first (default)";
			break;
		case 1:
			Collections.sort(children, Comparators.reverseKeyComparator);
			sortOrder = "Newest first";
			break;
		case 2:
			Collections.sort(children, Comparators.childrenMostLeastComparator);
			sortOrder = "Children (descending)";
			break;
		case 3:
			Collections.sort(children, Comparators.childrenLeastMostComparator);
			sortOrder = "Children (ascending)";
			break;
		case 4:
			Collections.shuffle(children);
			sortOrder = "Random";
			break;
		}
		for (ChildEpisode child : children) {
			sb.append("<p><a href='" + child.id + "' rel='child'>" + escape(child.link) + "</a>" + " (" + child.count + ")" + "</p>\n");
		}
		
		String addEp, modify="";			
			
		try {
			User currentUser = Accounts.getUser(token);
			if (ep.authorId.equals(currentUser.id)) modify = "<br/><br/><a href=/fb/modify/" + id + ">Modify your episode</a>";
			else if (currentUser.level >= ((byte)10)) modify = "<br/><br/><a href=/fb/modify/" + id + ">Modify as moderator</a>";
			else modify = "<br/><br/><a href=/fb/flag/" + id + ">Flag this episode</a>";
			addEp = "<a href=../add/" + id + ">Add a new episode</a>";
			modify += "<br/>\n<a href=/fb/path/" + id + ">Path to here</a>" + 
					"<br/>\n<a href=/fb/outline/" + id + ">Outline from here</a>" + 
					"<br/>\n<a href=/fb/complete/" + id + ">View story so far</a>";
		} catch (FBLoginException e) {
			addEp = "<a href=/fb/login>Log in</a> or <a href=/fb/createaccount>create an account</a> to add episodes";
		}
			
		if (Strings.READ_ONLY_MODE) addEp = "";
			
		String author = escape(ep.authorName);
		if (!ep.isLegacy) author = "<a href='/fb/user/" + ep.authorId + "' rel='author'>" + author + "</a>";
			
		String editHTML = "";
		if (!ep.date.equals(ep.editDate)) {
			editHTML = "<br/>\nEpisode last modified by <a href='/fb/user/" + ep.editorId + "' rel='editor'>" + escape(ep.editorName) + "</a> on " + escape(Strings.outputDateFormat(ep.editDate));
		}
			
		return Strings.getFile("story.html", token)
				.replace("$TITLE", escape(ep.title))
				.replace("$BODY", formatBody(ep.body))
				.replace("$AUTHOR", author)
				.replace("$PARENTID", (ep.parentId == null) ? ".." : escape(ep.parentId))
				.replace("$ID", id)
				.replace("$DATE", escape(Strings.outputDateFormat(ep.date)) + editHTML)
				.replace("$MODIFY", modify)
				.replace("$ADDEP", addEp)
				.replace("$SORTORDER", sortOrder)
				.replace("$CHILDREN", sb.toString());
	}
	
	
	/**
	 * Gets an list of recent episodes
	 * 
	 * @return HTML recents
	 */
	public static String getRecents(Cookie token, String numString, String rootId) {
		int root = -1;
		{ // Check rootId is actually a root Id
			if (rootId == null || rootId.length() == 0) root = 0;
			for (char c : rootId.toCharArray()) if (c<'0' || c>'9') root = 0;
			if (root == -1) try {
				root = Integer.parseInt(rootId);
			} catch (NumberFormatException e) {
				root = 0;
			}
		}
		int num;
		try { // Check that numString is a number
			num = Integer.parseInt(numString);
			if (num > 100) num = 100;
		} catch (NumberFormatException e) {
			num = 25;
		}
		Episode[] recents;
		try {
			recents = DB.getRecents(root, num);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", e.getMessage());
		}
				
		StringBuilder sb = new StringBuilder();
		for (Episode child : recents) if (child != null){
			String story;
			try {
				story = "(" + DB.getEp(child.id.split("-")[0]).link + ")";
			} catch (DBException e) {
				return Strings.getFile("generic.html", token).replace("$EXTRAS", "Recents appears to be broken (you should never see this), tell Phoenix you saw this");
			}
			sb.append("<p class='hentry'><a class='url entry-title' href='/fb/get/" + child.id + "'>" + escape(child.link) + "</a>" + " by <span='author'>" + escape(child.authorName) + "</span> on <time class='published'>" + Strings.outputDateFormat(child.date) + "</time> " + story + "</p>\n");
		}
		return Strings.getFile("recents.html", token).replace("$CHILDREN", sb.toString());
	}
	
	public static String getOutline(Cookie token, String rootId, String depthString) {
		
		int depth;
		try {
			depth = Integer.parseInt(depthString);
		} catch (NumberFormatException e) {
			depth = 50;
		}
		if (depth > 100) depth = 50;
		else if (depth < 1) depth = 1;
		Episode[] outline;
		try {
			if (DB.getEp(rootId) == null) return Strings.getFile("generic.html", token).replace("$EXTRA", "ID not found: " + rootId);
			outline = DB.getOutline(rootId, depth);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Recents is broken, you should never see this, tell Phoenix");
		}
		ArrayList<Episode> list = new ArrayList<>(Arrays.asList(outline));
		Collections.sort(list, Comparators.episodeKeyComparator);
		int minDepth = list.get(0).depth;
		StringBuilder sb = new StringBuilder();
		for (Episode child : list) if (child != null){
			//sb.append("<p>");
			for (int i=minDepth; i<child.depth; ++i) sb.append("&nbsp;");
			sb.append(child.depth + ". " + epLine(child));
		}
		return Strings.getFile("outline.html", token).replace("$ID", rootId).replace("$CHILDREN", sb.toString());
	}
	
	public static String epLine(Episode ep) {
		if (ep.isLegacy) return "<a href='/fb/get/" + ep.id + "'>" + escape(ep.link) + "</a> (" + escape(ep.authorName) + ")<br/>\n";
		else return "<a href='/fb/get/" + ep.id + "'>" + escape(ep.link) + "</a> (<a href='/fb/user/" + ep.authorId + "' class='author'>" + escape(ep.authorName) + "</a>)<br/>\n";
	}
	
	public static String getPath(Cookie token, String id) {
		Episode[] path;
		try {
			path = DB.getPath(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", e.getMessage());
		}
		ArrayList<Episode> list = new ArrayList<>(Arrays.asList(path));
		StringBuilder sb = new StringBuilder();
		for (Episode child : list) if (child != null){
			sb.append(child.depth + ". " + epLine(child));
		}
		return Strings.getFile("path.html", token).replace("$ID", id).replace("$CHILDREN", sb.toString());
	}
	
	public static String getCompleteHTML(Cookie token, String id) {
		Episode[] path;
		try {
			System.out.println("Getting episodes from DB");
			path = DB.getPath(id);
			System.out.println("Got episodes from DB");
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", e.getMessage());
		}
		StringBuilder sb = new StringBuilder();
		for (Episode child : path) if (child != null){ 
			sb.append(formatBody(child.body) + "<hr/>\n");
		}
		
		return Strings.getFile("completestory.html", token).replace("$TITLE", escape(path[0].title)).replace("$BODY", sb.toString());
	}
	
	/**
	 * Gets an list of recent episodes
	 * 
	 * @return HTML recents
	 */
	public static String getWelcome(Cookie token) {
		Episode[] roots;
		try {
			roots = DB.getRoots();
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Welcome is broken, you should never see this, tell Phoenix");
		}
				
		StringBuilder sb = new StringBuilder();
		for (Episode ep : roots) {
			sb.append("<h3><a href=/fb/get/" + ep.id + ">" + ep.link + "</a> (" + ep.count + ")</h3>" + "<a href=/fb/feed/" + ep.id + "><img width=20 height=20 src=/images/rss.png title=\"RSS feed for " + ep.link + "\" /></a>" + " <a href=/fb/recent/" + ep.id + ">" + ep.link + "'s recently added episodes</a> " + "<br/><br/>");
		}
		return Strings.getFile("welcome.html", token).replace("$EPISODES", sb.toString());
		
	}
	
	
	/////////////////////////////////////// functions to add episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	public static String addForm(String id, Cookie token) {
		if (!Accounts.isLoggedIn(token)) return Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to add episodes");
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e) {
			//return notFound(id);
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id);
		}
		return Strings.getFile("addform.html", token)
				.replace("$TITLE", ep.title)
				.replace("$ID", id);
	}
	
	/**
	 * Adds an episode to the story
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return ID of new episode
	 * @throws EpisodeException if there's any error, e.getMessage() will contain HTML page for error
	 */
	public static String addPost(String id, String link, String title, String body, Cookie token) throws EpisodeException {
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e) {
			throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to add episodes"));
		}
		link = link.trim();
		title = title.trim();
		body = body.trim();
		
		String errors = checkEpisode(link, title, body);
		if (errors != null) throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", errors));
		try {
			return DB.addEp(id, link, title, body, user.id, new Date()).id;
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", e.getMessage()));
		}
	}
	
	/**
	 * Returns the form for adding new root episodes, or error page if not admin
	 * @param id id of parent episode
	 * @return HTML form
	 */
	public static String newRootForm(Cookie token) {
		//if (!Accounts.isLoggedIn(token)) 
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e1) {
			return Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to add episodes");
		}
		if (user.level < 100) return Strings.getFile("generic.html",token).replace("$EXTRA", "Only admins can add new root episodes");

		return Strings.getFile("newrootform.html", token);
	}
	
	public static String newRootPost(String link, String title, String body, Cookie token) throws EpisodeException {
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e) {
			throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to add episodes"));
		}
		if (user.level < 100) return Strings.getFile("generic.html",token).replace("$EXTRA", "Only admins can add new root episodes");
		link = link.trim();
		title = title.trim();
		body = body.trim();
		
		String errors = checkEpisode(link, title, body);
		if (errors != null) throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", errors));
		try {
			return DB.addRootEp(link, title, body, user.id, new Date()).id;
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", e.getMessage()));
		}
	}
	
	public static class EpisodeException extends Exception {
		/** */
		private static final long serialVersionUID = 5020407245081273282L;
		public EpisodeException(String message) {
			super(message);
		}
	}
	
	/////////////////////////////////////// functions to modify episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	public static String modifyForm(String id, Cookie token) {
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id);
		}
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e) {
			return Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to do that");
		}
		if (!user.id.equals(ep.authorId) && user.level<10) return Strings.getFile("generic.html",token).replace("$EXTRA", "You can only edit episodes that you wrote");
		return Strings.getFile("modifyform.html", token)
				.replace("$TITLE", escape(ep.title))
				.replace("$BODY", escape(ep.body))
				.replace("$LINK", escape(ep.link))
				.replace("$ID", id);
	}
	
	public static String flagForm(String id, Cookie token) {
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id);
		}
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e) {
			return Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to do that");
		}
		if (user.id.equals(ep.authorId)) return Strings.getFile("generic.html",token).replace("$EXTRA", "You cannot flag your own episode.");
		return Strings.getFile("flagform.html", token)
				.replace("$TITLE", escape(ep.title))
				.replace("$ID", id);
	}
	
	public static void flagPost(String id, String flag, Cookie token) throws EpisodeException {
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e1) {
			//throw new EpisodeException(notFound(id));
			throw new EpisodeException(Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id));
		}
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e) {
			throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to do that"));
		}
		if (user.id.equals(ep.authorId)) throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "You cannot flag your own episode."));
		
		flag = flag.trim();
		
		if (flag.length() == 0) throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "Reason cannot be empty."));
		if (flag.length() > 100000) throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "Reason cannot be longer than 100000 (" + flag.length() + ")."));
		
		ArrayList<String> list = new ArrayList<>();
		for (String r : replacers) {
			if (flag.contains(r)) list.add(r);
		}
		if (list.size() > 0) throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "Flag text may not contain the following: " + list));
		
		try {
			DB.flagEp(id, user.id, flag);
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", e.getMessage()));
		}
		
	}
	
	/**
	 * Modifies an episode of the story
	 * @param id id of episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return id of modified episode
	 * @throws EpisodeException if error occurs, e.getMessage() will contain HTML page with error
	 */
	public static String modifyPost(String id, String link, String title, String body, Cookie token) throws EpisodeException {
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e1) {
			//throw new EpisodeException(notFound(id));
			throw new EpisodeException(Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id));
		}
		User user;
		try {
			user = Accounts.getUser(token);
		} catch (FBLoginException e) {
			throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "You must be logged in to do that"));
		}
		if (!user.id.equals(ep.authorId) && user.level<10) throw new EpisodeException(Strings.getFile("generic.html",token).replace("$EXTRA", "You can only edit episodes that you wrote"));
		
		link = link.trim();
		title = title.trim();
		body = body.trim();
		
		String errors = checkEpisode(link, title, body);
		if (errors != null) throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", errors));
				
		try {
			DB.modifyEp(id, link, title, body, user.id);
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$EXTRA", "Not found: " + id));
		}
				
		return id;	
	}
	
	/////////////////////////////////////// utility functions \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Checks that episode fields are non-empty within appropriate limits
	 * @param link
	 * @param title
	 * @param body
	 * @return
	 */
	private static String checkEpisode(String link, String title, String body) {
		StringBuilder errors = new StringBuilder();
		if (link.length() == 0) errors.append("Link text cannot be empty<br/>\n");
		if (title.length() == 0) errors.append("Title cannot be empty<br/>\n");
		if (body.length() == 0) errors.append("Body cannot be empty<br/>\n");
		
		if (link.length() > 255) errors.append("Link text cannot be longer than 255 (" + link.length() + ")<br/>\n");
		if (title.length() > 255) errors.append("Title cannot be longer than 255 (" + title.length() + ")<br/>\n");
		if (body.length() > 100000) errors.append("Body cannot be longer than 100000 (" + body.length() + ")<br/>\n");
				
		TreeSet<String> list = new TreeSet<>();
		for (String s : replacers) if (link.contains(s)) list.add(s);
		for (String s : replacers) if (title.contains(s)) list.add(s);
		for (String s : replacers) if (body.contains(s)) list.add(s);
		if (list.size() > 0) {
			errors.append("Link text, title, and body may not contain any of the following strings: ");
			for (String s : list) errors.append("\"" + s + "\"");
			errors.append("<br/>\n");
		}
		
		if (errors.length() > 0) return errors.toString();
		
		return (errors.length() > 0) ? errors.toString() : null;
	}
	public static final List<String> replacers = 
			Collections.unmodifiableList(new ArrayList<>(Arrays.asList("$SORTORDER","$ACCOUNT","$TITLE","$AUTHOR","$DATE","$MODIFY","$BODY","$CHILDREN","$ID","$PARENTID","$LINK","$EXTRA","$EPISODES","$STYLE")));
	
	/**
	 * Escape body text and convert markdown/formatting to HTML
	 * @param body unformatted markdown body
	 * @return HTML formatted body
	 */
	public static String formatBody(String body) {
		return renderer.render(parser.parse(escape(body)));
	}
	
	private static Parser parser;
	private static HtmlRenderer renderer;
	
	static {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(StrikethroughExtension.create(), AutolinkExtension.create()));
		options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
		options.set(Parser.FENCED_CODE_BLOCK_PARSER, false);
		options.set(Parser.INDENTED_CODE_BLOCK_PARSER, false);
		options.set(Parser.HTML_BLOCK_PARSER, false);
		options.set(Parser.BLOCK_QUOTE_PARSER, false);
				
		parser = Parser.builder(options).build();
		renderer = HtmlRenderer.builder(options).build();
	}

}
