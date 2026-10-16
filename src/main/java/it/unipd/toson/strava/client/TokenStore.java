//gestisce la persistenza locale dei token OAuth salvandoli su un file JSON nel file system.
// Evita che l'utente debba fare l'autentificazione ogni volta.
// Si occupa di caricare i token esistenti(restituendo null se il file non è presente),
// di serializzare e salvare le nuove coppie di token e di applicare restrizioni sui permessi del file





package it.unipd.toson.strava.client;

import java.io.IOException;//importa la classe per gestire errori di I/O e di accesso al file
import java.nio.file.Files;//importa la classe per operazioni sul file system
import java.nio.file.Path;//importa la classe per rappresentare percorsi di file e directory in modo indipendente dal sistema operativo
import java.nio.file.attribute.PosixFilePermission;//importa enumerazione per gestire permessi POSIX sui file
import java.util.EnumSet;//importa colllezione ottimizzata per enumerati, usata per definire insieme di permessi


public final class TokenStore
{

    private final Path file;//memorizza il percorso del file JSON in cui salvare i token

    //costruttore che riceve il percorso del file come parametro e lo assegna a file
    public TokenStore(Path file)
    {
        this.file = file;
    }

    //metodo per caricare i token dal file system locale, lanciando ecccezzioni di I/O in caso di problemi
    public TokenPair load() throws IOException {
        //se il file dei token non esiste ancora sul disco allora restituisce null(normale al primo avvio)
        if (!Files.exists(file))
        {
            return null;
        }
        //inizia try per intercettare errori di lettura o di corruzione del file JSON
        try
        {
            //legge il file JSON e lo deserializza in un oggetto Java TokenPair usando il mapper condiviso
            return JacksonMapper.MAPPER.readValue(file.toFile(), TokenPair.class);

        } //cattura qualsiasi eccezzione di parsing o I/O dovuta a un file corrotto
        catch (Exception e)
        {
            //lancia eccezione dicendo che il file è corrotto
            throw new IOException("Token locali illeggibili. Elimina il file token e autorizza di nuovo l'app.", e);
        }
    }

    //metodo per salvare una coppia di token sul fle system locale
    public void save(TokenPair pair) throws IOException
    {
        Path parent = file.toAbsolutePath().getParent();//estrae il percorse della cartella contenitore in cui risiede il file dei token

        Files.createDirectories(parent);//crea cartella padre e le directory intermedie se non esistono già sul disco

        //configura Jackson per produrre un JSON formattato e leggibile e scrive l'oggetto TokenPair convertendolo in formato JSON nel file specificato
        JacksonMapper.MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), pair);

        //inizia il blocco try per tentare l'impostazione dei permessi di sicurezza sul file appena creato
        try
        {
            //imposta i permessi di accesso avanzati sul file secondo lo standard POSIX e concede i permessi di sola lettura e scrittura al proprietario
            Files.setPosixFilePermissions(file,EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
        //cattura ed ignora l'eccezione lanciata dai sistemi operativi
        catch (UnsupportedOperationException ignored)
        {
            //l'eccezione viene ignorata e il programma prosegue comunque.
        }
    }
}
