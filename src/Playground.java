import java.util.ArrayList;

public class Playground {
    public static void main(String[] args) {
        ArrayList<String> test = new ArrayList<>();
        test.add("eins");
        String test2 = "zwei";
        test.add(test2);
        System.out.println(test.contains("eins"));
        System.out.println(test.contains("zwei"));
    }
}
