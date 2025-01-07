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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Klasa będąca serwerem
 */
public class Server {
    /**
     * Metoda łączy się z bazą danych na oddzielnej maszynie poprzez udostępniany w sieci folder,
     * po czym wyświetla wszystkie rekordy z tabeli users.
     * Pobiera linie od aplikacji policjanta i rozpoznaje czy policjant chce się zalogować, czy chce wystawić mandat.
     * Jeżeli zalogować to sprawdza, czy dane istnieją w bazie danych.
     * Jeżeli wystawić mandat to wystawia mandat.
     * Jeżeli anulować mandat to usuwa mandat.
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
                                    int createdTicketId = createTicket(connection, driver, pesel, offense, fine, penaltyPoints, serviceNumber);

                                    // Odpowiedź
                                    if(createdTicketId > 0) {
                                        String response = "true," + createdTicketId;
                                        out.println(response);
                                    }
                                    else {
                                        String response = "false";
                                        out.println(response);
                                    }
                                    break;
                                case "cancel":
                                    String id = credentials[1];
                                    boolean cancelled = cancelTicket(connection, id);

                                    // Odpowiedź
                                    if(cancelled) {
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

    private static void start_http() throws IOException {
        // Tworzenie serwera na porcie 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Obsługa plików statycznych
        server.createContext("/", new StaticFileHandler("src/klient"));

        // Obsługa API JSON
        server.createContext("/api", new JsonHandler());

        // Uruchomienie serwera
        server.setExecutor(null); // Domyślny executor
        server.start();
        System.out.println("Serwer HTTP działa na http://192.168.1.10:8080");
    }

    // Handler dla plików statycznych
    static class StaticFileHandler implements HttpHandler {
        private final String basePath;

        public StaticFileHandler(String basePath) {
            this.basePath = basePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();

            // Jeśli użytkownik żąda favicon
            if (requestedPath.equals("/favicon.ico")) {
                requestedPath = "/image/logo-removebg.png"; // Ustaw ścieżkę do pliku favicon
            }

            // Jeśli użytkownik wejdzie na "/", zwróć "index.html"
            if (requestedPath.equals("/")) {
                requestedPath = "/html/index.html";
            }

            // Tworzenie pełnej ścieżki do pliku
            String filePath = basePath + requestedPath;
            File file = new File(filePath);

            if (file.exists() && !file.isDirectory()) {
                // Odczytanie zawartości pliku
                byte[] response = Files.readAllBytes(file.toPath());

                // Ustalanie typu MIME
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) {
                    contentType = "application/octet-stream"; // Domyślny typ MIME
                }

                // Wysyłanie nagłówków i treści
                exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } else {
                // Plik nie znaleziony
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    // Handler dla API JSON
    static class JsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String jsonResponse = null;

            if ("POST".equalsIgnoreCase(method)) {
                // Odczytanie treści żądania POST
                String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                        .lines().collect(Collectors.joining("\n"));

                Map<String, String> data = parseJson(requestBody); // Parsowanie JSON

                String action = data.get("action"); // Pobranie wartości pola `action`
                System.out.println("Wartość action: " + action);
                if ("login".equalsIgnoreCase(action)) {
                    String dbURL = "jdbc:sqlite:C:\\Users\\igor1\\DataGripProjects\\mandaty\\identifier.sqlite";
                    try (Connection connection = DriverManager.getConnection(dbURL)) {
                        if (connection != null) {
                            // Wyszukiwanie użytkownika w bazie danych
                            PreparedStatement pstmt = connection.prepareStatement("SELECT driver.id, driver.pesel, driver.password FROM driver WHERE driver.pesel = ?");
                            pstmt.setString(1, data.get("pesel"));
                            ResultSet rs = pstmt.executeQuery();

                            if (rs.next()) {
                                String passwordFromDb = rs.getString("password");
                                if (passwordFromDb != null && passwordFromDb.equals(data.get("password"))) {
                                    jsonResponse = "{ \"message\": \"Poprawnie zalogowano\" }";
                                } else {
                                    jsonResponse = "{ \"message\": \"Podano złe hasło lub użytkownik nie istnieje\" }";
                                }
                            } else {
                                jsonResponse = "{ \"message\": \"Podano złe hasło lub użytkownik nie istnieje\" }";
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
                        jsonResponse = "{ \"message\": \"Błąd wewnętrzny serwera\" }";
                    }
                } else if ("main_page".equalsIgnoreCase(action)) {
                    // Odczytanie PESEL z treści żądania POST
                    System.out.println("hhhh");
                    String pesel = data.get("pesel");

                    String dbURL = "jdbc:sqlite:C:\\Users\\igor1\\DataGripProjects\\mandaty\\identifier.sqlite";
                    try (Connection connection = DriverManager.getConnection(dbURL)) {
                        if (connection != null) {
                            // Zapytanie o bilety dla kierowcy na podstawie PESEL
                            PreparedStatement pstmt = connection.prepareStatement(
                                    "SELECT tickets.driver_name, tickets.offense, tickets.fine_amount, tickets.penalty_points, tickets.issue_date " +
                                            "FROM tickets " +
                                            "JOIN driver ON driver.pesel = tickets.pesel " +
                                            "WHERE driver.pesel = ?"
                            );
                            pstmt.setString(1, pesel);
                            ResultSet rs = pstmt.executeQuery();

                            // Tworzenie odpowiedzi JSON z danymi o biletach
                            StringBuilder ticketsJson = new StringBuilder("[");
                            boolean hasTickets = false;
                            while (rs.next()) {
                                hasTickets = true;
                                ticketsJson.append("{")
                                        .append("\"driver_name\":\"").append(rs.getString("driver_name")).append("\",")
                                        .append("\"offense\":\"").append(rs.getString("offense")).append("\",")
                                        .append("\"fine_amount\":\"").append(rs.getDouble("fine_amount")).append("\",")
                                        .append("\"penalty_points\":\"").append(rs.getInt("penalty_points")).append("\",")
                                        .append("\"issue_date\":\"").append(rs.getString("issue_date")).append("\"")
                                        .append("},");
                            }
                            if (hasTickets) {
                                ticketsJson.deleteCharAt(ticketsJson.length() - 1); // Usuwamy ostatni przecinek
                            }
                            ticketsJson.append("]");
                            jsonResponse = ticketsJson.toString();
                            System.out.println("hhhh");
                        }
                    } catch (SQLException e) {
                        System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
                        jsonResponse = "{ \"message\": \"Błąd wewnętrzny serwera\" }";
                    }
                } else {
                    jsonResponse = "{ \"message\": \"Podano złą metodę\" }";
                }

                // Ustawienie nagłówka odpowiedzi
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

                // Wysłanie odpowiedzi
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                // Obsługa zapytań GET
                 jsonResponse = "{ \"status\": \"API działa poprawnie\" }";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } else {
                // Obsługa nieobsługiwanych metod
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }



        private Map<String, String> parseJson(String json) {
            Map<String, String> data = new HashMap<>();
            json = json.replace("{", "").replace("}", "").replace("\"", "");
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                data.put(keyValue[0].trim(), keyValue[1].trim());
            }
            return data;
        }
    }




    /**
     * Funkcja sprawdzająca dane logowania czy znajdują się w bazie danych
     * @param connection miejsce pliku z bazą danych
     * @param serviceNumber login policjanta
     * @param password hasło policjanta
     * @return zwraca true, jeśli podane dane znajdują się w bazie danych lub false, jeśli się nie znajdują w bazie danych
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
     * Funkcja tworząca w bazie danych mandat.
     *
     * @param connection miejsce pliku z bazą danych
     * @param driver imię kierowcy
     * @param pesel pesel kierowcy
     * @param offense wykroczenie popełnione
     * @param fine grzywna
     * @param penaltyPoints ilość punktów karnych
     * @param serviceNumber numer służbowy policjanta
     * @return zwraca true, jeżeli uda się zinsertować dane do bazy danych, jeżeli się nie uda zwraca false
     */
    private static int createTicket(Connection connection, String driver, String pesel, String offense, String fine, String penaltyPoints, String serviceNumber) {
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
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Zwraca ID mandatu
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas sprawdzania danych logowania: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Funkcja usuwa mandat z bazy danych.
     *
     * @param connection miejsce pliku z bazą danych
     * @param id id mandatu do anulowania
     * @return True, jeśli powiedzie się anulowanie mandatu, false, jeśli się nie powiedzie.
     */
    private static boolean cancelTicket(Connection connection, String id) {
        String query = "DELETE FROM tickets WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, id);

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
