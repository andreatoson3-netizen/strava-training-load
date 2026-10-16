//gestisce la logica di filtraggio e selezione del dataset acquisito,
// separando nettamente la scelta dei dati(quali attività includere) dai calcoli successivi di aggregazione
//si occupa di validare le sessioni di corsa idonee tramite il modello e di applicare un filtro temporale rigoroso
//con estremi inclusivi, sfruttando le Stream API di Java per un'elaborazione dichiarativa, efficiente e pulita




package it.unipd.toson.strava.service;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività sportiva
import java.time.LocalDate;//importa la classe per gestire date senza informazioni di fuso orario
import java.util.List;//importa interfaccia List per la gestione delle collezioni di elementi

//dichiara la classe pubblica, finale e non istanziabile dedicata all'analisi e filtraggio delle attività
public final class ActivityAnalyzer
{
    //costruttore privato per evitare di costruire una nuova istanza
    private ActivityAnalyzer() { }

    //metodo per filtrare le attività valide all'interno di un intervallo di date
    public static List<Activity> inDateRange(List<Activity> data,LocalDate start, LocalDate end)
    {
        //converte la lista di attività in uno stream Java per abilitare l'elaborazione dichiarativa
        return data.stream()
                .filter(Activity::isUsableRun)//filtra lo stream mantenendo solo le corse valide e idonee all'analisi tramite riferimento al metodo del modello
                .filter(a -> !a.getStartDate().toLocalDate().isBefore(start))//esclude le attività la cui data di inizio è precedente alla data di inizio
                                                                                    // dell'intervallo (garantendo l'inclusività dell'estremo inferiore)
                .filter(a -> !a.getStartDate().toLocalDate().isAfter(end))//esclude le attività la cui data di inizio è successiva alla data di fine
                                                                                 // dell'intervallo(garantendo l'inclusività dell'estremo superiore)
                .toList();//raccoglie i risultati filtrati in una nuova lista immutabile

    }
}
