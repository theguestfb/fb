package fb;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.text.WordUtils;

import com.google.gson.Gson;

/**
 * Methods to handle api requests (kept here to keep FBAPI.java clean) along with some utility fuctions. 
 * 
 * Also contains hardcoded HTML strings, for now (TODO)
 */
public class Stuff {
	
	// Aforementioned hardcoded HTML
	private static final String storyDefault = readFile("story.html");
	private static final String formDefault = readFile("addform.html");
	private static final String successDefault = readFile("success.html");
	
	/**
	 * Gets an episode by its id
	 * @param id id of episode
	 * @return HTML episode
	 */
	static String getAPI(String id) {
		FBEpisode ep = DB.getEp(id);
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
		FBEpisode ep = DB.getEp(id);
		if (ep == null) return notFound(id);
		else {
			StringBuilder sb = new StringBuilder();
			List<FBEpisode> children = ep.getChildren();
			if (children != null) for (FBEpisode child : children) if (child != null){
				sb.append("<p><a href=" + child.getId() + ">" + child.getLink() + "</a></p>");
			}
			return storyDefault
					.replace("$TITLE", ep.getTitle())
					.replace("$BODY", formatBody(ep.getBody()))
					.replace("$AUTHOR", ep.getAuthor())
					.replace("$PARENTID", (ep.getParent() == null)?(""):(ep.getParent().getId()))
					.replace("$ID", id)
					.replace("$CHILDREN", sb.toString());
		}
	}

	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form
	 */
	static String addForm(String id) {
		FBEpisode ep = DB.getEp(id);
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
		FBEpisode child = DB.addEp(jsonChild.getId(), jsonChild.getTitle(), jsonChild.getBody(), jsonChild.getAuthor());
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
	static String addpost(String id, String title, String body, String author) {
		FBEpisode child = DB.addEp(id, title, body, author);
		if (child == null) return null;
		return successDefault.replace("$ID", child.getId() + "");	
	}
	
	static String readFile(String path) {
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
		/*StringBuilder ret = new StringBuilder();
		StringBuilder line = new StringBuilder();
		for (char c : body.toCharArray()) {
			if (c=='\n') {
				ret.append(WordUtils.wrap(line.toString(), 200, "<br/>", false) + "<br/>");
				line = new StringBuilder();
			} else line.append(c);
		}
		ret.append(line.toString());
		return ret.toString();*/
		return body.replace("\n", "<br/>");
	}
	
	private static String notFound(String id) {
		return "<html><head><title>Fiction Branches</title></head><body><h1>Not found</h1>" + id + "</body></html>";
	}	
}
