//controlla che le stringhe JSON ricevute da Strava vengano convertite correttamente negli oggetti Activity
//e che la presenza di campi futuri non comprometta il parsing e verifica che la risposta JSON relativa
//al flusso OAUth venga mappata accuratamente all'interno della struttura TokenPair'







package it.unipd.toson.strava.client;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività sportiva
import org.junit.jupiter.api.Test;//importa annotazione @Test di JUnit 5 per marcare i metodi
import java.util.List;//importa interfaccia List per gestire collezioni di oggetti
import static org.junit.jupiter.api.Assertions.*;//importa staticamente le asserzioni di JUnit per i controlli sui risultati

class JacksonMapperTest {

    //Test 1: verifica il corretto parsing delle attività e la gestione dei campi sconosciuti
    @Test
    void mapsApiNamesToActivityAndIgnoresUnknownFields()
    {   //simula una stringa JSON di risposta dell'API di Strava con dati di corsa
        String json = "[{\"id\":1,\"type\":\"Run\",\"start_date\":\"2026-03-15T07:12:04Z\","
            + "\"distance\":5000.0,\"moving_time\":1800,\"average_heartrate\":150.0,"
            + "\"has_heartrate\":true,\"future_field\":\"ignored\"}]";//inserisce un campo fittizio non mappato per testare l'ignorabilità
        List<Activity> result = JacksonMapper.activities(json);//invoca il mapper per convertire la stringa JSON in una lista di Activity
        assertEquals(1, result.size());//verifica che sia stata deserializzata correttamente un'attività
        assertEquals(1L, result.get(0).getId());//verifica la correttezza dell'identificativo numerico
        assertEquals("Run", result.get(0).getType());//verifica la tipologia dell'attività
        assertEquals(5000.0, result.get(0).getDistance());//verifica la distanza registrata in metri
        assertEquals(150.0, result.get(0).getAverageHeartrate());//verifica la frequenza cardiaca media
        assertNull(result.get(0).getWeightedAverageWatts());//verifica che i campi mancanti o null(come la potenza) siano gestiti correttamente
    }

    //Test 2: verifica la corretta deserializzazione dei dati di autentificazione OAuth
    @Test
    void mapsOAuthTokenFields()
    {
        //converte una stringa JSON di risposta OAuth in un oggetto TokenPair
        TokenPair t = JacksonMapper.tokenPair("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_at\":2000000000,\"scope\":\"activity:read_all\"}");
        assertEquals("a", t.getAccessToken());//verifica che il token di accesso sia mappato correttamente
        assertEquals("r", t.getRefreshToken());//verifica che il token di aggiornamento sia mappato correttamente
        assertEquals(2000000000L, t.getExpiresAt());//verifica la correttezza della data/ora di scadenza in formato epoch
        assertEquals("activity:read_all", t.getScope());//verifica gli scope associati all'autorizzazione
    }
}
