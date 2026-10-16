//gestisce l'intero flusso di autentificazione OAuth 2.0 con Strava: controlla se esiste già un token valido salvato localmente
// o se deve essere rinnovato, altrimenti avvia un server HTTP locale temporaneo e apre automaticamente il browser dell'utente
//per il login interattivo. Include meccanismi di sicurezza tramite il parametro state casuale e un contatore di sincronizzazione
//per mettere in pausa il programma principale finchè l'utente non completa l'autorizzazione




package it.unipd.toson.strava.client;

import com.sun.net.httpserver.HttpExchange;//rappresenta una singola richiesta/risposta HTTP gestita dal server locale.
import com.sun.net.httpserver.HttpServer;// importa il server HTTP minimale integrato in Java
import java.awt.Desktop;//importa la classe per interagire con le funzioni dekstop del sistema operativo(come aprire browser)
import java.io.IOException;//importa classe per le eccezioni di I/O
import java.net.InetSocketAddress;//importa la gestione degli indirizzi(host+porta di rete) su cui il server locale si mette in ascolto
import java.net.URI;//importa classe per gestione deglu URI/URL
import java.net.URLDecoder;//importa classe per decodificare stringhe URL
import java.net.URLEncoder;//importa per codificare stringhe URL

// Codifica/decodifica URL: obbligatoria per costruire query string corrette (spazi, caratteri speciali nei valori).

import java.net.http.HttpClient;//importa classe per il client HTTP moderno di Java(Java 11+) per le richieste web.
import java.net.http.HttpRequest;//importa la classe per costruire richieste HTTP
import java.net.http.HttpResponse;//importa la classe per gestire le risposte HTTP
import java.nio.charset.StandardCharsets;//importa i set di caratteri standard
import java.security.SecureRandom;//importa un generato di numeri casuali per sicurezza dello state OAuth
import java.time.Duration;//importa per la gestione degli intervalli di tempo
import java.util.Base64;//importa la codifica Base64
// Codifica dello state in caratteri URL-safe (senza / e +).
import java.util.HashMap;//importa la struttura dati mappa(dizionario)
import java.util.Map;//importa l'interfaccia Mappa
import java.util.concurrent.CountDownLatch;//Imporra un sincronizzatore per thread diversi: il thread principale è bloccato finchè non riceve risposta dal server
import java.util.concurrent.TimeUnit;//importa le unità di tempo per la sincronizzazione


public final class OAuthManager
{
    private final AppConfig config;//riferimento alla configurazione dell'app(client id, secret, redirect uri)
    private final TokenStore store;//persistenza dei token: OAuthManager salva ogni nuovo token ricevuto

    //timeout di connessione se il server non risponde in 20 secondi è un problema di rete e quindi la chiamata fallisce
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    //costruttore
    public OAuthManager(AppConfig config)
    {
        this.config = config;//salva la configurazione passata come parametro
        this.store = new TokenStore(config.tokenFile());//il gestore crea il proprio store puntando al file configurato
    }

    //restituisce un token valido(caricandolo, rinnovandolo oppure autorizzandolo da zero
    public TokenPair validToken() throws Exception
    {
        //tenta il caricamento di un token prima salvato su file
        TokenPair token  = store.load();
        //se non c'è alcun token salvato allora avvia il flusso interattivo di autorizzazione via browser
        if (token == null)
        {
            return authorize();
        }
        //se il token sta per scadere allora esegue il refresh token in automatico senza aprire il browser
        if (token.expiresSoon())
        {
            token = refresh(token);

        }
        return token;//restituisce il token valido pronto all'uso
    }

    //gestisce il flusso interattivo di autorizzazione completo via browser: apre il browser e attende il callback
    private TokenPair authorize() throws Exception
    {
        URI redirect = URI.create(config.redirectUri());//converte l'URI di redirect in un oggetto URI
        //controlla se il protocollo dell'URI  deve necessariamente essere http (con https non funziona),
        // se è specificata una porta valida maggiore o uguale a 1 e controlla che l'host non sia nullo
        //se i controlli falliscono viene lanciata un'eccezione
        if (!"http".equalsIgnoreCase(redirect.getScheme()) || redirect.getPort() < 1 || redirect.getHost() == null)
        {
            throw new IllegalArgumentException("Per l'esecuzione locale redirect_uri deve essere un URL http://host:porta/percorso");
        }

        String state = randomState();//genera una stringa casuale per ogni flusso di dati per motivi di sicurezza (nel callback il valore ricevuto deve essere questo)

        CountDownLatch completed = new CountDownLatch(1);//inizializza un contatore(che fa da latch) con contatore a 1 per sincronizzare thread principale e server
        //mette in pausa il programma principale finchè il server HTTP nel thread secondario non riceve la risposta del browser tramite metodo countDown().

        //Il thread principale del programma arriva al metodo await e si ferma. Non va avanti e aspetta al massimo 3 minuti dopo c'è errore per timeout
        //Nel frattempo il server HTTP locale è acceso e aspetta l'apertura del browser e quindi il login. Quando Strava reindirizza l'utente al server,
        // il server si attiva su un suo thread separato e fa partire callback. Alla fine di callback, il codice entra nel blocco finally


        TokenPair[] result = new TokenPair[1];//crea array di supporto a dimensione 1 per memorizzare il token risultante dal callback

        Exception[] failure = new Exception[1];//crea un array di supporto a dimensione 1 per catturare eventuali eccezioni avvenute nel thread

        //crea un'istanza di server HTTP locale legata all'indirizzo e alla porta di redirect. Il redirect uri deve essere lo stesso nel file di configurazione,
        // in HttpServer e nel pannello di configurazione dell'app sul sito di Strava
        HttpServer server = HttpServer.create(new InetSocketAddress(redirect.getHost(), redirect.getPort()), 0);

        //configura l'endpoint del server mappandolo sul percorso specificato nel redirect e associa l'espressione lambda del callback al contesto del server
        server.createContext(redirect.getPath(),exchange -> callback (exchange, redirect, state, completed, result, failure));

        server.start(); //avvia il server HTTP locale mettendolo in ascolto

        //Inizia un blocco try per garantire la chiusura del server anche in caso di errori di esecuzione
        try {
            //costruisce l'URL completo di autorizzazione di Strava concatenando i parametri codificati
            String authUrl = "https://www.strava.com/oauth/authorize?" + form(Map.of(
                    "client_id", config.clientId(),                  //inserisce l'identificativo cliente dell'applicazione
                    "redirect_uri", config.redirectUri(),            //inserisce l'URI di reindirizzamento configurato
                    "response_type", "code",                         //specifica che ci si attende un codice di autorizzazione come risposta("code")
                    "approval_prompt", "auto",                       //richiede a Strava di mostrare la schermata di approvazione se necessario("auto")
                    "scope", "activity:read_all,profile:read_all",   //richiede i permessi di lettura estesa per attività e profilo
                    "state", state));                                //inserisci il token casuale di sicurezza

            System.out.println("Apri questo indirizzo per autorizzare l'applicazione:\n" + authUrl); //l'URL completo di autorizzazione viene stampato in console

            //inizia un blocco try interno per gestire l'apertura automatica del browser
            try
            {   //verifica se l'ambiente di esecuzione supporta le API Desktop grafiche
                if (Desktop.isDesktopSupported())
                {
                    Desktop.getDesktop().browse(URI.create(authUrl)); //tenta di aprire il browser predefinito puntando all'URL di autorizzazione

                }
            }
            catch (Exception ignored) //cattura e ignora delle eccezioni
            {
                //Avvisa l'utente che l'apertura automatica è fallita, e quindi invia il messaggio
                System.err.println("Impossibile aprire automaticamente il browser. Usa il link generato sopra");

            }
            //sospende il thread principale per un massimo di 8 minuti in attesa del callback per lo sblocco del latch da parte del server
            if (!completed.await(8, TimeUnit.MINUTES))
            {
                throw new IOException("Autorizzazione non completata entro 8 minuti.");//lancia eccezione se scade il limite del timeout
            }
            //controlla se durante il callback è stata registrata un'eccezione
            if (failure[0] != null)
            {
                throw failure[0];//viene rilanciata l'eccezione catturata nel thread secondario
            }
            store.save(result[0]);//salva sul file system locale la coppia di token ottenuta con successo
            return result[0];//restituisce la coppia di token al chiamante
        }
        //il blocco finally viene sempre eseguito sia in caso di successo che di errore
        finally
        {
            server.stop(0);//arresta il server HTTP locale liberando la porta perchè senza ciò la porta resterebbe occupata e
            // un secondo avvio fallirebbe con Adress already in use
        }

    }

    //metodo privato eseguito in modo asincrono quando il browser invia la richiesta al server locale
    //gestisce il redirect del browser: riceve code e state nella query string(dopo ? nell'URL).
    private void callback(HttpExchange exchange, URI redirect, String expectedState,CountDownLatch completed, TokenPair[] result, Exception[] failure)
            throws IOException
    {
        //inizia un blocco try per intercettare eventuali errori durante l'elaborazione della risposta HTTP ricevuta
        try {
            Map<String, String> q = query(exchange.getRequestURI().getRawQuery());//estrae e converte i parametri della query string HTTP in una mappa chiave-valore
                                                                                  // la query string del redirect contiene code, state (e scope).

            //verifica che il percorso della richiesta HTTP corrisponda esattamente a quello atteso
            if (!redirect.getPath().equals(exchange.getRequestURI().getPath()))
            {
                throw new IOException("Percorso callback non corrispondente");//se il percorso non coincide lanciata eccezione
            }
            //viene eseguita la verifica di sicurezza confrontando il parametro state ricevuto con quello originario
            if (!expectedState.equals(q.get("state")))
            {
                throw new IOException("Parametro state OAuth non valido");//lancia eccezione se lo state non corrisponde
            }
            //controlla se i parametri contengono un segnale di errore esplicito restituito da Strava
            if (q.containsKey("error"))
            {
                throw new IOException("Autorizzazione rifiutata: " + q.get("error"));//lanciata indicando che l'utente ha rifiutato l'autorizzazione
                                                                                     //Strava reindirizza con error=access_denied invece del codice
            }

            String code = q.get("code");//estrae il codice di autorizzazione temporaneo dai parametri della query

            //verifica che il codice sia nullo oppure vuoto
            if (code == null || code.isBlank())
            {
                throw new IOException("Il callback non contiene il codice OAuth");//se è vuoto o nulla lanciata eccezione
            }
            result[0] = exchangeCode(code);//invia una richiesta a Strava per scambiare il codice temporaneo con i token definitivi nell'array di supporto

            //invia una risposta HTTP con codice di stato 200(OK) al browser dell'utente con un messaggio di conferma
            reply(exchange, 200,"Autorizzazione completata. Puoi chiudere questa pagina e tornare al programma.");
        }
        catch (Exception e)//cattura eccezione verificatasi durante la gestione del callback
        {
            failure[0] = e;//salva l'eccezione nell'array di supporto per poterla rilanciare nel thread principale
            //invia una risposta HTTP con codice di stato 400(Bad Request) al browser con messaggio HTML di errore visualizzato
            reply(exchange, 400,"Autorizzazione non completata. Torna al programma per i dettagli.");
        }
        //blocco eseguito in ogni caso alla fine del metodo callback
        finally {
            completed.countDown();//decrementa il contatore del latch, sbloccando il thread principale

        }
    }

    //metodo usato per effettuare lo scambio del codice temporaneo
    private TokenPair exchangeCode(String code) throws Exception
    {
        //invia una richiesta passando i parametri necessari per lo scambio del codice
        return postToken(Map.of(
            "client_id", config.clientId(),         //inserisce l'ID cliente
            "client_secret", config.clientSecret(), //inserisce la chiave segreta dell'applicazione
            "code", code,                           //inserisce il codice temporaneo ottenuto dal callback
            "grant_type", "authorization_code"));   //specifica che il tipo di concessione è un codice di autorizzazione
    }

    //metodo usato per rinnovare un token che sta per scadere
    private TokenPair refresh(TokenPair old) throws Exception
    {
        //controlla se il refresh token è assente oppure vuoto
        if (old.getRefreshToken() == null || old.getRefreshToken().isBlank())
        {
            //lancia un'eccezione se non è possibile effettuare il rinnovo per mancanza del refresh token
            throw new IOException("Refresh token assente: elimina il token locale e autorizza di nuovo l'app.");
        }

        //invia una richiesta per richiedere un nuovo token di accesso usando il refresh token
        TokenPair fresh = postToken(Map.of(
            "client_id", config.clientId(),            //inserisce l'ID cliente
            "client_secret", config.clientSecret(),    //inserisce la chiave segreta dell'applicazione
            "refresh_token", old.getRefreshToken(),    //inserisce il refresh token salvato in precedenza
            "grant_type", "refresh_token"));           //specifica che il tipo di concessione è un rinnovo tramite refresh token
        store.save(fresh);                             //salva sul file system la nuova coppia di token appena ottenuta

        return fresh;//restituisce la nuova coppia di token aggiornata
    }

    //metodo per inviare richieste all'endpoint dei token di Strava

    private TokenPair postToken(Map<String, String> values) throws Exception
    {
        //inizia la costruzione della richiesta HTTP verso l'endpoint OAuth di Strava
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.strava.com/oauth/token"))
                .timeout(Duration.ofSeconds(30))//imposta un timeout di attesa di 30 secondi per la risposta di rete
                .header("Content-Type", "application/x-www-form-urlencoded")//imposta l'intestazione HTTP per indicare dati inviati come form urlencoded
            .POST(HttpRequest.BodyPublishers.ofString(form(values)))//converte la mappa di parametri in stringa e la inserisce nel corpo della richiesta
            .build();//finalizza la costruzione della richiesta HTTP

        //invia la richiesta tramite il client HTTP e attende la risposta testuale
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        //controlla se il codice di stato HTTP della risposta non appartiene alla famiglia dei "successi"
        if (response.statusCode() / 100 != 2)
        {
            //lancia eccezione se la richiesta al server ddi Strava è fallita
            throw new IOException("Scambio token fallito (HTTP "+ response.statusCode() + "): " + response.body());
        }
        return JacksonMapper.tokenPair(response.body());//converta la risposta JSON string del server in oggetto Java TokenPair usando il mapper
    }

    //metodo per inviare una risposta HTML al browser
    private static void reply(HttpExchange ex, int status, String text) throws IOException
    {
        //crea una stringa HTML di risposta racchiudendo il testo in un paragrafo e converte il codice HTML in un array di byte usando la codifica UTF-8
        byte[] b = ("<!doctype html><meta charset=\"utf-8\"><p>" + text + "</p>").getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");//imposta l'intestazione HTTP della risposta come contenuto HTML in UTF-8

        ex.sendResponseHeaders(status, b.length);//invia il codice di stato HTTP e la lunghezza in byte del contenuto

        //ottiene lo stream di output della risposta e usa il try-with-resources per chiuderlo automaticamente
        try (var out = ex.getResponseBody())
        {
            out.write(b);//scrive i byte dell'HTML all'interno dello stream di risposta verso il browser
        }
    }



    //metodo per analizzare la stringa di query grezza di un URI HTTP
    private static Map<String, String> query(String raw)
    {
        Map<String, String> m = new HashMap<>();//inizializza una nuova mappa vuota per memorizzare le coppie chiave-valore estratte

        //controlla se la stringa di query grezza è nulla
        if (raw == null)
        {
            return m;//restituisce la mappa vuota se non ci sono parametri
        }
        //suddivide la stringa query in singoli parametri usando il carattere '&' come separatore
        for (String p : raw.split("&"))
        {
            String[] kv = p.split("=", 2);//suddivide ciascun parametro in chiave e valore usando il carattere '='(massimo 2 parti)

            //decodifica la chiave URL e la inserisce nella mappa, decodifica il valore se presente, altrimenti inserisce una stringa vuota
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;//restituisce la mappa completa con tutti i parametri decodificati
    }

    //metodo per convertire una mappa di parametri in una stringa formattata application/x-www-form-urlencoded
    private static String form(Map<String, String> m)
    {
        return m.entrySet().stream()                                                        //converte l'insieme delle entrate della mappa in uno stream di dati
               .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)//codifica in URL ciascuna chiave della mappa in UTF-8
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))    //codifica in URL ciascun valore e li unisce con il segno '='
                                                                                            // Entrambe le parti codificate: i valori contengono caratteri speciali
                                                                                            // (i due punti degli scope, lo slash del redirect_uri).

                .reduce((a, b) -> a + "&" + b)                                 //concatena tutti i parametri codificabili separandoli con il carattere '&'
               .orElse("");                                                           //restituisce una stringa vuota se la mappa è vuota

    }

    //metodo per generare una stringa casuale di sicurezza per il parametro state
    private static String randomState()
    {
        byte[] b = new byte[32];//crea un array di byte di dimensione 32
        new SecureRandom().nextBytes(b);//riempie l'array con valori casuali generati da un generatore sicuro
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);//codifica l'array di byte in una stringa Base64 sicura per URL
                                                                         // rimuovendo i caratteri di padding '='

    }
}
