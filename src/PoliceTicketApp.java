import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.*;

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
     * Zmienna zawierająca adres url serwera
     */
    private static final String SERVER_URL = "http://localhost:8080/api";

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
     * Pobiera od serwera dane wykroczeń po czym dodaje do mapy offenses - możliwe wykroczenia,
     * ich klamerki punktów karnych oraz klamerki wysokości mandatu.
     */
    public static void addOffenses() {
        try {
            URL url = new URL(SERVER_URL + "/offences");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                System.out.println("Błąd połączenia z serwerem. Kod odpowiedzi: " + status);
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }

            in.close();

            JSONObject jsonResponse = new JSONObject(responseBuilder.toString());

            // Sprawdzamy, czy odpowiedź zawiera tablicę wykroczeń
            if (jsonResponse.has("offences")) {
                JSONArray offensesArray = jsonResponse.getJSONArray("offences");

                for (int i = 0; i < offensesArray.length(); i++) {
                    JSONObject offenseObject = offensesArray.getJSONObject(i);

                    String name = offenseObject.getString("name");
                    int penaltyPointsMin = offenseObject.getInt("penalty_points_min");
                    int penaltyPointsMax = offenseObject.getInt("penalty_points_max");
                    int fineMin = offenseObject.getInt("fine_min");
                    int fineMax = offenseObject.getInt("fine_max");
                    boolean isRecidivist = offenseObject.getBoolean("is_recidivist");

                    Offense offense = new Offense(penaltyPointsMin, penaltyPointsMax, fineMin, fineMax, isRecidivist);
                    offenses.put(name, offense);
                }
            }
        } catch (IOException e) {
            System.out.println("Błąd połączenia z serwerem: " + e.getMessage());
        }
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
                    addOffenses();
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

        Button goToCancelTicketButton = new Button("Anuluj mandat");
        goToCancelTicketButton.setPrefWidth(150);
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
        ticketFormLayout.add(goToCancelTicketButton, 0, 6);
        ticketFormLayout.add(submitTicketButton, 1, 6);
        GridPane.setHalignment(submitTicketButton, HPos.RIGHT);

        goToCancelTicketButton.setOnAction(e -> {
            cancelTicketScene();
        });

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
                    //jeśli kierowca jest recydywistą i wykroczenie podlega recydywie dwuktornie zwiększa wysokość mandatu.
                    if (isRecidivist.equals("Tak") && offense.getRecidivist()) {
                        fineInt *= 2;
                    }
                    fine = Integer.toString(fineInt);
                    //jeśli wszystkie pola są poprawnie wypełnione to wysyła dane do serwera
                    submitTicket(driver, pesel, selectedOffense, fine, penaltyPoints);
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
     * funkcja wysyła do serwera login i hasło w formacie json i czeka na ocene czy są poprawne
     *
     * @param serviceNumber numer policjanta (login)
     * @param password hasło
     * @return zwraca false dla niepoprawnych danych logowania lub true dla poprawnych
     */
    private boolean authenticate(String serviceNumber, String password) {
        try {
            URL url = new URL(SERVER_URL + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            JSONObject json = new JSONObject();
            json.put("serviceNumber", serviceNumber);
            json.put("password", password);

            // Wysłanie danych do serwera
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Odbieranie odpowiedzi z serwera
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            System.out.println("response: " + response.toString());

            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.optBoolean("success", false);

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
    public boolean submitTicket(String driver, String pesel, String offense, String fine, String penaltyPoints) {
        try {
            URL url = new URL(SERVER_URL + "/createTicket");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JSONObject ticketData = new JSONObject();
            ticketData.put("driver", driver);
            ticketData.put("pesel", pesel);
            ticketData.put("offense", offense);
            ticketData.put("fine", fine);
            ticketData.put("penaltyPoints", penaltyPoints);
            ticketData.put("serviceNumber", serviceNumber);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = ticketData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                String response = in.readLine();
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.getBoolean("success")) {
                    showAlert("Sukces", "Mandat o ID: " + jsonResponse.getInt("ticketId") + " został wystawiony.");
                    return true;
                }
            }
            showAlert("Błąd", "Nie udało się wystawić mandatu.");
        } catch (IOException | JSONException e) {
            System.out.println("Błąd komunikacji z serwerem: " + e.getMessage());
        }
        return false;
    }

    /**
     * Dla okna od anulowania mandatów ustawia grid, ustawia padding oraz odległośći między rzędami i kolumnami.
     * Dodaje pola przyjmujące id mandatu.
     * W polu id mandatu program pozwala na wpisywanie tylko liczb.
     * Program sprawdza czy pole id mandatu jest puste jeśli tak to wyświetla powiadomienie z uwagą o wypełnieniu go.
     * Jeśli uda się anulować mandat to pokazuje powiadomienie o sukcesie operacji.
     * Jeśli nie uda się anulować mandatu to pokazuje powiadomienie o porażce operacji.
     */
    private void cancelTicketScene() {
        GridPane cancelTicketFormLayout = new GridPane();
        cancelTicketFormLayout.setPadding(new Insets(10));
        cancelTicketFormLayout.setHgap(10);
        cancelTicketFormLayout.setVgap(10);

        Label ticketIdLabel = new Label("Id mandatu:");
        TextField ticketIdInput = new TextField();
        //niepozwolenie na wpisanie czegoś innego niż intów
        ticketIdInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { //jeśli podany znak jest intem to wpisuje
                return change;
            }
            return null; //jeśli podany znak nie jest intem to go nie wpisuje
        }));

        Button cancelTicketButton = new Button("Anuluj mandat");
        Button goBackButton = new Button("Powrót");

        cancelTicketFormLayout.add(ticketIdLabel, 0, 0);
        cancelTicketFormLayout.add(ticketIdInput, 1, 0);
        cancelTicketFormLayout.add(goBackButton, 0, 1);
        cancelTicketFormLayout.add(cancelTicketButton, 1, 1);
        GridPane.setHalignment(cancelTicketButton, HPos.RIGHT);

        goBackButton.setOnAction(e -> {
            ticketScene();
        });

        cancelTicketButton.setOnAction(e -> {
            String id = ticketIdInput.getText();
            if(id.isEmpty()) {
                showAlert("Błąd anulowania mandatu", "Pola nie mogą być puste.");
            }
            else {
                if(cancelTicket(id)) {
                    showAlert("Sukces", "Mandat został anulowany.");
                }
                else {
                    showAlert("Błąd", "Wystąpił problem podczas anulowania mandatu.");
                }
            }
        });

        Scene cancelTicketScene = new Scene(cancelTicketFormLayout, 250, 90);
        mainStage.setScene(cancelTicketScene);
        mainStage.show();
    }

    /**
     * Funkcja wysyła do serwera id mandatu który ma być anulowany.
     *
     * @param ticketId id mandatu do anulowania
     * @return zwraca true, jeżeli uda się anulować mandat, false, jeżeli się nie powiedzie
     */
    private boolean cancelTicket(String ticketId) {
        //System.out.println("cancelTicket() called at: " + System.currentTimeMillis());
        HttpURLConnection connection = null;
        try {
            JSONObject ticketData = new JSONObject();
            ticketData.put("ticketId", ticketId);

            HttpURLConnection.setFollowRedirects(false);
            System.setProperty("sun.net.http.retryPost", "false");

            URL url = new URL(SERVER_URL + "/cancelTicket");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Expect", "");
            connection.setRequestProperty("Connection", "close");
            System.setProperty("http.keepAlive", "false");

            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = ticketData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
                os.close();
            }

            // Odczytanie odpowiedzi z serwera
            int status = connection.getResponseCode();
            //System.out.println("Response Code: " + status);

            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                String response = in.readLine();
                JSONObject jsonResponse = new JSONObject(response);

                return jsonResponse.getBoolean("success");
            } else {
                showAlert("Błąd", "Wystąpił problem podczas komunikacji z serwerem.");
            }
        } catch (IOException | JSONException e) {
            showAlert("Błąd", "XD Wystąpił błąd podczas anulowania mandatu: " + e.getMessage());
        }finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
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
