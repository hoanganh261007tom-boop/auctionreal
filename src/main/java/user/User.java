package user;
import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    protected String username;
    protected String role;
    protected String password;
    public User(String id, String username,String password, String role){
    this.id = id;
    this.username = username;
    this.password = password;
    this.role = role;
    }
    public String getUsername(){return username;}
    public String getId(){return id;}
    public String getRole(){return role;}
    public String getPassword(){return password;}
    @Override
    public String toString(){return String.format("[%s] %s (ID: %s)", role, username, id);}
}
