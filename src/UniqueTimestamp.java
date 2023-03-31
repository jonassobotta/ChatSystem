import java.io.Serializable;

public class UniqueTimestamp implements Comparable<UniqueTimestamp> , Serializable {
    public String user;
    public long timestamp;

    public UniqueTimestamp(String user, long timestamp) {
        this.user = user;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(UniqueTimestamp o) {
        int compare = user.compareTo(o.user);
        if (compare == 0) {
            compare = Long.compare(timestamp, o.timestamp);
        }
        return compare;
    }
}