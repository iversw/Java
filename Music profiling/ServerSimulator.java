public class ServerSimulator {

    public static void main(String[] args) {
        boolean naiveMode = false;

        for (String s : args) {
            if (s.equals("-n")) {
                naiveMode = true;
            }
        }

        try {
            for (int i = 2000; i < 2005; i++) {
                Server obj = new Server("Server" + i, i, naiveMode);
                obj.launch();
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e);
            e.printStackTrace();
        }
    }
}
