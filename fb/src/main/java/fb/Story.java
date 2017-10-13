package fb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.google.common.html.HtmlEscapers;

import fb.db.DB;
import fb.db.DBEpisode;

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
	static String getHTML(String id, int sort) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		else {
			StringBuilder sb = new StringBuilder();
			ArrayList<DBEpisode> children = new ArrayList<>(ep.getChildren());
			switch (sort) {
			case 0:
			default:
				Collections.sort(children, Strings.keyComparator);
				break;
			case 1:
				Collections.sort(children, Strings.reverseKeyComparator);
				break;
			case 2:
				Collections.sort(children, Strings.childrenMostLeastComparator);
				break;
			case 3:
				Collections.sort(children, Strings.childrenLeastMostComparator);
				break;
			case 4:
				Collections.shuffle(children);
				break;
			}
			if (children != null) for (DBEpisode child : children) if (child != null && !child.getId().equals(ep.getId())){
				sb.append("<p><a href=" + child.getId() + ">" + child.getLink() + "</a>" + " (" + child.getChildren().size() + ")" + "</p>");
			}
			return Strings.storyDefault
					.replace("$TITLE", HtmlEscapers.htmlEscaper().escape(ep.getTitle()))
					.replace("$BODY", formatBody(HtmlEscapers.htmlEscaper().escape(ep.getBody())))
					.replace("$AUTHOR", HtmlEscapers.htmlEscaper().escape(ep.getAuthor()))
					.replace("$PARENTID", (ep.getParent() == null)?(""):(HtmlEscapers.htmlEscaper().escape(ep.getParent().getId())))
					.replace("$ID", id)
					.replace("$DATE", outputDate.format(ep.getDate()))
					.replace("$CHILDREN", sb.toString());
		}
	}
	
	private static final DateFormat outputDate = new SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss");
	
	
	/////////////////////////////////////// functions to add episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	static String addForm(String id) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		return Strings.formDefault
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
	static String addpost(String id, String link, String title, String body, String author) {
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
	 * Apply formatting to the body of an episode
	 * @param body unformatted body
	 * @return formatted body
	 */
	private static String formatBody(String body) {
		return body.replace("\n", "<br/>");
	}
	
	private static String notFound(String id) {
		return "<html><head><title>Fiction Branches</title></head><body><h1>Not found</h1>" + id + "</body></html>";
	}	
}
