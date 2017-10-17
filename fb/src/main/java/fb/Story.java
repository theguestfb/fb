package fb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.mindrot.jbcrypt.BCrypt;

import com.google.common.html.HtmlEscapers;

import fb.db.DB;
import fb.db.DBEpisode;
import fb.db.DBRecents;

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
	public static String getHTML(String id, int sort) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		else {
			StringBuilder sb = new StringBuilder();
			ArrayList<DBEpisode> children = new ArrayList<>(ep.getChildren());
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
			if (children != null) for (DBEpisode child : children) if (child != null && !child.getId().equals(ep.getId())){
				sb.append("<p><a href=" + child.getId() + ">" + HtmlEscapers.htmlEscaper().escape(child.getLink()) + "</a>" + " (" + child.getChildren().size() + ")" + "</p>\n");
			}
			return Strings.storyDefault
					.replace("$TITLE", HtmlEscapers.htmlEscaper().escape(ep.getTitle()))
					.replace("$BODY", formatBody(ep.getBody()))
					.replace("$AUTHOR", HtmlEscapers.htmlEscaper().escape(ep.getAuthor()))
					.replace("$PARENTID", (((ep.getParent() == null) || (ep.getParent().getId().equals(ep.getId()))) ? ("..") : (HtmlEscapers.htmlEscaper().escape(ep.getParent().getId()))))
					.replace("$ID", id)
					.replace("$DATE", HtmlEscapers.htmlEscaper().escape(outputDate.format(ep.getDate())))
					.replace("$CHILDREN", sb.toString());
		}
	}
	
	private static final DateFormat outputDate = new SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss");
	
	
	/**
	 * Gets an list of recent episodes

	 * @return HTML recents
	 */
	public static String getRecents() {
		DBRecents recents = DB.getRecents();
		{
			StringBuilder sb = new StringBuilder();

			for (DBEpisode child : recents.getRecents()) if (child != null){
				String story = "";
				switch (child.getId().split("-")[0]) {
				case "1":
					story = "(Forum)";
					break;
				case "2":
					story = "(You Are What You Wish)";
					break;
				case "3":
					story = "(Altered Fates)";
					break;
				case "4":
					story = "(The Future of Gaming)";
					break;
				}
				sb.append("<p><a href=get/" + child.getId() + ">" + child.getLink() + "</a>" + " by " + child.getAuthor() + " on " + outputDate.format(child.getDate()) + " " + story + "</p>");
			}
			return Strings.recentsDefault.replace("$CHILDREN", sb.toString());
		}
	}
	
	
	/////////////////////////////////////// functions to add episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	public static String addForm(String id) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		return Strings.addFormDefault
				.replace("$TITLE", ep.getTitle())
				.replace("$ID", id);
	}
	
	/**
	 * Adds an episode to the story
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return HTML success page
	 */
	public static String addPost(String id, String link, String title, String body, String author) {
		link = link.trim();
		title = title.trim();
		body = body.trim();
		author = author.trim();
		
		String errors = checkEpisode(link, title, body, author);
		if (errors != null) return Strings.failureDefault.replace("$REASON", errors);
		
		DBEpisode child = DB.addEp(id, link, title, body, author, new Date());
		if (child == null) return Strings.failureDefault.replace("$REASON", "ERROR: unable to add episode (talk to Phoenix if you see this)");
				
		return Strings.successDefault.replace("$ID", child.getId() + "");	
	}
	
	/////////////////////////////////////// functions to modify episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	public static String modifyForm(String id) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		return Strings.modifyFormDefault
				.replace("$TITLE", HtmlEscapers.htmlEscaper().escape(ep.getTitle()))
				.replace("$BODY", HtmlEscapers.htmlEscaper().escape(ep.getBody()))
				.replace("$AUTHOR", HtmlEscapers.htmlEscaper().escape(ep.getAuthor()))
				.replace("$LINK", HtmlEscapers.htmlEscaper().escape(ep.getLink()))
				.replace("$ID", id);
	}
	
	/**
	 * Modifies an episode of the story
	 * @param id id of episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return HTML success page
	 */
	public static String modifyPost(String id, String link, String title, String body, String author, String password) {

		link = link.trim();
		title = title.trim();
		body = body.trim();
		author = author.trim();
		
		String errors = checkEpisode(link, title, body, author);
		if (errors != null) return Strings.failureDefault.replace("$REASON", errors);
		
		if (!BCrypt.checkpw(password, Strings.modifyPasswordhash)) return Strings.failureDefault.replace("$REASON", "Incorrect password");
		
		if (!DB.modifyEp(id, link, title, body, author)) {
			return Strings.failureDefault.replace("$REASON", "Not found: " + id);
		}
				
		return Strings.successDefault.replace("$ID", id + "");	
	}
	
	/////////////////////////////////////// utility functions \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	private static String checkEpisode(String link, String title, String body, String author) {
		StringBuilder errors = new StringBuilder();
		if (link.length() == 0) errors.append("Link text cannot be empty<br/>");
		if (title.length() == 0) errors.append("Title cannot be empty<br/>");
		if (body.length() == 0) errors.append("Body cannot be empty<br/>");
		if (author.length() == 0) errors.append("Author cannot be empty<br/>");
		
		if (link.length() > 255) errors.append("Link text cannot be longer than 255 (" + link.length() + ")<br/>");
		if (title.length() > 255) errors.append("Title cannot be longer than 255 (" + title.length() + ")<br/>");
		if (body.length() > 100000) errors.append("Body cannot be longer than 100000 (" + body.length() + ")<br/>");
		if (author.length() > 255) errors.append("Author cannot be longer than 255 (" + author.length() + ")<br/>");
		if (errors.length() > 0) return errors.toString();
		
		return (errors.length() > 0) ? errors.toString() : null;
	}
	

	
	/**
	 * Escape body text and convert markdown/formatting to HTML
	 * @param body unformatted markdown body
	 * @return HTML formatted body
	 */
	private static String formatBody(String body) {
		String ret = body;
		ret = HtmlEscapers.htmlEscaper().escape(ret);
		ret = HtmlRenderer.builder().build().render(Parser.builder().build().parse(ret));
		return ret;
	}
	
	private static String notFound(String id) {
		return "<html><head><title>Fiction Branches</title></head><body><h1>Not found</h1>" + id + "</body></html>";
	}	
}
