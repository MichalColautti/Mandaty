import java.io.*;
import java.net.*;
import java.sql.*;

/**
 * klasa będąca serwerem
 */
public class Server {
    /**
     * Metoda łączy się z bazą danych na odzielnej maszynie poprzez udostępniany w sieci folder
     * po czym wświetla wszystkie rekordy z tabeli users
     * narazie obsługuje tylko authenticate dla aplikacji policjanta
     */
    public static void main(String[] args) {
        String dbURL = "jdbc:sqlite:Z:/identifier.sqlite";

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

                //uruchamianie serwera
                try (ServerSocket serverSocket = new ServerSocket(12345)) {
                    System.out.println("Serwer uruchomiony na porcie " + 12345);
                    while (true) {
                        try (Socket clientSocket = serverSocket.accept();
                             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                            String request = in.readLine();
                            System.out.println("Otrzymano dane: " + request);

                            // Sprawdzanie danych logowania
                            String[] credentials = request.split(",");
                            String serviceNumber = credentials[0];
                            String password = credentials[1];

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
}
