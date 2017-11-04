package fb.db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="fbuserdb")
public class DBUser {
	
	public DBUser() {}
	
	@OneToOne
	private DBEmail email;
	
	@Id
	private String id;
		
	private String author;
	
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
