public class User {
    private String username;
    private String token;

    public User(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public boolean validate(String username, String token) {
        if (this.username.equals(username) && this.token.equals(token)) return true;
        return false;
    }
}
