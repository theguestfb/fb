package fb.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;


@Entity
@Table(name="fbepisodes")
public class DBEpisode {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long generatedId;
	
	
	@Column(columnDefinition = "text", unique=true)
	private String id;
			
	private String legacyId = null;
	
	private String title;
	
	private String link;
	
	private int depth;
	
	private int childCount;
	
	@ManyToOne
	private DBUser author;
	
	private Date date;
	
	@ManyToOne
	private DBUser editor;
	
	private Date editDate;
	
	@ManyToOne
	private DBEpisode parent;
	
	@OneToMany(mappedBy = "parent")
	private List<DBEpisode> children = new ArrayList<>();
	
	@OneToMany(mappedBy = "episode")
	private List<DBFlaggedEpisode> flags = new ArrayList<>();
	
	@Column(columnDefinition = "text")
	private String body;
	
	// The following constructor, getters, and setters are required for JPA persistence
	public DBEpisode() {} 
	
	public long getGeneratedId() {
		return generatedId;
	}

	public void setGeneratedId(long generatedId) {
		this.generatedId = generatedId;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getLegacyId() {
		return legacyId;
	}

	public void setLegacyId(String legacyId) {
		this.legacyId = legacyId;
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
	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getChildCount() {
		return childCount;
	}

	public void setChildCount(int childCount) {
		this.childCount = childCount;
	}

	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public DBUser getAuthor() {
		return author;
	}
	public void setAuthor(DBUser author) {
		this.author = author;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public DBUser getEditor() {
		return editor;
	}

	public void setEditor(DBUser editor) {
		this.editor = editor;
	}

	public Date getEditDate() {
		return editDate;
	}

	public void setEditDate(Date editDate) {
		this.editDate = editDate;
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
	
	public List<DBFlaggedEpisode> getFlags() {
		return flags;
	}

	public void setFlags(List<DBFlaggedEpisode> flags) {
		this.flags = flags;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id + ": " + title);
		sb.append(" [" + ((parent==null)?"root":parent.getTitle()) + "]");
		for (DBEpisode child : children) sb.append(" [" + child.getTitle() + "]");
		return sb.toString();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof DBEpisode)) return false;
		DBEpisode that = (DBEpisode)o;
		return this.id.equals(that.id);
	}
	
	public int hashCode() {
		return id.hashCode();
	}
}
