# Tym 5

## AvaTask

AvaTask je JavaFX aplikacia s SQLite databazou buildena cez Maven.

## Lokalne spustenie

### Pozadavky

- Java JDK 21
- Maven 3.9 alebo novsi

### Spustenie z terminalu

V koreni projektu spusti:

```powershell
mvn clean javafx:run
```

### Poznamky

- Aplikacia sa da spustat priamo cez Maven, nie je nutne pouzivat iba `Launcher`.
- Ak prikaz `mvn` nefunguje, treba doinstalovat Maven a pridat ho do systemovej premennej `PATH`.
- Lokalna SQLite databaza sa vytvori alebo pouzije zo suboru `avatask.db`.

## Struktura projektu

- Java zdrojaky: `src/main/java`
- FXML a obrazky: `src/main/resources`
- Maven konfiguracia: `pom.xml`
