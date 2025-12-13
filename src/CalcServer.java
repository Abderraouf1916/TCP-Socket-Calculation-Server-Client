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
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    //  lOG FUNCTION 
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

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        String clientName = "Unknown";

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // ask client for name
            out.println("Enter your name:");
            clientName = in.readLine();
            if (clientName == null || clientName.trim().isEmpty()) clientName = "Unknown";
            out.println("Welcome, " + clientName + "! Choose operations or type EXIT to quit.");

            String line;
            while (true) {
                out.println("\nAvailable operations:");
                out.println("Binary: ADD, SUB, MUL, DIV, POW, MOD, MAX, MIN, AVERAGE");
                out.println("Unary: SQRT, ABS, FACT, SIN, COS, TAN");
                out.println("Enter OPERATION:");
                line = in.readLine();

                if (line == null || line.equalsIgnoreCase("EXIT")) {
                    out.println("Goodbye, " + clientName + "!");
                    CalcServer.log("connect.log", "Client disconnected: " + clientName + " (" + socket.getInetAddress() + ")");
                    break;
                }

                String op = line.trim().toUpperCase();
                out.println("Enter ARG1:");
                String arg1Line = in.readLine();
                Double arg1 = parseDouble(arg1Line, out);
                Double arg2 = null;

                if (isBinary(op)) {
                    out.println("Enter ARG2:");
                    String arg2Line = in.readLine();
                    arg2 = parseDouble(arg2Line, out);
                }

                try {
                    double result = compute(op, arg1, arg2);
                    out.println("RESULT:" + result);
                    CalcServer.log("operations.log", clientName + " (" + socket.getInetAddress() + "): " +
                            op + " " + arg1 + ((arg2 != null) ? ", " + arg2 : "") + " = " + result);
                } catch (ArithmeticException e) {
                    out.println("ERROR:" + e.getMessage());
                    CalcServer.log("error.log", "Client " + clientName + " (" + socket.getInetAddress() + ") error: " + e.getMessage());
                } catch (RuntimeException e) {
                    out.println("ERROR: Unknown operation");
                }
            }

        } catch (IOException e) {
            CalcServer.log("error.log", "Client error (" + clientName + "): " + e.getMessage());
        }
    }

    private boolean isBinary(String op) {
        return switch (op) {
            case "ADD","SUB","MUL","DIV","POW","MOD","MAX","MIN","AVERAGE" -> true;
            default -> false;
        };
    }

    private Double parseDouble(String line, PrintWriter out) {
        try {
            return Double.parseDouble(line.trim());
        } catch (Exception e) {
            out.println("ERROR: Invalid number");
            throw new RuntimeException("Invalid number");
        }
    }

    private double compute(String op, double a, Double b) {
        switch(op) {
            case "ADD": return a + b;
            case "SUB": return a - b;
            case "MUL": return a * b;
            case "DIV": if (b == 0) throw new ArithmeticException("Division by zero"); return a / b;
            case "POW": return Math.pow(a, b);
            case "MOD": return a % b;
            case "MAX": return Math.max(a, b);
            case "MIN": return Math.min(a, b);
            case "AVERAGE": return (a + b)/2;
            case "SQRT": if(a<0) throw new ArithmeticException("SQRT of negative"); return Math.sqrt(a);
            case "ABS": return Math.abs(a);
            case "FACT": return factorial((int)a);
            case "SIN": return Math.sin(a);
            case "COS": return Math.cos(a);
            case "TAN": return Math.tan(a);
            default: throw new RuntimeException("Unknown operation");
        }
    }

    private long factorial(int n) {
        if (n < 0) throw new ArithmeticException("Factorial of negative number");
        long f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }
}
