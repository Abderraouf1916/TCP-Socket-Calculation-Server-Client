import java.io.*;
import java.net.*;
import java.util.Scanner;

public class CalcClient {

    public static void main(String[] args) {
        String host = "localhost"; 
        int port = 5000;
        Scanner sc = new Scanner(System.in);

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine()); 
            String name = sc.nextLine();
            out.println(name);

            System.out.println(in.readLine()); 

            while (true) {
                String serverMsg;
                while ((serverMsg = in.readLine()) != null && !serverMsg.startsWith("Enter OPERATION:")) {
                    System.out.println(serverMsg);
                }

                System.out.print("> ");
                String op = sc.nextLine();
                if (op.equalsIgnoreCase("EXIT")) {
                    out.println("EXIT");
                    System.out.println("Disconnected from server.");
                    break;
                }
                out.println(op);

                System.out.println(in.readLine()); 
                String arg1 = sc.nextLine();
                out.println(arg1);

                if (isBinary(op)) {
                    System.out.println(in.readLine()); 
                    String arg2 = sc.nextLine();
                    out.println(arg2);
                }

                String response = in.readLine();
                if (response.startsWith("RESULT:")) {
                    System.out.println("Result = " + response.substring(7));
                } else if (response.startsWith("ERROR:")) {
                    System.out.println("Server error = " + response.substring(6));
                } else {
                    System.out.println("Invalid response: " + response);
                }
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static boolean isBinary(String op) {
        return switch (op.toUpperCase()) {
            case "ADD","SUB","MUL","DIV","POW","MOD","MAX","MIN","AVERAGE" -> true;
            default -> false;
        };
    }
}
