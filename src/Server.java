import java.sql.*;

/**
 * klasa będąca serwerem
 */
public class Server {

    /**
     * Metoda łączy się z bazą danych na odzielnej maszynie poprzez udostępniany w sieci folder
     * po czym wświetla wszystkie rekordy z tabeli users
     */
    public static void main(String[] args) {
        String dbURL = "jdbc:sqlite:Z:/identifier.sqlite";

        try {
            Connection connection = DriverManager.getConnection(dbURL);

            if (connection != null) {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM users");

                System.out.println("ID | Service Number | Password");

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String serviceNumber = rs.getString("service_number");
                    String password = rs.getString("password");

                    System.out.println(id + " | " + serviceNumber + " | " + password);
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
        }
    }
}
