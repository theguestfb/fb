package fb;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.common.html.HtmlEscapers;

import fb.db.DB;
import fb.db.DBEpisode;
import fb.json.JsonEpisode;
import fb.json.JsonID;

/**
 * Contains the actual logic that controls how the site works
 */
public class Story {
	
	private static final String storyDefault = readFile("story.html");
	private static final String formDefault = readFile("addform.html");
	private static final String successDefault = readFile("success.html");
	private static final String failureDefault = readFile("failure.html");
	
	/////////////////////////////////////// functions to get episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Gets an episode by its id
	 * @param id id of episode
	 * @return HTML episode
	 */
	static String getAPI(String id) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return null;
		String[] childIDs = new String[ep.getChildren().size()];
		for (int i=0; i<childIDs.length; ++i) childIDs[i] = ep.getChildren().get(i).getId();
		return new Gson().toJson(new JsonEpisode(ep, (ep.getParent()==null)?null:ep.getParent().getId(), childIDs));
	}
	
	/**
	 * Gets an episode by its id
	 * @param id id of episode
	 * @return HTML episode
	 */
	static String getHTML(String id) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		else {
			StringBuilder sb = new StringBuilder();
			List<DBEpisode> children = ep.getChildren();
			if (children != null) for (DBEpisode child : children) if (child != null){
				sb.append("<p><a href=" + child.getId() + ">" + child.getLink() + "</a></p>");
			}
			return storyDefault
					.replace("$TITLE", HtmlEscapers.htmlEscaper().escape(ep.getTitle()))
					.replace("$BODY", formatBody(HtmlEscapers.htmlEscaper().escape(ep.getBody())))
					.replace("$AUTHOR", HtmlEscapers.htmlEscaper().escape(ep.getAuthor()))
					.replace("$PARENTID", (ep.getParent() == null)?(""):(HtmlEscapers.htmlEscaper().escape(ep.getParent().getId())))
					.replace("$ID", id)
					.replace("$CHILDREN", sb.toString());
		}
	}
	
	
	/////////////////////////////////////// functions to add episodes \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	static String addForm(String id) {
		DBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		return formDefault
				.replace("$TITLE", ep.getTitle())
				.replace("$ID", id);
	}
	
	/**
	 * Adds an episode to the story
	 * @param ep JsonEpisode containing new episode data
	 * @return
	 */
	static String addAPI(String ep) {
		JsonEpisode jsonChild = new Gson().fromJson(ep, JsonEpisode.class);
		
		String errors = checkEpisode(jsonChild.getLink(), jsonChild.getTitle(), jsonChild.getBody(), jsonChild.getAuthor());
		if (errors != null) return errors;
		
		DBEpisode child = DB.addEp(jsonChild.getId(), jsonChild.getLink(), jsonChild.getTitle(), jsonChild.getBody(), jsonChild.getAuthor());
		if (child == null) return null;
		return new Gson().toJson(new JsonID(child.getId()));	
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
		if (errors != null) return failureDefault.replace("$REASON", errors);
		
		DBEpisode child = DB.addEp(id, link, title, body, author);
		if (child == null) return failureDefault.replace("$REASON", "ERROR: unable to add episode (talk to Phoenix if you see this)");
		
		return successDefault.replace("$ID", child.getId() + "");	
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
	
	public static String readFile(String path) {
		try {
			StringBuilder sb = new StringBuilder();
			Scanner f = new Scanner(new File("/opt/fb/static_snippets/" + path));
			while (f.hasNextLine()) sb.append(f.nextLine() + "\n");
			f.close();
			return sb.toString();
		} catch (IOException e) {
			return "Not found";
		}
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
