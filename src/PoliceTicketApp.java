import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

/**
 * Główna klasa aplikacji do wystawiania mandatów przez policję.
 * Implementuje funkcjonalność logowania oraz wystawiania mandatów.
 */
public class PoliceTicketApp extends Application {

    /**
     * Główne okno aplikacji, używane do wyświetlania różnych widoków w aplikacji.
     */
    private Stage mainStage;

    /**
     * Metoda główna aplikacji, która uruchamia aplikację JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Metoda startowa aplikacji, uruchamiana przy starcie programu.
     * Inicjalizuje okno główne oraz ustawia mu tytuł, po czym wywołuje metodę do pokazania ekranu logowania.
     *
     * @param primaryStage główne okno aplikacji, przekazywane przez JavaFX.
     */
    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        primaryStage.setTitle("Aplikacja Policjanta");

        loginScene();
    }

    /**
     * Ustawia grid, ustawia padding oraz odległośći między rzędami i kolumnami.
     * Dodaje pola przyjmujące login i hasło oraz przycisk zaloguj.
     * Jeśli wprowadzony login i hasło będzie poprawne pokazuje scenę do wystawiania mandatu.
     * Jeśli wprowadzony login lub hasło będzie puste pokazuje powiadomienie aby uzupełnić pola
     * Jeśli wprowadzony login i hasło nie będzie poprawne pokazuje powiadomienie, że wprowadzony został nieprawidłowy login lub hasło.
     */
    private void loginScene() {
        GridPane loginGrid = new GridPane();

        //padding gridu od okna
        loginGrid.setPadding(new Insets(10));

        //odległość między kolumnami
        loginGrid.setHgap(10);

        //odległość między rzędami
        loginGrid.setVgap(10);

        Label serviceNumberLabel = new Label("Numer służbowy:");
        TextField serviceNumberInput = new TextField();

        Label passwordLabel = new Label("Hasło:");
        PasswordField passwordInput = new PasswordField();

        Button loginButton = new Button("Zaloguj");

        //i - column | i1 - row
        loginGrid.add(serviceNumberLabel, 0, 0);
        loginGrid.add(serviceNumberInput, 1, 0);
        loginGrid.add(passwordLabel, 0, 1);
        loginGrid.add(passwordInput, 1, 1);
        loginGrid.add(loginButton, 1, 2);
        GridPane.setHalignment(loginButton, HPos.RIGHT);

        loginButton.setOnAction(e -> {
            String serviceNumber = serviceNumberInput.getText();
            String password = passwordInput.getText();
            if(Objects.equals(serviceNumber, "") || Objects.equals(password, "")) {
                showAlert("Błąd logowania", "Pola nie mogą być puste.");
            }
            else {
                if (authenticate(serviceNumber, password)) {
                    ticketScene();
                } else {
                    showAlert("Błąd logowania", "Nieprawidłowy numer służbowy lub hasło.");
                }
            }
        });

        //v - szerokość | v1 - wysokość
        Scene loginScene = new Scene(loginGrid, 280, 130);
        mainStage.setScene(loginScene);
        mainStage.show();
    }

    /**
     * Dla okna od wystawiania mandatów ustawia grid, ustawia padding oraz odległośći między rzędami i kolumnami.
     * Dodaje pola przyjmujące dane kierowcy, wykroczenie, kwote mandatu i ilość punktów karnych.
     * Jeśli uda się wypisać mandat to pokazuje powiadomienie o sukcesie operacji.
     * Jeśli nie uda się wypisać mandatu to pokazuje powiadomienie o porażce operacji.
     */
    private void ticketScene() {
        GridPane ticketFormLayout = new GridPane();
        ticketFormLayout.setPadding(new Insets(10));
        ticketFormLayout.setHgap(10);
        ticketFormLayout.setVgap(10);

        Label driverLabel = new Label("Dane kierowcy:");
        TextField driverInput = new TextField();

        Label offenseLabel = new Label("Wykroczenie:");
        TextField offenseInput = new TextField();

        Label fineLabel = new Label("Kwota mandatu:");
        TextField fineInput = new TextField();

        Label penaltyPointsLabel = new Label("Punkty karne:");
        TextField penaltyPointsInput = new TextField();

        Button submitTicketButton = new Button("Wystaw mandat");

        ticketFormLayout.add(driverLabel, 0, 0);
        ticketFormLayout.add(driverInput, 1, 0);
        ticketFormLayout.add(offenseLabel, 0, 1);
        ticketFormLayout.add(offenseInput, 1, 1);
        ticketFormLayout.add(fineLabel, 0, 2);
        ticketFormLayout.add(fineInput, 1, 2);
        ticketFormLayout.add(penaltyPointsLabel, 0, 3);
        ticketFormLayout.add(penaltyPointsInput, 1, 3);
        ticketFormLayout.add(submitTicketButton, 1, 4);
        GridPane.setHalignment(submitTicketButton, HPos.RIGHT);

        submitTicketButton.setOnAction(e -> {
            String driver = driverInput.getText();
            String offense = offenseInput.getText();
            String fine = fineInput.getText();
            String penaltyPoints = penaltyPointsInput.getText();

            if (submitTicket(driver, offense, fine, penaltyPoints)) {
                showAlert("Sukces", "Mandat został wystawiony.");
            } else {
                showAlert("Błąd", "Wystąpił problem podczas wystawiania mandatu.");
            }
        });

        Scene ticketScene = new Scene(ticketFormLayout, 280, 200);
        mainStage.setScene(ticketScene);
        mainStage.show();
    }

    /**
     * funkcja wysyła do serwera login i hasło i czeka na ocene czy są poprawne
     *
     * @param serviceNumber numer policjanta (login)
     * @param password hasło
     * @return zwraca false dla niepoprawnych danych logowania lub true dla poprawnych
     */
    private boolean authenticate(String serviceNumber, String password) {
        try (Socket socket = new Socket("192.168.1.55", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Wysyłanie danych logowania
            String loginData = serviceNumber + "," + password;
            out.println(loginData);

            // Odbieranie odpowiedzi z serwera
            String response = in.readLine();
            System.out.println("Odpowiedź z serwera: " + response);

            return response.contains("true");
        } catch (IOException e) {
            System.out.println("Błąd komunikacji z serwerem: " + e.getMessage());
            return false;
        }
    }

    /**
     * Funkcja wypisuje dane na które ma zostać wypisany mandat
     *
     * @param driver dane kierowcy
     * @param offense wykroczenie popełnione
     * @param fine grzywna
     * @param penaltyPoints ilość punktów karnych
     * @return true ponieważ narazie nie może się niepowieść
     */
    private boolean submitTicket(String driver, String offense, String fine, String penaltyPoints) {
        System.out.println("Dane mandatu:");
        System.out.println("Kierowca: " + driver);
        System.out.println("Wykroczenie: " + offense);
        System.out.println("Kwota: " + fine);
        System.out.println("Punkty karne: " + penaltyPoints);
        return true;
    }

    /**
     * Funkcja tworzy nowe okno powiadomienia, nadaje mu tytuł oraz wypisuje wiadomość
     * @param title tytuł okna powiadomienia
     * @param message wiadomość która ma zostać wyświetlona w oknie
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
