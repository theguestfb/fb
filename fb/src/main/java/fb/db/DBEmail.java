package fb.db;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;


@Entity
@Table(name="fbemails")
public class DBEmail implements Serializable {
	
	/****/
	private static final long serialVersionUID = 2380721117543200537L;

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
