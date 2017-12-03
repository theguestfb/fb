package fb.objects;

import java.util.ArrayList;
import java.util.Date;

import fb.db.DBEpisode;

public class Episode {
	public final String id;
	public final String title;
	public final String link;
	public final String authorId;
	public final String authorName;
	public final boolean isLegacy;
	public final String body;
	public final Date date;
	public final int depth;
	public final ArrayList<ChildEpisode> children;
	public final String parentId;
	public Episode(DBEpisode ep) {
		this.id = ep.getId();
		this.title = ep.getTitle();
		this.link = ep.getLink();
		this.authorId = ep.getAuthor().getId();
		this.authorName = ep.getAuthor().getAuthor();
		this.body = ep.getBody();
		this.date = ep.getDate();
		this.isLegacy = ep.getAuthor().getEmail() == null;
		this.depth = ep.getDepth();
		this.children = new ArrayList<>();
		for (DBEpisode child : ep.getChildren()) children.add(new ChildEpisode(child));
		this.parentId = (ep.getParent() == null) ? null : ep.getParent().getId();
	}
	public Episode(String id, String link, String authorName, Date date, int depth) {
		this.id = id;
		this.title = "";
		this.link = link;
		this.authorId = "";
		this.authorName = authorName;
		this.isLegacy = true;
		this.body = "";
		this.date = date;
		this.children = new ArrayList<>();
		this.parentId = "";
		this.depth = depth;
	}
	public static class ChildEpisode {
		public final String id;
		public final String link;
		public final int count;
		public ChildEpisode(DBEpisode ep) {
			this.id = ep.getId();
			this.link = ep.getLink();
			//this.count = ep.getChildren().size();
			this.count = ep.getChildCount();
		}
	}
}
