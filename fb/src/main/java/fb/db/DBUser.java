package fb.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="fbusers")
public class DBUser implements Serializable {
	
	/****/
	private static final long serialVersionUID = -3123455003291956376L;

	public DBUser() {}
	
	@OneToOne
	private DBEmail email;
	
	@Id
	private String id;
		
	private String author;
	
	@Column(columnDefinition = "text")
	private String bio;
	
	private String password;
	
	private byte level; // 1=user, 10=mod, 100=admin, 
	
	private String theme = "Default";
	
	@OneToMany(mappedBy = "author")
	private List<DBEpisode> episodes = new ArrayList<>();
	
	public DBEmail getEmail() {
		return email;
	}

	public void setEmail(DBEmail email) {
		this.email = email;
	}

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

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<DBEpisode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<DBEpisode> episodes) {
		this.episodes = episodes;
	}

	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}
	
}
