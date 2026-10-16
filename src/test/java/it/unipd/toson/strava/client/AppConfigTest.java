//controlla che dato un file di configurazione(config.properties) valido e completo, i parametri
//fondamentali(credenziali e intervalli di date) vengono letti e convertiti correttamente
//controlla che il sistema lanci un'eccezione(IOException) qualora il file di configurazione richiesto risulti assente





package it.unipd.toson.strava.client;

import org.junit.jupiter.api.Test;//importa l'annotazione @Test per definire i metodi di test di JUnit 5
import org.junit.jupiter.api.io.TempDir;//importa l'annotazione @TempDir per la gestione di directory temporanee isolate
import java.nio.file.Files;//importa le utilità per la scrittura e manipolazione dei file
import java.nio.file.Path;//importa la classe per la gestione dei percorsi di file system
import static org.junit.jupiter.api.Assertions.*;//importa staticamente tutte le asserzioni di JUnit(assertEquals,assertThrows, etc)

class AppConfigTest
{
    //inietta automaticamente una cartella temporanea pulita ed isolata per i test
    @TempDir Path temp;

    //Test 1 per verificare il caricamento di credenziali e date valide
    @Test
    void loadsRequiredCredentialsAndExplicitDates() throws Exception
    {
        //crea il percorso di un file di configurazione fittizio nella cartella temporanea
        Path configFile = temp.resolve("config.properties");
        //scrive nel file le proprietà di configurazione di prova
        Files.writeString(configFile, """
                client_id=123
                client_secret=secret
                redirect_uri=http://localhost:8080/callback
                start_date=2025-01-01
                end_date=2025-12-31
                """);

        AppConfig c = AppConfig.load(configFile);//invoca il metodo di caricamento della configurazione puntando al file temporaneo
        assertEquals("123", c.clientId());//verifica che il client_id letto corrisponde a quello impostato
        assertEquals("secret", c.clientSecret());//verifica che il client_secret letto corrisponda
        assertEquals(java.time.LocalDate.parse("2025-01-01"), c.startDate());//verifica che la data di inizio sia parsata correttamente
        assertEquals(java.time.LocalDate.parse("2025-12-31"), c.endDate());//verifica che la data di fine sia parsata nel modo giusto
    }

    //Test 2 per verificare cosa succede con file mancante
    @Test
    void rejectsMissingConfigurationFile()
    {
        //si aspetta che l'esecuzione avvii  IOException e tenti di caricare un file di configurazione inesistente nella cartella temporanea
        assertThrows(java.io.IOException.class,() -> AppConfig.load(temp.resolve("missing.properties")));
    }
}
