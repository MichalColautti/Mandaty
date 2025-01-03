import java.io.*;
import java.net.*;
import java.sql.*;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * klasa będąca serwerem
 */
public class Server {
    /**
     * Metoda łączy się z bazą danych na odzielnej maszynie poprzez udostępniany w sieci folder
     * po czym wświetla wszystkie rekordy z tabeli users.
     * Pobiera linie od aplikacji policjanta i rozpoznaje czy policjant chce się zalogować czy chce wystawić mandat.
     * Jeżeli zalogować to sprawdza czy dane istnieją w bazie danych.
     * Jeżeli wystawić mandat to wystawia mandat.
     */
    public static void main(String[] args) {
        try{
            start_http();
        }catch (IOException e){
            System.out.println("Błąd uruchomienia serwera hhtp: " + e.getMessage());
        }


        //String dbURL = "jdbc:sqlite:Z:/identifier.sqlite";
        String dbURL = "jdbc:sqlite:C:\\Users\\igor1\\DataGripProjects\\mandaty\\identifier.sqlite";
        try {
            Connection connection = DriverManager.getConnection(dbURL);

            if (connection != null) {
                //wyprintowanie wszystkich danych w bazie danych users (mozna usunąć później)
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM users");

                System.out.println("ID | Service Number | Password");

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String serviceNumber = rs.getString("service_number");
                    String password = rs.getString("password");

                    System.out.println(id + " | " + serviceNumber + " | " + password);
                }

                rs = stmt.executeQuery("SELECT * FROM driver");

                System.out.println("ID | Pesel | Password");

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String pesel = rs.getString("pesel");
                    String password = rs.getString("password");
                    System.out.println(id + " | " + pesel + " | " + password);
                }

                //uruchamianie serwera
                try (ServerSocket serverSocket = new ServerSocket(12345)) {
                    System.out.println("Serwer JavaFx uruchomiony na porcie " + 12345);
                    while (true) {
                        try (Socket clientSocket = serverSocket.accept();
                             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                            String request = in.readLine();
                            System.out.println("Otrzymano dane: " + request);

                            // Sprawdzanie danych logowania
                            String[] credentials = request.split(",");

                            String serviceNumber;
                            switch (credentials[0]) {
                                case "auth":
                                    serviceNumber = credentials[1];
                                    String password = credentials[2];
                                    boolean authenticated = authenticate(connection,serviceNumber, password);

                                    // Odpowiedź
                                    if( authenticated) {
                                        String response = "true";
                                        out.println(response);
                                    }
                                    else {
                                        String response = "false";
                                        out.println(response);
                                    }
                                    break;
                                case "ticket":
                                    String driver = credentials[1];
                                    String pesel = credentials[2];
                                    String offense = credentials[3];
                                    String fine = credentials[4];
                                    String penaltyPoints = credentials[5];
                                    serviceNumber = credentials[6];
                                    boolean ticket = create_ticket(connection, pesel,driver, offense, fine, penaltyPoints, serviceNumber);

                                    // Odpowiedź
                                    if(ticket) {
                                        String response = "true";
                                        out.println(response);
                                    }
                                    else {
                                        String response = "false";
                                        out.println(response);
                                    }
                                    break;
                            }

                        } catch (IOException e) {
                            System.out.println("Błąd podczas komunikacji z klientem: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Błąd uruchomienia serwera: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
        }
    }


    private static void start_http() throws IOException{
        // Tworzenie serwera na porcie 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Dodanie kontekstu dla strony głównej
        server.createContext("/", new HtmlFileHandler());
        server.createContext("/css", new StaticFileHandler("src/klient/css"));
        server.createContext("/js", new StaticFileHandler("src/klient/js"));
        server.createContext("/image", new StaticFileHandler("src/klient/image"));
        server.setExecutor(null); // Domyślny executor
        server.start();
        System.out.println("Serwer http działa na porcie 8080");

    }

    static class HtmlFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = Files.readAllBytes(Paths.get("src/klient/html/index.html"));

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // Handler dla plików statycznych (CSS, JS)
    static class StaticFileHandler implements HttpHandler {
        private final String basePath;

        public StaticFileHandler(String basePath) {
            this.basePath = basePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();
            String filePath = basePath + requestedPath.replaceFirst("/(css|js|image)", "");

            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                // Określ odpowiedni typ MIME
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) {
                    contentType = "application/octet-stream"; // Domyślny typ
                }

                byte[] response = Files.readAllBytes(file.toPath());
                exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);

                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else {
                exchange.sendResponseHeaders(404, -1); // Plik nie znaleziony
            }
        }
    }


    /**
     * Funkcja sprawdzająca dane logowania czy znajdują się w bazie danych
     * @param connection miejsce pliku z bazą danych
     * @param serviceNumber login policjanta
     * @param password hasło policjanta
     * @return zwraca true jeśli podane dane znajduja się w bazie danych lub false jeśli się nie znajdują w bazie danych
     */

    private static boolean authenticate(Connection connection, String serviceNumber, String password) {
        String query = "SELECT * FROM users WHERE service_number = ? AND password = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, serviceNumber);
            statement.setString(2, password);

            try (ResultSet rs = statement.executeQuery()) {
                // Jeśli wynik istnieje, dane logowania są poprawne
                if(rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas sprawdzania danych logowania: " + e.getMessage());
        }
        return false;
    }

    /**
     * @param connection miejsce pliku z bazą danych
     * @param driver imię kierowcy
     * @param pesel pesel kierowcy
     * @param offense wykroczenie popełnione
     * @param fine grzywna
     * @param penaltyPoints ilość punktów karnych
     * @param serviceNumber numer służbowy policjanta
     * @return zwraca true jeżeli uda się zinsertować dane do bazy danych jeżeli się nie uda zwraca false
     */
    private static boolean create_ticket(Connection connection, String driver,String pesel, String offense, String fine, String penaltyPoints, String serviceNumber) {
        String query = "INSERT INTO tickets (driver_name, pesel, offense, fine_amount, penalty_points,issued_by) values (?,?,?,?,?,?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, driver);
            statement.setString(2, pesel);
            statement.setString(3, offense);
            statement.setString(4, fine);
            statement.setString(5, penaltyPoints);
            statement.setString(6, serviceNumber);

            int rows = statement.executeUpdate();
            if(rows > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas sprawdzania danych logowania: " + e.getMessage());
        }
        return false;
    }
}
