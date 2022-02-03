import java.io.IOException;

public class Main {

    /*
    args[0] (String) n == naive OR c == cached
    args[1] (int) number of servers
    args[2] (int) number of clients
     */

    public static void main(String[] args) {
        try {
            runScript(args[0], args[1], args[2]);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void runScript(String arg1, String arg2, String arg3) throws IOException {
        String[] cmd = { "bash", "-c", System.getProperty("user.dir") + "\\simulator.sh" + arg1 + " " + arg2 + " " + arg3 };
        Process p = Runtime.getRuntime().exec(cmd);
    }
}
