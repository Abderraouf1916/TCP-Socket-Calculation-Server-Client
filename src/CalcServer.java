import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CalcServer {

    public static void main(String[] args) {
        int PORT = 5000;
        System.out.println("Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("connect.log", "Client connected: " + clientSocket.getInetAddress());

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    //  LOG FUNCTION 
    public static void log(String filename, String msg) {
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write("[" + timestamp() + "] " + msg + "\n");
        } catch (IOException ignored) {}
    }

    public static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}



class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String clientName = "Unknown";

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // ask client for name
            out.println("Enter your name:");
            clientName = in.readLine();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Unknown";
            }

            CalcServer.log("connect.log", "Client connected: " + clientName + " (" + socket.getInetAddress() + ")");
            out.println("Welcome, " + clientName + "! Send calculations or type EXIT to quit.");

            String line;
            while (true) {
                line = in.readLine();
                if (line == null || line.equalsIgnoreCase("EXIT")) {
                    out.println("Goodbye, " + clientName + "!");
                    CalcServer.log("connect.log", "Client disconnected: " + clientName + " (" + socket.getInetAddress() + ")");
                    break;
                }

                String line1 = line;
                String line2 = in.readLine();
                String line3 = in.readLine();

                double n1, n2;
                String op;

                try {
                    n1 = parseNumber(line1, out);
                    n2 = parseNumber(line2, out);
                    op = parseOperator(line3, out);
                } catch (RuntimeException e) {
                    continue;
                }

                double result;
                try {
                    result = compute(n1, n2, op);
                } catch (ArithmeticException e) {
                    out.println("ERROR: Division by zero");
                    CalcServer.log("error.log", "Division by zero from client " + clientName + " (" + socket.getInetAddress() + ")");
                    continue;
                }

                out.println("RESULT:" + result);
                CalcServer.log("operations.log", clientName + " (" + socket.getInetAddress() + "): " + n1 + " " + op + " " + n2 + " = " + result);
            }

        } catch (IOException e) {
            CalcServer.log("error.log", "Client error (" + clientName + "): " + e.getMessage());
        }
    }

    private double parseNumber(String line, PrintWriter out) {
        try {
            String[] parts = line.split(":");
            if (parts.length < 2) throw new Exception();
            return Double.parseDouble(parts[1].trim());
        } catch (Exception e) {
            out.println("ERROR: Invalid number");
            throw new RuntimeException("Invalid number");
        }
    }

    private String parseOperator(String line, PrintWriter out) {
        try {
            String[] parts = line.split(":");
            if (parts.length < 2) throw new Exception();
            String op = parts[1].trim();
            if (!op.matches("[+\\-*/]")) throw new Exception();
            return op;
        } catch (Exception e) {
            out.println("ERROR: Invalid operator");
            throw new RuntimeException("Invalid operator");
        }
    }

    private double compute(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException();
                return a / b;
            default: throw new RuntimeException("Unknown operator");
        }
    }
}
