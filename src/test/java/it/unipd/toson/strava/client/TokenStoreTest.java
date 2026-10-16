//controlla che un oggetto TokenPair possa essere correttamente salvato su file in formato JSON
// e poi ricaricato senza perdita di dati
//verifica che il file generato sul file system contenga effettivamente la struttura JSON attesa(come access_token)



package it.unipd.toson.strava.client;

import org.junit.jupiter.api.Test;//importa @Test di JUnit 5 per definire i metodi di prova
import org.junit.jupiter.api.io.TempDir;//importa @TempDir per la gestione di directory temporanee isolate
import java.nio.file.Path;//importa la classe per la gestione dei percorsi di sile system
import static org.junit.jupiter.api.Assertions.*;//importa tutte le asserzioni di JUnit per validare i risultati

class TokenStoreTest
{
    //inietta una directory temporanea pulita e isolata per il test di scrittura file
    @TempDir Path temp;

    //Test 1:verifica il salvataggio, caricamento e formato JSON dei token
    @Test
    void savesAndLoadsTokenPairAsJson() throws Exception
    {
        Path path = temp.resolve("tokens.json");//definisce il percorso del file JSON dei token all'interno della cartella temporanea
        TokenStore store = new TokenStore(path);//inizializza il componente TokenStore associandolo al file di test
        //crea un oggetto TokenPair di prova tramite deserializzazione JSON
        TokenPair original = JacksonMapper.tokenPair("{\"access_token\":\"test-access\",\"refresh_token\":\"test-refresh\",\"expires_at\":2000000000,\"scope\":\"activity:read_all\"}");
        store.save(original);//esegue il salvataggio su file del token originale
        TokenPair loaded = store.load();//ricarica il token prima salvato leggendolo dal file system
        assertNotNull(loaded);//verifica che l'oggetto ricaricato non sia nullo
        assertEquals("test-access", loaded.getAccessToken());//verifica che il token di accesso salvato e ricaricato coincidano
        assertEquals("test-refresh", loaded.getRefreshToken());//verifica che il token di aggiornamento corrisponde perfettamente
        assertTrue(java.nio.file.Files.readString(path).contains("access_token"));//verifica che il file su disco contenga la stringa JSON con la proprietà "access_token"
    }
}
