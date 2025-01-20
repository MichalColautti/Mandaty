Do project structure dodać java fx, json i sqlite-jdbc

https://gluonhq.com/products/javafx/
https://github.com/stleary/JSON-java/tree/master?tab=readme-ov-file
https://github.com/xerial/sqlite-jdbc/releases/tag/3.47.1.0

oraz do run configurations dla aplikacji policjanta dodać vm options i wpisać tą linijkę
--module-path sciezkaDoFolderuJavafx/lib --add-modules javafx.controls,javafx.fxml

Na komputerze z bazą danych plik bazy danych musi zostać udostępniony w sieci, a jego lokalizacja zaktualizowana na serwerze.
