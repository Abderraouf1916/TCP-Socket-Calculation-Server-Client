import java.io.*;
import java.net.*;
import java.util.Scanner;

public class CalcClient {

    public static void main(String[] args) {
        String host = "6.tcp.eu.ngrok.io";
        int port = 17070;
        Scanner sc = new Scanner(System.in);

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // read  name 
            System.out.println(in.readLine()); 
            String name = sc.nextLine();
            out.println(name); 

            // read server welcome message
            System.out.println(in.readLine()); 

            // calculation loop
            while (true) {
                System.out.print("Enter number 1 (or type EXIT to quit): ");
                String input1 = sc.nextLine();
                if (input1.equalsIgnoreCase("EXIT")) {
                    out.println("EXIT");
                    System.out.println("Disconnected from server.");
                    break;
                }

                System.out.print("Enter number 2: ");
                String input2 = sc.nextLine();

                System.out.print("Enter operator (+, -, *, /): ");
                String op = sc.nextLine();

                // send calculation protocol
                out.println("NUMBER:" + input1);
                out.println("NUMBER:" + input2);
                out.println("OPERATOR:" + op);

                // read server response
                String response = in.readLine();
                if (response == null) {
                    System.out.println("Server disconnected.");
                    break;
                }

                if (response.startsWith("RESULT:")) {
                    System.out.println("Server result = " + response.substring(7));
                } else if (response.startsWith("ERROR:")) {
                    System.out.println("Server error = " + response.substring(6));
                } else {
                    System.out.println("Invalid response from server: " + response);
                }
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
