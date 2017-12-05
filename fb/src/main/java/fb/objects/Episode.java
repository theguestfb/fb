package fb.objects;

import java.util.ArrayList;
import java.util.Date;

import fb.db.DBEpisode;

/**
 * Immutable episode object
 */
public class Episode {
	public final String id;
	public final String title;
	public final String link;
	public final String authorId;
	public final String authorName;
	public final boolean isLegacy;
	public final String body;
	public final Date date;
	public final Date editDate;
	public final String editorId;
	public final String editorName;
	public final int count;
	public final int depth;
	public final ChildEpisode[] children;
	public final String parentId;
	
	/**
	 * Construct a complete Episode from a DBEpisode database object
	 * @param ep
	 */
	public Episode(DBEpisode ep) {
		this.id = ep.getId();
		this.title = ep.getTitle();
		this.link = ep.getLink();
		this.authorId = ep.getAuthor().getId();
		this.authorName = ep.getAuthor().getAuthor();
		this.body = ep.getBody();
		this.date = ep.getDate();
		this.editDate = ep.getEditDate();
		this.editorId = ep.getEditor().getId();
		this.editorName = ep.getEditor().getAuthor();
		this.isLegacy = ep.getAuthor().getEmail() == null;
		this.depth = ep.getDepth();
		this.count = ep.getChildCount();
		ArrayList<ChildEpisode> childList = new ArrayList<>();
		for (DBEpisode child : ep.getChildren()) childList.add(new ChildEpisode(child));
		ChildEpisode[] arr = new ChildEpisode[childList.size()];
		this.children = childList.toArray(arr);
		this.parentId = (ep.getParent() == null) ? null : ep.getParent().getId();
	}
	
	/**
	 * Construct an Episode from specific values (returns an *incomplete* Episode, not all values are available parameters)
	 * @param id
	 * @param link
	 * @param authorName
	 * @param date
	 * @param depth
	 */
	public Episode(String id, String link, String authorName, Date date, int depth) {
		this.id = id;
		this.title = "";
		this.link = link;
		this.authorId = "";
		this.authorName = authorName;
		this.isLegacy = true;
		this.body = "";
		this.date = date;
		//this.children = new ArrayList<>();
		this.children = new ChildEpisode[0];
		this.parentId = "";
		this.depth = depth;
		this.editDate = null;
		this.editorId = "";
		this.editorName = "";
		this.count = 0;
	}
	
	/**
	 * Immutable child episode
	 */
	public static class ChildEpisode {
		public final String id;
		public final String link;
		public final int count;
		/**
		 * Construct a child episode from a DBEpisode database object
		 * @param ep
		 */
		public ChildEpisode(DBEpisode ep) {
			this.id = ep.getId();
			this.link = ep.getLink();
			//this.count = ep.getChildren().size();
			this.count = ep.getChildCount();
		}
	}
}
