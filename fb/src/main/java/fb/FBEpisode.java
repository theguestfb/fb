package fb;

import java.util.ArrayList;
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
public class FBEpisode {
	
	@Id
	@Column( length = 2048 )
	private String id;
		
	private String title;
	
	private String link;
	
	private String author;
	
	@ManyToOne
	private FBEpisode parent;
	
	@OneToMany(mappedBy = "parent")
	private List<FBEpisode> children = new ArrayList<>();
	
	@Lob
	private String body;
	
	// The following constructor, getters, and setters are required for JPA persistence
	public FBEpisode() {} 
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
	public FBEpisode getParent() {
		return parent;
	}
	public void setParent(FBEpisode parent) {
		this.parent = parent;
	}
	public List<FBEpisode> getChildren() {
		return children;
	}
	public void setChildren(List<FBEpisode> children) {
		this.children = children;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id + ": " + title);
		sb.append(" [" + ((parent==null)?"root":parent.getTitle()) + "]");
		for (FBEpisode child : children) sb.append(" [" + child.getTitle() + "]");
		return sb.toString();
	}
}
