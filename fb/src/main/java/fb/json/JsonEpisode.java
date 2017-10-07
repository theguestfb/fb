package fb.json;

import fb.db.DBEpisode;

public class JsonEpisode {

	private String id;
	private String title;
	private String link;
	private String author;
	private String body;
	private String parentID;
	private String[] childIDs;
	public JsonEpisode() {} 
	public JsonEpisode(DBEpisode ep, String parentID, String[] childIDs) {
		this.id = ep.getId();
		this.title = ep.getTitle();
		this.link = ep.getLink();
		this.author = ep.getAuthor();
		this.body = ep.getBody();
		this.parentID = parentID;
		this.childIDs = childIDs;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getParentID() {
		return parentID;
	}
	public void setParentID(String parentID) {
		this.parentID = parentID;
	}
	public String[] getChildIDs() {
		return childIDs;
	}
	public void setChildIDs(String[] childIDs) {
		this.childIDs = childIDs;
	}
}
