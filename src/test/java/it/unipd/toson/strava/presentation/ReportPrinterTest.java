//controlla che le attività vengano esportate in un file CSV(activities.csv), gestendo in modo sicuro caratteri speciali
//come le virgole all'interno dei nomi. Verifica che l'intestazione del CSV includa correttamente i campi relativi alla FC
//valuta che il file(monthly_summary.csv) contenga le metriche aggregate corrette e gestisca i dati mancanti



package it.unipd.toson.strava.presentation;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività
import it.unipd.toson.strava.service.TrainingLoadCalculator;//importa il servizio per il calcolo dei carichi di allenamento
import org.junit.jupiter.api.Test;//importa l'annotazione @Test di JUnit 5
import org.junit.jupiter.api.io.TempDir;//importa la gestione per gestire cartelle temporanee isolate nei test
import java.nio.file.Files;//importa l'utilità per la lettura di file
import java.nio.file.Path;//importa la classe per la gestione dei percorsi di file system
import java.time.ZonedDateTime;//importa la gestione avanzata di data e ora con fuso orario
import java.util.List;//importa interfaccia List
import static org.junit.jupiter.api.Assertions.*;//importa tutte le asserzioni di JUnit

class ReportPrinterTest
{
    //inietta una cartella temporanea pulita in cui scrivere i file CSV di test
    @TempDir Path temp;

    //Test:verifica la scrittura dei CSV e la corretta formattazione
    @Test
    void writesActivityAndMonthlyCsvAndEscapesNames() throws Exception
    {
        Activity a = new Activity();//istanzia una nuova attività di prova
        a.setId(7); a.setName("Long run, morning"); a.setType("Run");//imposta ID, nome(con virgola per testare escaping) e tipo
        a.setStartDate(ZonedDateTime.parse("2026-03-15T07:12:04Z"));//imposta la data di inizio
        a.setDistance(10000.0); a.setMovingTime(3000); a.setTotalElevationGain(25.0);//imposta distanza(10km), tempo e dislivello
        a.setHasHeartrate(false);//specifica l'assenza di dati cardiaci
        var summary = TrainingLoadCalculator.monthly(List.of(a));//calcola il riepilogo mensile basato sull'attività creata
        ReportPrinter.write(temp, List.of(a), summary);//esegue la scrittura dei file CSV nella cartella temporanea
        String activities = Files.readString(temp.resolve("activities.csv"));//legge il contenuto del CSV delle attività generate
        String monthly = Files.readString(temp.resolve("monthly_summary.csv"));//legge il contenuto del CSV del riepilogo mensile
        //verifica che la virgola nel nome non rompa la struttura CSV grazie alle virgole
        assertTrue(activities.contains("\"Long run, morning\""),"il nome con virgola deve restare racchiuso tra apici nel CSV");
        //verifica la presenza dei campi di frequenza cardiaca nell'header
        assertTrue(activities.contains("average_heartrate_bpm"),"l'intestazione deve includere i campi di frequenza cardiaca");
        //verifica che l'intestazione del report mensile rispetti lo schema atteso
        assertTrue(monthly.startsWith("month,activity_count,distance_km"),"l'intestazione del riepilogo mensile deve essere stabile");
        //verifica che i dati aggregati del mese siano calcolati e formattati correttamente
        assertTrue(monthly.contains("2026-03,1,10.000,0.833,25.000"),"la riga del mese deve riportare conteggio, distanza, durata e dislivello");
        //verifica che le metriche non disponibile siano rappresentate da zeri e non lascino celle vuote o corrotte
        assertTrue(monthly.contains("0,0"),"FC e potenza assenti devono dare conteggi a zero, non celle piene");
    }
}
