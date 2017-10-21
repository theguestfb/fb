package fb.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="fbemaildb")
public class DBEmail {
	
	public DBEmail() {}
	
	@Id
	@Column( length = 1024 )
	private String email;
	
	@OneToOne(mappedBy = "email")
	private DBUser user;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public DBUser getUser() {
		return user;
	}

	public void setUser(DBUser user) {
		this.user = user;
	}
}
