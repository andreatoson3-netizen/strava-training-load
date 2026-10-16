//gestisce lo stato di presentazione e output dell'applicazine, occupandosi dell'esportazione dei dati
//elaborati in formato CSV standard. Si ocuupa di creare la directory di destinaione, formattare le liste di attività
//e i riepiloghi mensili usando la codifica UTF-8, applicare regole di escape conformi allo standard RFC per le stringhe,
// forzare l'uso del punto decimale tramite Locale.ROOT indipendenemente dalle impostazioniu di sistema, e stamopare un reseconto
//finale testuale in console al termine dell'esecuzione




package it.unipd.toson.strava.presentation;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività sportiva
import it.unipd.toson.strava.service.MonthlySummary;//importa il modello dati per il riepilogo mensile aggregato
import java.io.IOException;//importa classe per gestire errori di I/o o scrittura file
import java.nio.charset.StandardCharsets;//importa le costanti per i set di caratteri standard(UTF-8)
import java.nio.file.Files;//importa la classe per operazioni di file system
import java.nio.file.Path;//importa la classe per gestione dei percorsi di file e directory
import java.util.List;//importa l'interfaccia List per collezioni di dati
import java.util.Locale;//importa la classe locale per gestire le convenzioni di formattazione dipendenti dalla lingua/paese


public final class ReportPrinter
{
    //costruttore privato per impedire la creazione di istanze
    private ReportPrinter() { }

    //metodo per scrivere i file di report nella cartella indicata
    public static void write(Path dir, List<Activity> activities, List<MonthlySummary> summaries) throws IOException
    {

        Files.createDirectories(dir);//crea directory di destinazione e le eventuali cartelle intermedie se non esistono già
        writeActivities(dir.resolve("activities.csv"), activities);//scrive il file CSV dettagliato contenente l'elenco delle singolo attività
        writeMonthly(dir.resolve("monthly_summary.csv"), summaries);//scrive il file CSV riepilogativo contenente gli aggregati mensili
        writeReadableActivities(dir, activities);//scrive il report leggibile delle attività singole
        writeReadableReport(dir,summaries);//scrive il report leggibile, una sola esecuzione produce entrambe le forme dello stesso risultato

    }

    //metodo per generare e scrivere il file CSV delle attività
    private static void writeActivities(Path file, List<Activity> data) throws IOException
    {   //inizializza un StringBuilder per costruire il testo del CSV in memoria e aggiunge l'intestazione iniziale del file CSV con i nomi dei campi
        StringBuilder s = new StringBuilder("id,date,name,distance_m,duration_s,elevation_gain_m,has_heartrate,"
                                              +"average_heartrate_bpm,max_heartrate_bpm,relative_effort,"
                                              +"average_speed_m_s,weighted_average_watts\n");
        //itera su ciascuna attività presente nella lista dei dati
        for (Activity a : data)
        {
            s.append(a.getId()).append(',') //aggiunge l'ID dell'attività seguito da una virgola
                .append(csv(a.getStartDate().toLocalDate().toString())).append(',')//aggiunge la data formattata in una stringa
                .append(csv(a.getName())).append(',')//aggiunge il nome nome dell'attività formatta per gestire virgole nel testo
                .append(num(a.getDistance())).append(',')//aggiunge la distanza convertita in formato numerico standard
                .append(a.getMovingTime()).append(',')//aggiunge la durata in movimento in secondi
                .append(num(a.getTotalElevationGain())).append(',')//aggiunge il dislivello totale formattandolo
                .append(Boolean.TRUE.equals(a.getHasHeartrate())).append(',')//aggiunge il valore booleano del flag cardiaco
                .append(num(a.getAverageHeartrate())).append(',')//aggiunge la frequenza cardiaca media formattata
                .append(num(a.getMaxHeartrate())).append(',')//aggiunge la frequenza cardiaca massima formattata
                .append(num(a.getSufferScore())).append(',')//aggiunge il punteggio di sforzo relativo formattato
                .append(num(a.getAverageSpeed())).append(',')//aggiunge la velocità media formatatta
                .append(num(a.getWeightedAverageWatts())).append('\n');//aggiunge la potenza media ponderata e chiude la riga andando a capo

        }
        //scrive tutto il contenuto testuale accumulato nel file specificato usando la codifica UTF-8
        Files.writeString(file, s.toString(), StandardCharsets.UTF_8);
    }

    //metodo per generare e scrivere il file CSV dei riepiloghi mensili
    private static void writeMonthly(Path file, List<MonthlySummary> data) throws IOException {
       //inizializza un StringBuilder per costruire il testo del report mensile e inserisce l'intestazione delle colonne per il report
        StringBuilder s = new StringBuilder("month,activity_count,distance_km,duration_h,elevation_gain_m,"
            + "average_heartrate_bpm,relative_effort_total,average_pace_min_km,"
            + "average_power_w,activities_with_heartrate,activities_with_power\n");

        //itera su ciascun elemento di riepilogo mensile
        for (MonthlySummary m : data)
        {
            s.append(m.month()).append(',')//aggiunge il mese di riferimento(formato ISO) seguito da una virgola
                .append(m.activityCount()).append(',')//aggiunge il conteggio delle attività del mese
                .append(num(m.distanceKm())).append(',')//aggiunge la distanza totale in chilometro formattata
                .append(num(m.durationHours())).append(',')//aggiunge la durata complessiva in ora formattata
                .append(num(m.elevationGainM())).append(',')//aggiunge il dislivello totale formattato
                .append(num(m.averageHeartRateBpm())).append(',')//aggiunge la frequenza cardiaca media mensile formattata
                .append(num(m.relativeEffortTotal())).append(',')//aggiunge lo sforzo relativo totale accumulato
                .append(num(m.averagePaceMinPerKm())).append(',')//aggiunge il passo medio mensile formattato
                .append(num(m.averagePowerWatts())).append(',')//aggiunge la potenza media formattata
                .append(m.heartRateActivityCount()).append(',')//aggiunge il numero di attività con dati cardiaci validi
                .append(m.powerActivityCount()).append('\n');//aggiunge il numero di attività con dati di potenza e chiude la riga
        }
        //scrive il testo completato nel file di output mensile con codifica UTF-8
        Files.writeString(file, s.toString(), StandardCharsets.UTF_8);
    }


    //metodo che accetta directory di destinazione Path e lista di attività(Activity) lanciando eventualmente in caso IOException
    public static void writeReadableActivities(Path dir, List<Activity> activities) throws IOException
    {
        //inizializza un StringBuilder per accumulare ed elaborare il testo delle attività
        StringBuilder s = new StringBuilder();

        //appende le tabelle formattate con larghezza delle colonne per allineare i dati
        //tipo -10% per la data e %6s per i km ecc
        s.append(String.format("%-10s %6s %5s %7s %7s %7s %8s  %s%n","Data", "Km", "Min", "FCmed", "FCmax", "Sforzo", "Min/km", "Nome"));

        //aggiunge una riga di separazione di 84 trattini con azione di andare a capo
        s.append("-".repeat(84)).append('\n');

        //avvia il ciclo per scorrere una a una le attività della lista
        for (Activity a : activities)
        {
            double km = a.getDistance() / 1000.0;//converta la distanza da metri in km

            //calcola la velocità in minuti al km dividendo minuti totali per i km percorsi.
            // Se la distanza è >0 e il tempo è != da zero allora scrivo il risultato con due decimali sennò metto il trattino -
            String pace = (km > 0 && a.getMovingTime() != null) ? String.format(Locale.ROOT, "%.2f", a.getMovingTime() / 60.0 / km) : "-";

            //appende la riga per attività corrente con le caratteristiche definite
            s.append(String.format("%-10s %6.2f %5d %7s %7s %7s %8s  %s%n",
                    a.getStartDate().toLocalDate(),      //estrae la data(anno-mese-giorno)
                    km,                                  //inserisce la distanza in km con 2 decimali
                    a.getMovingTime(),                   //inserisce il tempo in movimento
                    numOrDash(a.getAverageHeartrate(), 1), //converte la FC media o "-" se assente
                    numOrDash(a.getMaxHeartrate(), 0),//converte la FC massima o restituisce "-" se assente
                    numOrDash(a.getSufferScore(), 0),//converte lo sforzo relativo o restituisce "-" se assente
                    pace, //inserisce il passo calcolato in min/km oppure "-"
                    fit(a.getName(), 32)));// il nome dell'attività è ridotto a un massimo di 32 caratteri per allineamento alla colonna
        }

        //converte il contenuto accumulato nello StringBuilder in una stringa e lo scrive nel file "report_attivita.txt" usando la codifica UTF-8
        Files.writeString(dir.resolve("report_attivita.txt"), s.toString(), StandardCharsets.UTF_8);
    }

    //tronca stringa alla massima larghezza della colonna e se serve usa "..."
    private static String fit(String value, int maxWidth)
    {
        //verifica se la stringa è nulla e nel caso restituisce una stringa vuota
        if (value == null)
        {
            return "";
        }

        //se la lunghezza rientra nel limite massimo maxWidth restituisce la stringa sennò taglia la stringa e al posto delle ultime 3 lettere mette ...
        return value.length() <= maxWidth ? value : value.substring(0, maxWidth - 3) + "...";
    }

    //metodo che riceve per parametro una directory e una lista di riepiloghi mensili, lanciando eccezioni di tipo IOException
    // in caso di problemi di scrittura oppure accesso ai file
    public static void writeReadableReport(Path dir, List<MonthlySummary> summaries)throws IOException
    {
        //inizializza un oggetto StringBuilder per accumulare e costruire il testo del report
        StringBuilder s = new StringBuilder();

        //appende le intestazioni della tabella formattata a larghezza fissa con colonne allineate in senso verticale con i dati
        s.append(String.format("%-8s %4s %9s %8s %9s %7s %8s %7s%n","Mese", "N.", "Km", "Ore", "Disl.m", "FCmed", "Sforzo", "Min/km"));

        // aggiunge linea divisoria composta da 66 trattini e con un interruzione di riga
        s.append("-".repeat(66)).append('\n');

        //ciclo per scorrere uno ad uno i riepiloghi mensili presenti nella lista
        for (MonthlySummary m : summaries)
        {
            //compone e appende la riga formattata per il mese corrente, rispettando i vincoli di spazio
            s.append(String.format("%-8s %4d %9.1f %8.1f %9.1f %7s %8s %7s%n",
                    m.month(),                      // inserisco il mese in formato stringa (es.2026-03)
                    m.activityCount(),              // inserisce il numero intero di attività registrate
                    m.distanceKm(),                 // inserisce la distanza totale in km con un decimale
                    m.durationHours(),              // inserisce la durata totale in ore con un decimale
                    m.elevationGainM(),             // inserisce il dislivello positivo in metri (con 1 decimale)
                    numOrDash(m.averageHeartRateBpm(), 1),//converte la frequenza cardiaca media o restituisce "-" se non presente
                    numOrDash(m.relativeEffortTotal(), 0),//converte lo sforzo relativo totale oppure restituisce "-" se non presente
                    numOrDash(m.averagePaceMinPerKm(), 2)));//converte il passo medio in min/km o restituisce "-" se non presente
        }

        //converte il contenuto dello StringBuilder in stringa e lo scrive nel file "report_mensile.txt"
        //nele directory di destinazione, usano la codifica standard UTF-8
        Files.writeString(dir.resolve("report_mensile.txt"), s.toString(),StandardCharsets.UTF_8);
    }

    //dichiara un metodo che riceve un numero generico e il numero di decimali desiderati restituendo una stringa formattata oppure "-" se il valore è nullo
    private static String numOrDash(Number n, int decimals)
    {
        //verifica se il numero è nullo e nel caso restituisce "-"
        if (n == null)
        {
            return "-";
        }

        //converte il numero in double e lo formatta applicando il numero di cifre decimali richiesto
        return String.format(Locale.ROOT, "%." + decimals + "f", n.doubleValue());
    }

    //metodo per formattare i valori numerici con tre cifre decimali
    private static String num(Number n)
    { //se il numero è nullo restituisce stringa vuota, altrimenti formatta usando il punto come separatore decimale fisso
        return n == null ? "" : String.format(Locale.ROOT, "%.3f", n.doubleValue());
    }

    //metodo per applicare l'escape alle stringhe secondo lo standard CSV
     private static String csv(String value)
     {
         //se la stringa di input è nulla restituisce una stringa vuota
        if (value == null)
        {
            return "";
        }
        //racchiude il testo tra doppi apici e raddoppia eventuali apici interni presenti nella stringa
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    //metodo per stampare in console un riepilogo finale dell'esportazione
    public static void printSummary(List<Activity> activities,List<MonthlySummary> summaries, Path output)
    {
        //stampa il numero totale di attività di corsa elaborate
        System.out.println("Attivita' di corsa incluse: " + activities.size());
        //stampa il numero di mesi coperti dai dati aggregati
        System.out.println("Mesi con dati: " + summaries.size());
        //stampa il percorso assoluto della cartella in cui sono stati salvati i file
        System.out.println("File CSV creati in: " + output.toAbsolutePath());
    }
}
