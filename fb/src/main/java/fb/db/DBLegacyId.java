package fb.db;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="legacyids")
public class DBLegacyId implements Serializable {
	/****/
	private static final long serialVersionUID = -3235868657047981276L;

	public DBLegacyId() {}
	
	@Id
	private String id;
	
	@Column( length = 4096 )
	private String newId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNewId() {
		return newId;
	}

	public void setNewId(String newId) {
		this.newId = newId;
	}

}
