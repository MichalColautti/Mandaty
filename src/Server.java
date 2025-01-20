import java.io.*;
import java.sql.*;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import org.json.*;

/**
 * Klasa będąca serwerem http dla kierowców oraz obsługująca zapytania aplikacji policjanta
 */
public class Server {

    /**
     * Stała zawierająca adres ip serwera
     */
    private final static String host_ip = "172.20.10.7";

    /**
     * Stała zawierająca port do obługi aplikacji klienta
     */
    private final static int port_klient = 8080;

    /**
     * Stała zaweierająca adres do bazy danych
     */
    private final static String dburl = "jdbc:sqlite:Z:\\identifier.sqlite";
    //private final static String dburl = "jdbc:sqlite:C:\\Users\\User\\DataGripProjects\\Mandaty\\identifier.sqlite";


    /**
     * Metoda uruchamia serwer http
     */
    public static void main(String[] args) {
        try {
            start_http();
        } catch (IOException e) {
            System.out.println("Błąd uruchomienia serwera hhtp: " + e.getMessage());
        }
    }

    /**
     * Metoda statyczna uruchamiająca serwer http
     *
     * @throws IOException wyrzcuca błąd IOException
     */
    private static void start_http() throws IOException {
        // Urochomienie serwera na porcie port_klient
        HttpServer server = HttpServer.create(new InetSocketAddress(host_ip, port_klient), 0);
        // Obsługa plików statycznych
        server.createContext("/", new StaticFileHandler("src/klient"));

        // Obsługa API JSON
        server.createContext("/api", new JsonHandler());

        // Obłusga API dla funkcjonalnośći aplikacji policjanta
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/offences", new OffencesHandler());
        server.createContext("/api/createTicket", new CreateTicketHandler());
        server.createContext("/api/cancelTicket", new CancelTicketHandler());

        // Uruchomienie serwera
        server.setExecutor(null);
        server.start();
        System.out.println("Serwer HTTP działa na http://" + host_ip + ":" + port_klient);
    }

    /**
     * Metoda do obługi plików, które są używane do aplikacji klienta
     */
    static class StaticFileHandler implements HttpHandler {
        /**
         * Ścieżka do do katalogu głównego klienta
         */
        private final String basePath;

        /**
         * Konstrucktor klasy StaticFileHandler
         *
         * @param basePath Ścieżka do do katalogu głównego klienta
         */
        public StaticFileHandler(String basePath) {
            this.basePath = basePath;
        }

        /**
         * Metoda obługująca wymianę danych między klient oraz serwerem
         *
         * @param exchange wymiana zawierająca żądanie od klienta i służąca do wysłania odpowiedzi
         * @throws IOException wyrzcuca błąd IOException
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();

            // Jeśli użytkownik żąda favicon
            if (requestedPath.equals("/favicon.ico")) {
                requestedPath = "/image/logo-removebg.png";
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
                    contentType = "application/octet-stream";
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

    /**
     * Metoda obługująca wymianę danych z serwera do klienta przez Jsona
     */
    static class JsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String jsonResponse = null;

            if ("POST".equalsIgnoreCase(method)) {
                // Odczytanie treści żądania POST
                String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                        .lines().collect(Collectors.joining("\n"));
                // Parsowanie JSON
                Map<String, String> data = parseJson(requestBody);

                // Pobieranie akcji od Jsona
                String action = data.get("action");
                System.out.println("Wartość action: " + action);
                if ("login".equalsIgnoreCase(action)) {
                    try (Connection connection = DriverManager.getConnection(dburl)) {
                        if (connection != null) {
                            // Wyszukiwanie użytkownika w bazie danych
                            PreparedStatement pstmt = connection.prepareStatement("SELECT driver.id, driver.pesel, driver.password FROM driver WHERE driver.pesel = ?");
                            pstmt.setString(1, data.get("pesel"));
                            ResultSet rs = pstmt.executeQuery();

                            // Pobranie danych z bazy danych
                            if (rs.next()) {
                                String passwordFromDb = rs.getString("password");
                                if (passwordFromDb != null && passwordFromDb.equals(data.get("password"))) {
                                    jsonResponse = "{ \"message\": \"Poprawnie zalogowano\" }";
                                    System.out.println("Poprawnie zalogowano użytkownika:" + rs.getString("pesel"));
                                } else {
                                    jsonResponse = "{ \"message\": \"Podano złe hasło lub użytkownik nie istnieje\" }";
                                    System.out.println("Błedna próba zalogowania użytkowniaka (złe hasło):" + rs.getString("pesel"));
                                }
                            } else {
                                jsonResponse = "{ \"message\": \"Podano złe hasło lub użytkownik nie istnieje\" }";
                                System.out.println("Błedna próba zalogowania użytkowniaka (nie istnieje):" + rs.getString("pesel"));
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
                        jsonResponse = "{ \"message\": \"Błąd wewnętrzny serwera\" }";
                    }
                } else if ("main_page".equalsIgnoreCase(action)) {
                    // Odczytanie PESEL z treści żądania POST
                    String pesel = data.get("pesel");

                    try (Connection connection = DriverManager.getConnection(dburl)) {
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
            } else {
                // Obsługa nieobsługiwanych metod
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }


        /**
         * Metoda służaca zamiany zapytanie Json na Mapę Stringów
         *
         * @param json zapytanie Json
         * @return Mapa przekonwertowanego Jsona
         */
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
     *  Funkcja czyta strumień i wstawia go do jednego Stringa
     * @param inputStream strumień danych
     * @return Zwraca string zbudowany z strumienia danych
     * @throws IOException zwraca błąd IOEcxeption
     */

    private static String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Obsługuje żądanie logowania użytkownika, sprawdza poprawność danych logowania w bazie danych.
     * Sprawdza, czy numer służbowy i hasło są zgodne z danymi w bazie danych.
     */
    static class LoginHandler implements HttpHandler {
        /**
         * Obsługuje żądanie logowania. Odczytuje dane i sprawdza czy zgadzają się z tymi
         * w bazie danych i zwraca odpowiednią odpowiedź JSON.
         *
         * @param exchange Obiekt HttpExchange zawierający informacje o żądaniu i odpowiedzi.
         * @throws IOException Jeśli wystąpi błąd podczas przetwarzania żądania lub wysyłania odpowiedzi.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = readInputStream(inputStream);
            System.out.println("Otrzymano żądanie logowania: " + requestBody);

            LoginCredentials credentials = parseJson(requestBody);

            String query = "SELECT * FROM users WHERE service_number = ? AND password = ?";

            try (Connection connection = DriverManager.getConnection(dburl)) {
                if (connection != null) {
                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.setString(1, credentials.getServiceNumber());
                        statement.setString(2, credentials.getPassword());

                        try (ResultSet rs = statement.executeQuery()) {
                            JSONObject jsonResponse = new JSONObject();

                            if (rs.next()) {
                                // Użytkownik znaleziony i dane logowania są poprawne
                                jsonResponse.put("success", true);
                                jsonResponse.put("message", "Zalogowano pomyślnie");
                            } else {
                                // Użytkownik nie znaleziony ale dane logowania są niepoprawne
                                jsonResponse.put("success", false);
                                jsonResponse.put("message", "Nieprawidłowe dane logowania");
                            }

                            exchange.getResponseHeaders().set("Content-Type", "application/json");
                            // Wysyłanie odpowiedzi
                            String response = jsonResponse.toString();
                            exchange.sendResponseHeaders(200, response.getBytes().length);

                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("Błąd podczas sprawdzania danych logowania: " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
            }
        }

        /**
         * Parsuje dane JSON i tworzy obiekt LoginCredentials.
         *
         * @param json Ciało żądania w formacie JSON.
         * @return Obiekt LoginCredentials zawierający dane logowania.
         */
        private LoginCredentials parseJson(String json) {
            JSONObject jsonObject = new JSONObject(json);
            String serviceNumber = jsonObject.getString("serviceNumber");
            String password = jsonObject.getString("password");

            LoginCredentials credentials = new LoginCredentials();
            credentials.setServiceNumber(serviceNumber);
            credentials.setPassword(password);

            return credentials;
        }

        /**
         * Klasa reprezentująca dane logowania użytkownika.
         * Zawiera numer służbowy i hasło.
         */
        public static class LoginCredentials {
            private String serviceNumber;
            private String password;

            /**
             * Zwraca numer służbowy użytkownika.
             *
             * @return Numer służbowy.
             */
            public String getServiceNumber() {
                return serviceNumber;
            }

            /**
             * Ustawia numer służbowy użytkownika.
             *
             * @param serviceNumber Numer służbowy.
             */
            public void setServiceNumber(String serviceNumber) {
                this.serviceNumber = serviceNumber;
            }

            /**
             * Zwraca hasło użytkownika.
             *
             * @return Hasło użytkownika.
             */
            public String getPassword() {
                return password;
            }

            /**
             * Ustawia hasło użytkownika.
             *
             * @param password Hasło użytkownika.
             */
            public void setPassword(String password) {
                this.password = password;
            }
        }
    }

    /**
     * Obsługuje żądanie dotyczące pobrania wykroczeń z bazy danych.
     * Wykonuje zapytanie do bazy danych, pobiera informacje o wykroczeniach i zwraca te dane w odpowiedzi w formacie JSON.
     */
    public static class OffencesHandler implements HttpHandler {

        /**
         * Obsługuje żądanie pobrania wykroczeń. Wykonuje zapytanie do bazy danych,
         * aby uzyskać listę wykroczeń, a następnie zwraca ją w odpowiedzi w formacie JSON.
         *
         * @param exchange Obiekt HttpExchange zawierający informacje o żądaniu i odpowiedzi.
         * @throws IOException Jeśli wystąpi błąd podczas przetwarzania żądania lub wysyłania odpowiedzi.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = readInputStream(inputStream);

            String query = "SELECT * FROM offenses";

            JSONObject responseJson = new JSONObject();
            JSONArray offencesArray = new JSONArray();

            try (Connection connection = DriverManager.getConnection(dburl)) {
                if (connection != null) {
                    try (Statement stmt = connection.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {

                        while (rs.next()) {
                            JSONObject offences = new JSONObject();
                            offences.put("id", rs.getInt("id"));
                            offences.put("name", rs.getString("name"));
                            offences.put("penalty_points_min", rs.getInt("penalty_points_min"));
                            offences.put("penalty_points_max", rs.getInt("penalty_points_max"));
                            offences.put("fine_min", rs.getInt("fine_min"));
                            offences.put("fine_max", rs.getInt("fine_max"));
                            offences.put("is_recidivist", rs.getBoolean("is_recidivist"));

                            offencesArray.put(offences);
                        }

                        responseJson.put("offences", offencesArray);

                    } catch (SQLException e) {
                        System.out.println("Błąd podczas ładowania wykroczeń: " + e.getMessage());
                        exchange.sendResponseHeaders(500, 0);
                        OutputStream os = exchange.getResponseBody();
                        os.write("{\"error\": \"Błąd podczas ładowania wykroczeń\"}".getBytes());
                        os.close();
                        return;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
                exchange.sendResponseHeaders(500, 0);
                OutputStream os = exchange.getResponseBody();
                os.write("{\"error\": \"Błąd połączenia z bazą danych\"}".getBytes());
                os.close();
                return;
            }

            // Wysyłanie poprawnej odpowiedzi HTTP 200
            String response = responseJson.toString();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            // Wysłanie odpowiedzi
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Obsługuje żądanie utworzenia nowego mandatu. Odczytuje dane z żądania w formacie JSON,
     * zapisuje te dane do bazy danych i zwraca odpowiedź z numerem wygenerowanego mandatu.
     */
    public static class CreateTicketHandler implements HttpHandler {

        /**
         * Obsługuje żądanie tworzenia nowego mandatu. Odczytuje dane z ciała żądania,
         * wykonuje zapytanie SQL w celu zapisania mandatu do bazy danych i zwraca odpowiedź zawierającą ID mandatu.
         *
         * @param exchange Obiekt HttpExchange zawierający informacje o żądaniu i odpowiedzi.
         * @throws IOException Jeśli wystąpi błąd podczas odczytu danych wejściowych lub wysyłania odpowiedzi.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Metoda nieobsługiwana
                return;
            }

            InputStream inputStream = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
            StringBuilder requestBody = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            reader.close();

            JSONObject jsonRequest = new JSONObject(requestBody.toString());
            String driver = jsonRequest.getString("driver");
            String pesel = jsonRequest.getString("pesel");
            String offense = jsonRequest.getString("offense");
            int fine = jsonRequest.getInt("fine");
            int penaltyPoints = jsonRequest.getInt("penaltyPoints");
            String serviceNumber = jsonRequest.getString("serviceNumber");

            // Zapytanie SQL do zapisania mandatu w bazie danych
            String insertQuery = "INSERT INTO tickets (driver_name, pesel, offense, fine_amount, penalty_points, issued_by) VALUES (?, ?, ?, ?, ?, ?)";
            int generatedTicketId = -1;

            try (Connection connection = DriverManager.getConnection(dburl)) {
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setString(1, driver);
                        stmt.setString(2, pesel);
                        stmt.setString(3, offense);
                        stmt.setInt(4, fine);
                        stmt.setInt(5, penaltyPoints);
                        stmt.setString(6, serviceNumber);

                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            ResultSet rs = stmt.getGeneratedKeys();
                            if (rs.next()) {
                                generatedTicketId = rs.getInt(1);
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Błąd SQL: " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
            }

            // Przygotowanie odpowiedzi JSON
            JSONObject jsonResponse = new JSONObject();
            if (generatedTicketId != -1) {
                jsonResponse.put("success", true);
                jsonResponse.put("ticketId", generatedTicketId);
            } else {
                jsonResponse.put("success", false);
            }

            // Wysłanie odpowiedzi
            String response = jsonResponse.toString();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Obsługuje anulowanie mandatu na podstawie podanego ID. Odczytuje dane z żądania i
     * wykonuje zapytanie SQL aby usunąć mandat z bazy danych i zwraca odpowiedź JSON
     * wskazującą, czy operacja anulowania zakończyła się powodzeniem.
     */
    public static class CancelTicketHandler implements HttpHandler {

        /**
         * Obsługuje żądanie anulowania mandatu.
         * Odczytuje dane wejściowe, wykonuje zapytanie SQL w celu anulowania mandatu
         * i zwraca odpowiedź informującą o wyniku operacji.
         *
         * @param exchange Obiekt HttpExchange zawierający informacje o żądaniu i odpowiedzi.
         * @throws IOException Jeśli wystąpi błąd podczas odczytu danych wejściowych lub wysyłania odpowiedzi.
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
//            System.out.println("handler object = " + this);
//            System.out.println("called by thread = " + Thread.currentThread());
//            exchange.getResponseHeaders().set("Connection", "close");
//            System.out.println("Received request with ID: " + exchange.hashCode());
//            System.out.println("Request method: " + exchange.getRequestMethod());
//            System.out.println("Thread: " + Thread.currentThread().getName());

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Metoda nieobsługiwana
                return;
            }

            StringBuilder requestBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            }
            //System.out.println("Received request body: " + requestBody);

            JSONObject jsonRequest = new JSONObject(requestBody.toString());
            int ticketId = jsonRequest.getInt("ticketId");

            // Zapytanie SQL do anulowania mandatu w bazie danych
            String deleteQuery = "DELETE FROM tickets WHERE id = ?";
            boolean success = false;

            try (Connection connection = DriverManager.getConnection(dburl)) {
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
                        stmt.setInt(1, ticketId);
                        int affectedRows = stmt.executeUpdate();
                        System.out.println("Rows deleted: " + affectedRows);
                        if (affectedRows > 0) {
                            success = true; // Mandat został anulowany
                        }
                    } catch (SQLException e) {
                        System.out.println("SQL error: " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                System.out.println("Database connection error: " + e.getMessage());
            }

            // Przygotowanie odpowiedzi JSON
            JSONObject jsonResponse = new JSONObject();
            if (success) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "Mandat został anulowany.");
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Mandat o podanym ID nie istnieje.");
            }

            //System.out.println("Prepared JSON response: " + jsonResponse);

            // Wysłanie odpowiedzi
            String response = jsonResponse.toString();
            byte[] bs = response.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bs.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bs);
                System.out.println("Response sent successfully.");
            } catch (IOException e) {
                System.out.println("Error while writing response: " + e.getMessage());
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }
}