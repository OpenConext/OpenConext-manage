package mr.format;

import org.junit.Test;

public class EduGainFeedParser {

    @Test
    public void testSwitch() {
        String s = "5";
        switch (s) {
            default:
                System.out.println("default");
            case "1":
                System.out.println("Yes");
                break;
            case "2":
                System.out.println("No");
                break;

        }
    }

}