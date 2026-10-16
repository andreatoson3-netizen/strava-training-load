//rappresenta la classe per identificare la coppia di token OAuth 2.0 restituita dall'endpoint di autentificazione di Strava
// (access_token e refresh_token,expires_at e scope).
// Fa da modello per la deserializzazione JSON tramite Jackson mappando i campi dell'API e offrendo un metodo di controllo(expireSoon)
// che verifica la validità del token integrando un margine di sicurezza temporale



package it.unipd.toson.strava.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;//importa la @JsonIgnoreProperties per istruire Jackson a ignorare campi JSON non mappati nella classe
import com.fasterxml.jackson.annotation.JsonProperty;//importa @JsonPropertiy per collegare i nomi dei campi JSON alle proprietà e metodi Java


@JsonIgnoreProperties(ignoreUnknown = true)//configura Jackson per ignorare campi extra presenti nel JSON di Strava che non interessano al software

public class TokenPair
{

    private String accessToken;//token che serve per memorizzare l'accesso temporaneo alle API(durata di ore)

    private String refreshToken;//token che memorizza il token di rinnovo a lungo termine(durata di mesi)

    private long expiresAt;//memorizza l'istante di scadenza del token in formato epoch

    private String scope;//memorizza i permessi concessi all'utente


    public String getAccessToken() { return accessToken; }//metodo getter per leggere il valore del token di accesso

    @JsonProperty("access_token")
    public void setAccessToken(String v) { accessToken = v; }//metodo sette associato alla proprietà JSON "access_token" per la deserializzazione

    public String getRefreshToken() { return refreshToken; }//metodo getter per leggere il valore del token di rinnovo

    @JsonProperty("refresh_token")
    public void setRefreshToken(String v) { refreshToken = v; }//metodo setter associato alla proprietà JSON "refresh_token" per la deserializzazione

    public long getExpiresAt() { return expiresAt; } //metodo getter per leggere il timestamp di scadenza

    @JsonProperty("expires_at")
    public void setExpiresAt(long v) { expiresAt = v; }//metodo setter associato alla proprietà JSON "expires_at" per la deserializzazione

    public String getScope() { return scope; }//metodo getter per leggere i vari permessi concessi all'utente

    @JsonProperty("scope")
    public void setScope(String v) { scope = v; }//metodo setter associato alla proprietà JSON "scope" per la deserializzazione

    //metodo bolleano che verifica se il token va rinnovato prima di usarlo
    public boolean expiresSoon() {

        //controlla se il token non esiste oppure se il tempo attuale con margine di 1 minuto supera la scadenza
        return accessToken == null || expiresAt <= System.currentTimeMillis() / 1000L + 60;
        // System.currentTimeMillis() restituisce millisecondi e la divisione per 1000 allinea l'unita' di misura con expiresAt (secondi).
        // Il margine di 60 secondi è usato per sicurezza
    }
}
