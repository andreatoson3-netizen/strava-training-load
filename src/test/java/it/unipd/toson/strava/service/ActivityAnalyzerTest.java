//controlla che il metodo analizzi una lista mista di attività e mantenga solo quelle di tipo RUN i cui timestamp
// rientrano rigorosamente nei confini(data di inizio e fine) specificati. Verifica che vengano scartate sia le attività
//che si trovano fuori dall'intervallo sia quelle di tipo differente(come il ciclismo "Ride"), anche se cronologicamente interne al periodo

package it.unipd.toson.strava.service;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività
import org.junit.jupiter.api.Test;//importa @Test di JUnit 5 per i metodi di prova
import java.time.LocalDate;//importa la gestione delle date(senza fuso orario)
import java.time.ZonedDateTime;//importa la gestione di data e ora con fuso orario
import java.util.List;//importa interfaccia List per collezioni di oggetti
import static org.junit.jupiter.api.Assertions.*;//importa tutte le asserzioni di JUnit

class ActivityAnalyzerTest
{
    //metodo di supporto per creare attività
    private Activity activity(String type, String date) {
        Activity a = new Activity();//istanzia una nuova attività
        a.setType(type);//imposta la tipologia dell'attività
        a.setStartDate(ZonedDateTime.parse(date));//imposta data e ora di inizio parsando la stringa
        a.setDistance(5000.0);//assegna distanza di default(5km)
        a.setMovingTime(1800);//assegna tempo in movimento di default(30 minuti)
        return a;//restituisce oggetto configurato
    }

    //Test: verifica la corretta selezione delle corse all'interno di date inclusivo
    @Test
    void selectsRunsWithinInclusiveDateRange()
    {
        var first = activity("Run", "2026-01-01T09:00:00Z");//crea corsa esatta sul limite iniziale dell'intervallo
        var middle = activity("Run", "2026-01-15T09:00:00Z");//crea corsa nel pieno centro del periodo
        var end = activity("Run", "2026-01-31T21:00:00Z");//crea corsa sul limite finale dell'intervallo
        var outside = activity("Run", "2026-02-01T00:01:00Z");//crea una corsa successiva alla data di fine
        var ride = activity("Ride", "2026-01-20T09:00:00Z");//crea attività di tipo bicicletta nel periodo giusto ma di tipo non valido

        //invoca il metodo di filtraggio passando la lista mista e i confini temporali
        List<Activity> found = ActivityAnalyzer.inDateRange(List.of(first, middle, end, outside, ride),LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"));

        assertEquals(List.of(first, middle, end), found);//verifica che vengano estratte le tre corse comprese nei limiti, escludendo bici
    }
}
