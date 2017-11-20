package fb.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="rootepisodes")
public class DBRootEpisodes implements Serializable {
	/****/
	private static final long serialVersionUID = 826937549665985189L;

	public DBRootEpisodes() {}
	
	@Id
	private int id;
	
	@OneToMany
	private List<DBEpisode> roots = new ArrayList<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<DBEpisode> getRoots() {
		return roots;
	}

	public void setRoots(List<DBEpisode> roots) {
		this.roots = roots;
	}
}
