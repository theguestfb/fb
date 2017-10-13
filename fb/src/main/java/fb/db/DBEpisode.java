package fb.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;


@Entity
@Table(name="storyfb")
public class DBEpisode {
	
	@Id
	@Column( length = 4096 )
	private String id;
		
	private String title;
	
	private String link;
	
	private String author;
	
	private Date date;
	
	@ManyToOne
	private DBEpisode parent;
	
	@OneToMany(mappedBy = "parent")
	private List<DBEpisode> children = new ArrayList<>();
	
	@Lob
	private String body;
	
	// The following constructor, getters, and setters are required for JPA persistence
	public DBEpisode() {} 
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
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public DBEpisode getParent() {
		return parent;
	}
	public void setParent(DBEpisode parent) {
		this.parent = parent;
	}
	public List<DBEpisode> getChildren() {
		return children;
	}
	public void setChildren(List<DBEpisode> children) {
		this.children = children;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id + ": " + title);
		sb.append(" [" + ((parent==null)?"root":parent.getTitle()) + "]");
		for (DBEpisode child : children) sb.append(" [" + child.getTitle() + "]");
		return sb.toString();
	}
}
