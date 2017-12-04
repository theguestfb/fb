package fb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;

import javax.ws.rs.core.Cookie;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.node.Block;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.google.common.collect.ImmutableSet;
import com.google.common.html.HtmlEscapers;

import fb.Accounts.FBLoginException;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.objects.Episode.ChildEpisode;
import fb.objects.EpisodeList;
import fb.objects.User;

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
	public static String getHTML(String id, int sort, String settings, Cookie token) {
		
		int set;
		try {
			set = Integer.parseInt(settings.trim());
			if (set < 0 || set > 127) return Strings.getFile("generic.html", token).replace("$EXTRA", "Invalid settings " + settings);
		} catch (NumberFormatException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Invalid settings " + settings);
		}
		
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e) {
			//return notFound(id);
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Not found: " + id);
		}
			StringBuilder sb = new StringBuilder();
			ArrayList<ChildEpisode> children = ep.children;
			switch (sort) {
			case 0:
			default:
				Collections.sort(children, Comparators.keyComparator);
				break;
			case 1:
				Collections.sort(children, Comparators.reverseKeyComparator);
				break;
			case 2:
				Collections.sort(children, Comparators.childrenMostLeastComparator);
				break;
			case 3:
				Collections.sort(children, Comparators.childrenLeastMostComparator);
				break;
			case 4:
				Collections.shuffle(children);
				break;
			}
			for (ChildEpisode child : children) {
				sb.append("<p><a href=" + child.id + ">" + HtmlEscapers.htmlEscaper().escape(child.link) + "</a>" + " (" + child.count + ")" + "</p>\n");
			}
			
			String addEp, modify="";			
			
			try {
				User currentUser = Accounts.getUser(token);
				if (ep.authorId.equals(currentUser.id)) modify = "<br/><br/><a href=../modify/" + id + ">Modify your episode</a>";
				else if (currentUser.level >= ((byte)10)) modify = "<br/><br/><a href=../modify/" + id + ">Modify as moderator</a>";
				addEp = "<a href=../add/" + id + ">Add a new episode</a>";
			} catch (FBLoginException e) {
				addEp = "<a href=/fb/login>Log in</a> or <a href=/fb/createaccount>create an account</a> to add episodes";
			}
			
			String author = HtmlEscapers.htmlEscaper().escape(ep.authorName);
			if (!ep.isLegacy) author = "<a href=/fb/user/" + ep.authorId + ">" + author + "</a>";
			
			String editHTML = "";
			if (!ep.date.equals(ep.editDate)) {
				editHTML = "<br/>\nEpisode last modified by <a href=/fb/user/" + ep.editorId + ">" + HtmlEscapers.htmlEscaper().escape(ep.editorName) + "</a> on " + HtmlEscapers.htmlEscaper().escape(Strings.outputDateFormat(ep.editDate));
			}
			
			return Strings.getFile("story.html", token)
					.replace("$TITLE", HtmlEscapers.htmlEscaper().escape(ep.title))
					.replace("$BODY", formatBody(ep.body, set))
					.replace("$AUTHOR", author)
					.replace("$PARENTID", (ep.parentId == null) ? ".." : HtmlEscapers.htmlEscaper().escape(ep.parentId))
					.replace("$ID", id)
					.replace("$DATE", HtmlEscapers.htmlEscaper().escape(Strings.outputDateFormat(ep.date)) + editHTML)
					.replace("$MODIFY", modify)
					.replace("$ADDEP", addEp)
					.replace("$CHILDREN", sb.toString());
	}
	
	
	
	/**
	 * Gets an list of recent episodes
	 * 
	 * @return HTML recents
	 */
	public static String getRecents(Cookie token, String daysString) {
		int days;
		try {
			days = Integer.parseInt(daysString);
		} catch (NumberFormatException e) {
			days = 25;
		}
		EpisodeList recents;
		try {
			recents = DB.getRecents(days);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Recents is broken, you should never see this, tell Phoenix");
		}
				
		StringBuilder sb = new StringBuilder();
		for (Episode child : recents.episodes) if (child != null){
			String story;
			try {
				story = "(" + DB.getEp(child.id.split("-")[0]).link + ")";
			} catch (DBException e) {
				return Strings.getFile("generic.html", token).replace("$EXTRAS", "Recents appears to be broken (you should never see this), tell Phoenix you saw this");
			}
			sb.append("<p><a href=get/" + child.id + ">" + HtmlEscapers.htmlEscaper().escape(child.link) + "</a>" + " by " + HtmlEscapers.htmlEscaper().escape(child.authorName) + " on " + Strings.outputDateFormat(child.date) + " " + story + "</p>\n");
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
		EpisodeList outline;
		try {
			if (DB.getEp(rootId) == null) return Strings.getFile("generic.html", token).replace("$EXTRA", "ID not found: " + rootId);
			outline = DB.getOutline(rootId, depth);
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Recents is broken, you should never see this, tell Phoenix");
		}
		ArrayList<Episode> list = outline.episodes;
		Collections.sort(list, Comparators.episodeKeyComparator);
		int minDepth = list.get(0).depth;
		StringBuilder sb = new StringBuilder();
		for (Episode child : list) if (child != null){
			//sb.append("<p>");
			for (int i=minDepth; i<child.depth; ++i) sb.append("&nbsp;");
			sb.append(child.depth + ". <a href=/fb/get/" + child.id + ">" + HtmlEscapers.htmlEscaper().escape(child.link) + "</a><br />\n");
		}
		return Strings.getFile("outline.html", token).replace("$ID", rootId).replace("$CHILDREN", sb.toString());
	}
	
	/**
	 * Gets an list of recent episodes
	 * 
	 * @return HTML recents
	 */
	public static String getWelcome(Cookie token) {
		EpisodeList roots;
		try {
			roots = DB.getRoots();
		} catch (DBException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "Recents is broken, you should never see this, tell Phoenix");
		}
				
		StringBuilder sb = new StringBuilder();
		for (Episode ep : roots.episodes) {
			sb.append("<h3><a href=/fb/get/" + ep.id + ">" + ep.link + "</a> (" + ep.children.size() + ")" + "</h3>");
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
		if (errors != null) throw new EpisodeException(Strings.getFile("failure.html", token).replace("$REASON", errors));
		try {
			return DB.addEp(id, link, title, body, user.id, new Date()).id;
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$REASON", e.getMessage()));
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
		if (errors != null) throw new EpisodeException(Strings.getFile("failure.html", token).replace("$REASON", errors));
		try {
			return DB.addRootEp(link, title, body, user.id, new Date()).id;
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$REASON", e.getMessage()));
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
			//return notFound(id);
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
				.replace("$TITLE", HtmlEscapers.htmlEscaper().escape(ep.title))
				.replace("$BODY", HtmlEscapers.htmlEscaper().escape(ep.body))
				.replace("$LINK", HtmlEscapers.htmlEscaper().escape(ep.link))
				.replace("$ID", id);
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
		if (errors != null) throw new EpisodeException(Strings.getFile("failure.html", token).replace("$REASON", errors));
				
		try {
			DB.modifyEp(id, link, title, body, user.id);
		} catch (DBException e) {
			throw new EpisodeException(Strings.getFile("failure.html", token).replace("$REASON", "Not found: " + id));
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
	public static final ImmutableSet<String> replacers = 
			ImmutableSet.copyOf(new String[]{"$ACCOUNT","$TITLE","$AUTHOR","$DATE","$MODIFY","$BODY","$CHILDREN","$ID","$PARENTID","$LINK","$EXTRA","$REASON","$EPISODES","$STYLE"});
	
	/**
	 * Escape body text and convert markdown/formatting to HTML
	 * @param body unformatted markdown body
	 * @return HTML formatted body
	 */
	public static String formatBody(String body, int settings) {
		String ret = body;
		ret = HtmlEscapers.htmlEscaper().escape(ret);
		
		HashSet<Class<? extends Block>> enabledBlockTypes = new HashSet<>();
		
		if (settings >= 64) {
			enabledBlockTypes.add(HtmlBlock.class);
			settings -= 64;
		}
		
		if (settings >= 32) {
			enabledBlockTypes.add(FencedCodeBlock.class);
			settings -= 32;
		}
		
		if (settings >= 16) {
			enabledBlockTypes.add(IndentedCodeBlock.class);
			settings -= 16;
		}
		
		if (settings >= 8) {
			enabledBlockTypes.add(ListBlock.class);
			settings -= 8;
		}
		
		if (settings >= 4) {
			enabledBlockTypes.add(BlockQuote.class);
			settings -= 4;
		}
		
		if (settings >= 2) {
			enabledBlockTypes.add(ThematicBreak.class);
			settings -= 2;
		}
		
		if (settings >= 1) {
			enabledBlockTypes.add(Heading.class);
			settings -= 1;
		}
		
		//ret = renderer.render(parser.parse(ret));
		ret = renderer.render(Parser.builder().enabledBlockTypes(enabledBlockTypes).extensions(extensions).build().parse(ret));
		return ret;
	}
	
	//private static Parser parser;
	private static HtmlRenderer renderer;
	
	private static HashSet<Extension> extensions = new HashSet<>();
	
	static {
		//HashSet<Class<? extends Block>> enabledBlockTypes = new HashSet<>();
		//HashSet<Extension> extensions = new HashSet<>();

		//enabledBlockTypes.add(Heading.class);
		//enabledBlockTypes.add(HtmlBlock.class);
		//enabledBlockTypes.add(ThematicBreak.class);
		//enabledBlockTypes.add(FencedCodeBlock.class);
		//enabledBlockTypes.add(IndentedCodeBlock.class);
		//enabledBlockTypes.add(BlockQuote.class);
		//enabledBlockTypes.add(ListBlock.class);
		
		extensions.add(StrikethroughExtension.create());
		extensions.add(AutolinkExtension.create());
		
		//parser = Parser.builder().enabledBlockTypes(enabledBlockTypes).extensions(extensions).build();
		renderer = HtmlRenderer.builder().extensions(extensions).build();
	}

}
