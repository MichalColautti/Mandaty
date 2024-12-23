import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
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
     * Zmienna przechowywująca numer służbowy policjanta.
     */
    private String serviceNumber;

    /**
     * klasa reprezentująca wykroczenia ich widełki ilościowe punktów karnych, widełki cenowe oraz czy podlegają recydywie.
     */
    private static class Offense {
        private final int penaltyPointsMin;
        private final int penaltyPointsMax;
        private final int fineMin;
        private final int fineMax;
        private final boolean recidivist;

        /**
         * Konstruktor klasy wykroczeń.
         *
         * @param penaltyPointsMin minimalna ilość punktów karnych dla danego wykroczenia.
         * @param penaltyPointsMax maksymalna ilość punktów karnych dla danego wykroczenia.
         * @param fineMin minimalna wysokość mandatu,
         * @param fineMax maksymalna wysokość mandatu,
         * @param recidivist czy kierowca jest recydywistą,
         */
        public Offense(int penaltyPointsMin, int penaltyPointsMax, int fineMin, int fineMax, boolean recidivist) {
            this.penaltyPointsMin = penaltyPointsMin;
            this.penaltyPointsMax = penaltyPointsMax;
            this.fineMin = fineMin;
            this.fineMax = fineMax;
            this.recidivist = recidivist;
        }

        /**
         * @return zwraca minimalną wysokość mandatu.
         */
        public int getFineMin() {
            return fineMin;
        }

        /**
         * @return zwraca maksymalną wysokość mandatu.
         */
        public int getFineMax() {
            return fineMax;
        }

        /**
         * @return zwraca minimalną ilość punktów karnych.
         */
        public int getPenaltyPointsMin() {
            return penaltyPointsMin;
        }

        /**
         * @return zwraca maksymalną ilość punktów karnych.
         */
        public int getPenaltyPointsMax() {
            return penaltyPointsMax;
        }

        /**
         * @return zwraca czy wykroczenie podlega recydywie.
         */
        public boolean getRecidivist() {
            return recidivist;
        }
    }

    /**
     * Mapa wykroczeń, ich ilości punktów karnych, widełek cenowych oraz
     */
    static HashMap<String,Offense> offenses = new HashMap<>();

    /**
     * dodaje do mapy offenses możliwe wykroczenia ich klamerki punktów karnych oraz klamerki wysokości mandatu.
     */
    public static void addOffenses() {
        //https://www.prawo.pl/prawnicy-sady/taryfikator-mandatow-dla-kierowcow-na-2024-r,525390.html
        offenses.put("Przekroczenie prędkości do 10", new Offense(1,1, 50, 50, false));
        offenses.put("Przekroczenie prędkości o 11-15", new Offense(2,2, 100, 100, false));
        offenses.put("Przekroczenie prędkości o 16-20", new Offense(3,3, 200, 200, false));
        offenses.put("Przekroczenie prędkości o 31-40", new Offense(9,9, 800, 800, true));

        offenses.put("Nieprawidłowe parkowanie", new Offense(1,7, 100, 1200, false));
        offenses.put("Przejazd na czerwonym świetle", new Offense(15,15, 500, 500, true));
        offenses.put("Brak pasów bezpieczeństwa", new Offense(5,5, 100, 100, false));
    }

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
        addOffenses();
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
            serviceNumber = serviceNumberInput.getText();
            String password = passwordInput.getText();

            // Jeśli pole z loginem lub hasłem jest puste to wysyła powiadomienie jeśli są wypełnione to próbuje się zalogować
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
     * Dodaje pola przyjmujące dane kierowcy, pesel, wykroczenie, kwote mandatu, czy kierowca jest recydywistą i ilość punktów karnych.
     * W polach pesel, kwota mandatu i ilość punktów karnych program pozwala na wpisywanie tylko liczb.
     * Pola wykroczenie i czy kierowca jest recydywistą są listami rozwijanymi z określonymi odpowiedziami.
     * Pesel musi zawierać 11 liter jeżeli nie spełni tego warunku program wyświetli powiadomienie.
     * Wysokość mandatu oraz ilość punktów karnych musi się zawierać w określonych klamerkach jeżeli nie spełni tego warunku program wyświetli powiadomienie.
     * Jeżeli kierowca jest recydywistą i wykroczenie podlega recydywie to wysokość mandatu podwaja swoją wartość.
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

        Label peselLabel = new Label("Pesel kierowcy:");
        TextField peselInput = new TextField();
        //niepozwolenie na wpisanie czegoś innego niż intów
        peselInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { //jeśli podany znak jest intem to wpisuje
                return change;
            }
            return null; //jeśli podany znak nie jest intem to go nie wpisuje
        }));

        Label offenseLabel = new Label("Wykroczenie:");
        ComboBox<String> offenseInput = new ComboBox<>();
        offenseInput.getItems().addAll(offenses.keySet());

        Label fineLabel = new Label("Kwota mandatu:");
        TextField fineInput = new TextField();
        //niepozwolenie na wpisanie czegoś innego niż intów
        fineInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { //jeśli podany znak jest intem to wpisuje
                return change;
            }
            return null; //jeśli podany znak nie jest intem to go nie wpisuje
        }));

        Label isRecidivistLabel = new Label("Czy recydywista:");
        ComboBox<String> isRecidivistInput = new ComboBox<>();
        isRecidivistInput.getItems().add("Tak");
        isRecidivistInput.getItems().add("Nie");

        Label penaltyPointsLabel = new Label("Punkty karne:");
        TextField penaltyPointsInput = new TextField();
        //niepozwolenie na wpisanie czegoś innego niż intów
        penaltyPointsInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { //jeśli podany znak jest intem to wpisuje
                return change;
            }
            return null; //jeśli podany znak nie jest intem to go nie wpisuje
        }));

        Button submitTicketButton = new Button("Wystaw mandat");

        ticketFormLayout.add(driverLabel, 0, 0);
        ticketFormLayout.add(driverInput, 1, 0);
        ticketFormLayout.add(peselLabel, 0, 1);
        ticketFormLayout.add(peselInput, 1, 1);
        ticketFormLayout.add(offenseLabel, 0, 2);
        ticketFormLayout.add(offenseInput, 1, 2);
        ticketFormLayout.add(fineLabel, 0, 3);
        ticketFormLayout.add(fineInput, 1, 3);
        ticketFormLayout.add(isRecidivistLabel, 0, 4);
        ticketFormLayout.add(isRecidivistInput, 1, 4);
        ticketFormLayout.add(penaltyPointsLabel, 0, 5);
        ticketFormLayout.add(penaltyPointsInput, 1, 5);
        ticketFormLayout.add(submitTicketButton, 1, 6);
        GridPane.setHalignment(submitTicketButton, HPos.RIGHT);

        submitTicketButton.setOnAction(e -> {
            String driver = driverInput.getText();
            String pesel = peselInput.getText();
            String selectedOffense = offenseInput.getValue();
            String fine = fineInput.getText();
            String isRecidivist = isRecidivistInput.getValue();
            String penaltyPoints = penaltyPointsInput.getText();

            if(driver.isEmpty() || pesel.isEmpty() || selectedOffense == null || fine.isEmpty() || isRecidivist == null || penaltyPoints.isEmpty()) {
                showAlert("Błąd wystawiania mandatu", "Pola nie mogą być puste.");
            }
            else {
                Offense offense = offenses.get(selectedOffense);
                int fineInt = Integer.parseInt(fine);

                //jeśli kierowca jest recydywistą i wykroczenie podlega recydywie dwuktornie zwiększa wysokość mandatu.
                if (isRecidivist.equals("Tak") && offense.getRecidivist()) {
                    fineInt *= 2;
                }
                int penaltyPointsInt = Integer.parseInt(penaltyPoints);

                //sprawdzenie czy długość peselu zgadza się
                if (!validatePesel(pesel)) {
                    showAlert("Błąd wystawiania mandatu", "Pesel powinien zawierać 11 cyfr.");
                }
                //sprawdzenie czy wysokość mandatu zgadza się.
                else if (!validateFine(fineInt, offense)) {
                    if(offense.fineMin == offense.fineMax) {
                        showAlert("Błąd wystawiania mandatu", "Wysokość mandatu musi wynosić " + offense.fineMin + ".");
                    }
                    else {
                        showAlert("Błąd wystawiania mandatu", "Wysokość mandatu musi się zawierać w klamerkach pomiędzy " + offense.fineMin + " a " + offense.fineMax + ".");
                    }
                }
                //sprawdzenie czy ilość punktów karnych zgada się.
                else if (!validatePenaltyPoints(penaltyPointsInt, offense)) {
                    if(penaltyPointsInt < offense.penaltyPointsMin || penaltyPointsInt > offense.penaltyPointsMax) {
                        if(offense.penaltyPointsMin == offense.penaltyPointsMax) {
                            showAlert("Błąd wystawiania mandatu", "Ilość punktów karnych musi wynosić " + offense.penaltyPointsMin + ".");
                        }
                        else {
                            showAlert("Błąd wystawiania mandatu", "Ilość punktów karnych musi się zawierać w klamerkach pomiędzy " + offense.penaltyPointsMin + " a " + offense.penaltyPointsMax + ".");
                        }
                    }
                }
                else {
                    fine = Integer.toString(fineInt);
                    //jeśli wszystkie pola są poprawnie wypełnione to wysyła dane do serwera
                    if (submitTicket(driver, pesel, selectedOffense, fine, penaltyPoints)) {
                        showAlert("Sukces", "Mandat został wystawiony.");
                    } else {
                        showAlert("Błąd", "Wystąpił problem podczas wystawiania mandatu.");
                    }
                }
            }
        });

        Scene ticketScene = new Scene(ticketFormLayout, 340, 270);
        mainStage.setScene(ticketScene);
        mainStage.show();
    }

    /**
     * Sprawdza czy pesel ma poprawną długość.
     *
     * @param pesel pesel do sprawdznia.
     * @return zwraca true jeśli pesel ma odpowiednią długość.
     */
    private boolean validatePesel(String pesel) {
        return pesel.length() == 11;
    }

    /**
     * Sprawdza czy wysokość mandatu jest odpowiednia dla danego wykroczenia.
     *
     * @param fine wysokość mandatu do sprawdzenia.
     * @param offense wykroczenie dla którego sprawdzamy czy zgadza się wysokość mandatu.
     * @return zwraca true jeśli wysokość mandatu zawiera się w klamerkach cenowych dla danego wykroczenia.
     */
    private boolean validateFine(int fine, Offense offense) {
        return fine >= offense.getFineMin() && fine <= offense.getFineMax();
    }

    /**
     * Sprawdza czy ilość punktów karnych jest odpowiednia dla danego wykroczenia.
     *
     * @param penaltyPoints ilość punktów karnych do sprawdzenia.
     * @param offense wykroczenie dla którego sprawdzamy czy zgadza się ilość punktów karnych.
     * @return zwraca true jeśli ilość punktów karnych zawiera się w klamerkach dozwolonej ilości punktów karnych.
     */
    private boolean validatePenaltyPoints(int penaltyPoints, Offense offense) {
        return penaltyPoints >= offense.getPenaltyPointsMin() && penaltyPoints <= offense.getPenaltyPointsMax();
    }

    /**
     * funkcja wysyła do serwera login i hasło i czeka na ocene czy są poprawne
     *
     * @param serviceNumber numer policjanta (login)
     * @param password hasło
     * @return zwraca false dla niepoprawnych danych logowania lub true dla poprawnych
     */
    private boolean authenticate(String serviceNumber, String password) {
        try (Socket socket = new Socket("127.0.0.1", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Wysyłanie danych logowania, na początku auth aby serwer wiedział ze chodzi o autoryzację
            String loginData = "auth," + serviceNumber + "," + password;
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
     * Funkcja wysyła do serwera dane na które ma zostać wystawiony mandat
     *
     * @param driver dane kierowcy
     * @param pesel pesel kierowcy
     * @param offense wykroczenie popełnione
     * @param fine grzywna
     * @param penaltyPoints ilość punktów karnych
     * @return true jeśli uda się wstawić dane do bazy danych, false jeśli się nie uda
     */
    private boolean submitTicket(String driver, String pesel, String offense, String fine, String penaltyPoints) {
        try (Socket socket = new Socket("127.0.0.1", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Wysyłanie danych logowania, na początku ticket aby serwer wiedział ze chodzi o wystawienie mandatu
            String loginData = "ticket," + driver + "," + pesel + "," + offense + "," + fine + "," + penaltyPoints + "," + serviceNumber;
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
