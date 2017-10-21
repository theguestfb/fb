package fb.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="legacyauthors")
public class DBLegacyAuthor {
	public DBLegacyAuthor() {}
	
	@Id
	@Column( length = 4096 )
	private String id; // episode id
	
	private String author;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

}
