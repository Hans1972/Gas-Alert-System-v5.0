package nl.hbelectronics.hpgastest;

import android.content.Context;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Persistent, offline UI translations. English is the default. Protocol tokens are never translated. */
public final class UiLanguage {
 private UiLanguage(){}
 public static final String[] CODES={"en","nl","de","fr"};
 public static final String[] OPTIONS={"🇬🇧 English","🇳🇱 Nederlands","🇩🇪 Deutsch","🇫🇷 Français"};
 private static final Map<String,String[]> LINES=new HashMap<>();
 private static void add(String key,String nl,String de,String fr){LINES.put(key,new String[]{nl,de,fr});}
 static {
  add("Settings","Instellingen","Einstellungen","Paramètres");
  add("General Settings","Algemene instellingen","Allgemeine Einstellungen","Paramètres généraux");
  add("Help & information","Uitleg en informatie","Hilfe und Informationen","Aide et informations");
  add("Language","Taal","Sprache","Langue");
  add("License management","Licentiebeheer","Lizenzverwaltung","Gestion des licences");
  add("Version","Versie","Version","Version");
  add("Checking license status…","Licentiestatus controleren…","Lizenzstatus wird geprüft…","Vérification de la licence…");
  add("Bluetooth not connected","Bluetooth niet verbonden","Bluetooth nicht verbunden","Bluetooth non connecté");
  add("Signal: unavailable (disconnected)","Signaal: niet beschikbaar (niet verbonden)","Signal: nicht verfügbar (getrennt)","Signal : indisponible (déconnecté)");
  add("Signal: measuring…","Signaal: meten…","Signal: wird gemessen…","Signal : mesure en cours…");
  add("Signal:","Signaal:","Signal:","Signal :");
  add("(last measurement)","(laatste meting)","(letzte Messung)","(dernière mesure)");
  add("Bluetooth connected · controller confirmed","Bluetooth verbonden · controller bevestigd","Bluetooth verbunden · Controller bestätigt","Bluetooth connecté · contrôleur confirmé");
  add("Bluetooth connected · waiting for fresh controller status","Bluetooth verbonden · wachten op nieuwe controllerstatus","Bluetooth verbunden · warte auf aktuellen Controllerstatus","Bluetooth connecté · attente du nouvel état du contrôleur");
  add("Bluetooth link disconnected — automatic reconnect","Bluetooth-verbinding verbroken · automatisch opnieuw verbinden","Bluetooth-Verbindung getrennt · automatische Wiederverbindung","Liaison Bluetooth interrompue · reconnexion automatique");
  add("Not connected","Niet verbonden","Nicht verbunden","Non connecté");
  add("Disconnected","Verbinding verbroken","Getrennt","Déconnecté");
  add("Searching for reserved controller…","Zoeken naar geselecteerde controller…","Suche nach ausgewähltem Controller…","Recherche du contrôleur sélectionné…");
  add("Bluetooth unavailable or permission missing","Bluetooth niet beschikbaar of toestemming ontbreekt","Bluetooth nicht verfügbar oder Berechtigung fehlt","Bluetooth indisponible ou autorisation manquante");
  add("Connect Bluetooth","Bluetooth verbinden","Bluetooth verbinden","Connecter le Bluetooth");
  add("Connect to a Gas Alert v5.0 controller","Verbind met een Gas Alert v5.0-controller","Mit einem Gas Alert v5.0-Controller verbinden","Se connecter à un contrôleur Gas Alert v5.0");
  add("STATUS UNKNOWN","STATUS ONBEKEND","STATUS UNBEKANNT","ÉTAT INCONNU");
  add("ALL LIGHTS OFF","ALLE LAMPEN UIT","ALLE LEUCHTEN AUS","ÉTEINDRE TOUTES LES LUMIÈRES");
  add("HP GAS ALERT\nNOT CONNECTED","HP GAS ALERT\nNIET VERBONDEN","HP GAS ALERT\nNICHT VERBUNDEN","HP GAS ALERT\nNON CONNECTÉ");
  add("HP GAS ALERT ON\nTAP TO RESET","HP GAS ALERT AAN\nTIK OM TE RESETTEN","HP GAS ALERT EIN\nZUM ZURÜCKSETZEN TIPPEN","HP GAS ALERT ACTIVÉ\nTOUCHER POUR RÉINITIALISER");
  add("HP GAS ALERT OFF\nTAP TO START","HP GAS ALERT UIT\nTIK OM TE STARTEN","HP GAS ALERT AUS\nZUM STARTEN TIPPEN","HP GAS ALERT ARRÊTÉ\nTOUCHER POUR DÉMARRER");
  add("LIGHTS OFF\nTAP TO TURN ON","LAMPEN UIT\nTIK OM AAN TE ZETTEN","LEUCHTEN AUS\nZUM EINSCHALTEN TIPPEN","LUMIÈRES ÉTEINTES\nTOUCHER POUR ALLUMER");
  add("ALARM · ORANGE","ALARM · ORANJE","ALARM · ORANGE","ALARME · ORANGE");
  add("NORMAL · GREEN","NORMAAL · GROEN","NORMAL · GRÜN","NORMAL · VERT");
  add("OFF","UIT","AUS","ARRÊT");
  add("Disconnect / Change device","Verbinding verbreken / andere controller","Trennen / Gerät wechseln","Déconnecter / changer d'appareil");
  add("Reconnect / Change device","Opnieuw verbinden / andere controller","Erneut verbinden / Gerät wechseln","Reconnecter / changer d'appareil");
  add("Scanning…","Zoeken…","Suche läuft…","Recherche…");
  add("Automatic Bluetooth reconnect is active","Automatisch opnieuw verbinden via Bluetooth is actief","Automatische Bluetooth-Wiederverbindung ist aktiv","La reconnexion Bluetooth automatique est active");
  add("Select the Gas Alert v5.0 controller.","Selecteer de Gas Alert v5.0-controller.","Gas Alert v5.0-Controller auswählen.","Sélectionnez le contrôleur Gas Alert v5.0.");
  add("Turn all lights off?","Alle lampen uitschakelen?","Alle Leuchten ausschalten?","Éteindre toutes les lumières ?");
  add("This is only possible when the controller confirms NORMAL (green).","Dit kan alleen als de controller NORMAAL (groen) bevestigt.","Nur möglich, wenn der Controller NORMAL (grün) bestätigt.","Possible uniquement lorsque le contrôleur confirme NORMAL (vert).");
  add("Cancel","Annuleren","Abbrechen","Annuler");
  add("Turn off","Uitschakelen","Ausschalten","Éteindre");
  add("Close","Sluiten","Schließen","Fermer");
  add("Save","Opslaan","Speichern","Enregistrer");
  add("Selected value","Gekozen waarde","Ausgewählter Wert","Valeur sélectionnée");
  add("✓ Saved successfully","✓ Opgeslagen","✓ Erfolgreich gespeichert","✓ Enregistré");
  add("Setting not saved","Instelling niet opgeslagen","Einstellung nicht gespeichert","Paramètre non enregistré");
  add("Green brightness (%)","Groene helderheid (%)","Grüne Helligkeit (%)","Luminosité verte (%)");
  add("Number of physical LEDs","Aantal fysieke leds","Anzahl physischer LEDs","Nombre de LED physiques");
  add("Enter the total number of LEDs. In Double mirrored mode this must be even: 37 LEDs per row = 74 total.","Vul het totale aantal leds in. Bij Dubbel gespiegeld moet dit even zijn: 37 leds per rij = 74 totaal.","Geben Sie die Gesamtzahl der LEDs ein. Bei doppelt gespiegelt muss sie gerade sein: 37 LEDs pro Reihe = 74 insgesamt.","Saisissez le nombre total de LED. En mode double miroir, ce nombre doit être pair : 37 LED par rangée = 74 au total.");
  add("Enter the total number of physical LEDs (1–256).","Vul het totale aantal fysieke leds in (1–256).","Geben Sie die Gesamtzahl physischer LEDs ein (1–256).","Saisissez le nombre total de LED physiques (1–256).");
  add("Enter a total of 1–256 LEDs.","Vul in totaal 1–256 leds in.","Geben Sie insgesamt 1–256 LEDs ein.","Saisissez un total de 1 à 256 LED.");
  add("Double mirrored requires an even total. For 37 LEDs per row, enter 74.","Dubbel gespiegeld vereist een even totaal. Bij 37 leds per rij vul je 74 in.","Doppelt gespiegelt erfordert eine gerade Gesamtzahl. Bei 37 LEDs pro Reihe geben Sie 74 ein.","Le mode double miroir nécessite un total pair. Pour 37 LED par rangée, saisissez 74.");
  add("The 8 × 32 matrix layout requires exactly 256 LEDs.","Voor de 8 × 32-matrix zijn precies 256 leds vereist.","Die 8 × 32-Matrix erfordert genau 256 LEDs.","La matrice 8 × 32 nécessite exactement 256 LED.");
  add("LED layout (0 single, 1 double, 2 matrix)","LED-indeling (0 enkel, 1 dubbel, 2 matrix)","LED-Anordnung (0 einzeln, 1 doppelt, 2 Matrix)","Disposition LED (0 simple, 1 double, 2 matrice)");
  add("Orange effect (1–40)","Oranje effect (1–40)","Orangeeffekt (1–40)","Effet orange (1–40)");
  add("Orange brightness (%)","Oranje helderheid (%)","Orange Helligkeit (%)","Luminosité orange (%)");
  add("Orange hue (0–100)","Oranje kleurtint (0–100)","Oranger Farbton (0–100)","Teinte orange (0–100)");
  add("Animation speed (25–200%)","Animatiesnelheid (25–200%)","Animationsgeschwindigkeit (25–200 %)","Vitesse d’animation (25–200 %)");
  add("Afterglow (0–100)","Nagloeien (0–100)","Nachleuchten (0–100)","Rémanence (0–100)");
  add("Color order (0–5)","Kleurvolgorde (0–5)","Farbreihenfolge (0–5)","Ordre des couleurs (0–5)");
  add("Buzzer (0 continuous, 1 pulsed)","Zoemer (0 continu, 1 pulserend)","Summer (0 dauerhaft, 1 pulsierend)","Buzzer (0 continu, 1 pulsé)");
  add("Buzzer duration (seconds)","Zoemertijd (seconden)","Summerdauer (Sekunden)","Durée du buzzer (secondes)");
  add("Pulse ON (milliseconds)","Puls AAN (milliseconden)","Impuls EIN (Millisekunden)","Impulsion ON (millisecondes)");
  add("Pulse OFF (milliseconds)","Puls UIT (milliseconden)","Impuls AUS (Millisekunden)","Impulsion OFF (millisecondes)");
  add("NORMAL: steady green. ALARM: saved orange effect and buzzer. ALL OFF is allowed only when NORMAL is confirmed.\n\nBluetooth: the owning phone reconnects automatically. The controller reserves ownership for up to 30 minutes after connection loss.\n\nAfter reinstalling the app, wait for the previous owner reservation to expire, or disconnect the previous app. The saved lighting settings remain in the controller.\n\nThe device is a visual and audible warning system, not a certified gas safety system.","NORMAAL: continu groen. ALARM: opgeslagen oranje effect en zoemer. Alles uitschakelen kan alleen als NORMAAL bevestigd is.\n\nBluetooth: de telefoon die eigenaar is verbindt automatisch opnieuw. De controller bewaart het eigenaarschap maximaal 30 minuten na verbindingsverlies.\n\nNa herinstallatie wacht je tot de eerdere reservering verloopt, of verbreek je de verbinding in de oude app. De opgeslagen lichtinstellingen blijven in de controller.\n\nHet apparaat is een visueel en hoorbaar waarschuwingssysteem, geen gecertificeerd gasveiligheidssysteem.","NORMAL: dauerhaft grün. ALARM: gespeicherter Orangeeffekt und Summer. Alles ausschalten ist nur im bestätigten NORMAL-Modus möglich.\n\nBluetooth: Das Besitzertelefon verbindet sich automatisch erneut. Die Besitzerreservierung bleibt nach Verbindungsverlust bis zu 30 Minuten bestehen.\n\nNach einer Neuinstallation warten, bis die alte Reservierung abläuft, oder die Verbindung in der alten App trennen. Die gespeicherten Lichteinstellungen bleiben im Controller.\n\nDas Gerät ist ein optisches und akustisches Warnsystem, kein zertifiziertes Gassicherheitssystem.","NORMAL : vert fixe. ALARME : effet orange enregistré et avertisseur sonore. Tout éteindre est uniquement possible lorsque NORMAL est confirmé.\n\nBluetooth : le téléphone propriétaire se reconnecte automatiquement. Le contrôleur conserve la réservation jusqu’à 30 minutes après une déconnexion.\n\nAprès réinstallation, attendre l’expiration de l’ancienne réservation ou déconnecter l’ancienne application. Les paramètres d’éclairage restent enregistrés dans le contrôleur.\n\nL’appareil est un système d’avertissement visuel et sonore, pas un dispositif certifié de sécurité gaz.");
  add("License required · HP Gas Alert v5.0","License vereist · HP Gas Alert v5.0","Lizenz erforderlich · HP Gas Alert v5.0","Licence requise · HP Gas Alert v5.0");
  add("Lamp control and settings are locked.","Lampbediening en instellingen zijn geblokkeerd.","Lampensteuerung und Einstellungen sind gesperrt.","La commande des lumières et les paramètres sont bloqués.");
  add("Status:","Status:","Status:","Statut :");
  add("Activate a valid license, start an available trial, or purchase a license from HB-Electronics.","Activeer een geldige licentie, start een beschikbare proefperiode of koop een licentie bij HB-Electronics.","Aktivieren Sie eine gültige Lizenz, starten Sie eine verfügbare Testphase oder kaufen Sie eine Lizenz bei HB-Electronics.","Activez une licence valide, démarrez un essai disponible ou achetez une licence auprès de HB-Electronics.");
  add("Activate license","Licentie activeren","Lizenz aktivieren","Activer la licence");
  add("Buy license","Licentie kopen","Lizenz kaufen","Acheter une licence");
  add("Check license","Licentie controleren","Lizenz prüfen","Vérifier la licence");
  add("Start trial","Proefperiode starten","Testphase starten","Démarrer l’essai");
  add("Buy license at HB-Electronics.nl","Licentie kopen bij HB-Electronics.nl","Lizenz bei HB-Electronics.nl kaufen","Acheter une licence sur HB-Electronics.nl");
  add("Enter license code","Licentiecode invoeren","Lizenzcode eingeben","Saisir le code de licence");
  add("Enter your license code first.","Vul eerst je licentiecode in.","Geben Sie zuerst Ihren Lizenzcode ein.","Saisissez d’abord votre code de licence.");
  add("License server is checking…","Licentieserver controleren…","Lizenzserver wird geprüft…","Vérification par le serveur de licences…");
  add("License check in progress.","Licentiecontrole is al bezig.","Lizenzprüfung läuft bereits.","Vérification de licence déjà en cours.");
  add("License management · v5.0","Licentiebeheer · v5.0","Lizenzverwaltung · v5.0","Gestion des licences · v5.0");
  add("License checked","Licentie gecontroleerd","Lizenz geprüft","Licence vérifiée");
  add("License check failed","Licentiecontrole mislukt","Lizenzprüfung fehlgeschlagen","Échec de vérification de la licence");
  add("License revoked","Licentie ingetrokken","Lizenz widerrufen","Licence révoquée");
  add("License blocked","Licentie geblokkeerd","Lizenz gesperrt","Licence bloquée");
  add("License or trial expired","Licentie of proefperiode verlopen","Lizenz oder Testphase abgelaufen","Licence ou essai expiré");
  add("Reactivation required","Heractivatie vereist","Erneute Aktivierung erforderlich","Réactivation requise");
  add("Invalid or expired license code","Licentiecode ongeldig of verlopen","Lizenzcode ungültig oder abgelaufen","Code de licence invalide ou expiré");
  add("Not activated","Nog niet geactiveerd","Nicht aktiviert","Non activée");
  add("License expired or online check required","Licentie verlopen of online controle nodig","Lizenz abgelaufen oder Onlineprüfung erforderlich","Licence expirée ou vérification en ligne nécessaire");
  add("License active","Licentie actief","Lizenz aktiv","Licence active");
  add("Trial active","Proefperiode actief","Testphase aktiv","Essai actif");
  add("valid until","geldig tot","gültig bis","valable jusqu’au");
  add("Tap for license management","Tik voor licentiebeheer","Für Lizenzverwaltung tippen","Toucher pour gérer la licence");
  add("Last known status:","Laatste bekende status:","Letzter bekannter Status:","Dernier état connu :");
  add("Online check not confirmed.","Online controle niet bevestigd.","Onlineprüfung nicht bestätigt.","Vérification en ligne non confirmée.");
  add("License checked: activated.","Licentie gecontroleerd: geactiveerd.","Lizenz geprüft: aktiviert.","Licence vérifiée : activée.");
  add("License checked: trial.","Licentie gecontroleerd: proefperiode.","Lizenz geprüft: Testphase.","Licence vérifiée : période d’essai.");
  add("This license has been revoked by HB-Electronics and is no longer valid. Contact HB-Electronics if you have questions.","Deze licentie is ingetrokken door HB-Electronics en is niet meer geldig. Neem bij vragen contact op met HB-Electronics.","Diese Lizenz wurde von HB-Electronics widerrufen und ist nicht mehr gültig. Wenden Sie sich bei Fragen an HB-Electronics.","Cette licence a été révoquée par HB-Electronics et n’est plus valide. Contactez HB-Electronics pour toute question.");
  add("This license is blocked. Contact HB-Electronics.","Deze licentie is geblokkeerd. Neem contact op met HB-Electronics.","Diese Lizenz ist gesperrt. Wenden Sie sich an HB-Electronics.","Cette licence est bloquée. Contactez HB-Electronics.");
  add("The license or trial has expired. Activate a valid license to continue.","De licentie of proefperiode is verlopen. Activeer een geldige licentie om verder te gaan.","Die Lizenz oder Testphase ist abgelaufen. Aktivieren Sie eine gültige Lizenz.","La licence ou la période d’essai a expiré. Activez une licence valide.");
  add("This product is currently unavailable on the license server.","Dit product is momenteel niet beschikbaar op de licentieserver.","Dieses Produkt ist derzeit auf dem Lizenzserver nicht verfügbar.","Ce produit est actuellement indisponible sur le serveur de licences.");
  add("This installation requires reactivation. Enter your valid license code.","Deze installatie moet opnieuw worden geactiveerd. Voer je geldige licentiecode in.","Diese Installation muss erneut aktiviert werden. Geben Sie Ihren gültigen Lizenzcode ein.","Cette installation doit être réactivée. Saisissez votre code de licence valide.");
  add("This installation is not registered. Start the trial or activate a license.","Deze app-installatie is nog niet geregistreerd. Start de proefperiode of activeer een licentie.","Diese App-Installation ist noch nicht registriert. Starten Sie die Testphase oder aktivieren Sie eine Lizenz.","Cette installation n’est pas enregistrée. Démarrez l’essai ou activez une licence.");
  add("The maximum number of devices has been reached.","Het maximum aantal toegestane apparaten voor deze licentie is bereikt.","Die maximale Anzahl zulässiger Geräte wurde erreicht.","Le nombre maximal d’appareils autorisés est atteint.");
  add("The entered license code is invalid or expired.","De ingevoerde licentiecode is ongeldig of verlopen.","Der eingegebene Lizenzcode ist ungültig oder abgelaufen.","Le code de licence saisi est invalide ou expiré.");
  add("The license server is busy. Please try again later.","De licentieserver ontvangt te veel verzoeken. Probeer het later opnieuw.","Der Lizenzserver ist ausgelastet. Bitte versuchen Sie es später erneut.","Le serveur de licences est occupé. Réessayez plus tard.");
  add("The license could not be checked. Please try again later.","De licentie kon niet worden gecontroleerd. Probeer het later opnieuw.","Die Lizenz konnte nicht geprüft werden. Bitte versuchen Sie es später erneut.","La licence n’a pas pu être vérifiée. Réessayez plus tard.");
  add("License check failed. Check your internet connection and try again.","Licentiecontrole niet gelukt. Controleer je internetverbinding en probeer opnieuw.","Lizenzprüfung fehlgeschlagen. Prüfen Sie Ihre Internetverbindung und versuchen Sie es erneut.","Échec de vérification. Vérifiez votre connexion Internet et réessayez.");
  add("Unable to open the HB-Electronics webshop.","Kan de HB-Electronics webshop niet openen.","Der HB-Electronics-Shop konnte nicht geöffnet werden.","Impossible d’ouvrir la boutique HB-Electronics.");
  add("Controller not connected","Controller niet verbonden","Controller nicht verbunden","Contrôleur non connecté");
  add("The app remembers a previously selected controller, but Bluetooth is currently disconnected.\n\nTry reconnecting, or choose another controller. This changes only the saved selection on this phone, not the ownership reservation on the ESP32.","De app heeft nog een eerder geselecteerde controller opgeslagen, maar heeft momenteel geen Bluetooth-verbinding.\n\nProbeer opnieuw te verbinden of kies een andere controller. Dit wist alleen de opgeslagen selectie op de telefoon, niet de eigenaarsreservering op de ESP32.","Die App hat einen zuvor ausgewählten Controller gespeichert, aber Bluetooth ist derzeit getrennt.\n\nVersuchen Sie erneut zu verbinden oder wählen Sie einen anderen Controller. Dies ändert nur die Auswahl auf dem Telefon, nicht die Besitzerreservierung auf dem ESP32.","L’application a mémorisé un contrôleur, mais le Bluetooth est actuellement déconnecté.\n\nReconnectez-vous ou choisissez un autre contrôleur. Seule la sélection sur le téléphone est modifiée, pas la réservation du propriétaire sur l’ESP32.");
  add("Reconnect","Opnieuw verbinden","Erneut verbinden","Reconnecter");
  add("Choose another controller","Andere controller kiezen","Anderen Controller wählen","Choisir un autre contrôleur");
  add("Release controller","Controller vrijgeven","Controller freigeben","Libérer le contrôleur");
  add("Release the controller for another phone and disconnect? Current LED and alarm state will not change.","Controller vrijgeven voor een andere telefoon en verbinding verbreken? De huidige led- en alarmstatus verandert niet.","Controller für ein anderes Telefon freigeben und trennen? LED- und Alarmzustand bleiben unverändert.","Libérer le contrôleur pour un autre téléphone et déconnecter ? L’état des LED et de l’alarme reste inchangé.");
  add("Release & disconnect","Vrijgeven en verbreken","Freigeben und trennen","Libérer et déconnecter");
  add("Bluetooth is not supported","Bluetooth wordt niet ondersteund","Bluetooth wird nicht unterstützt","Bluetooth n’est pas pris en charge");
  add("Bluetooth unavailable","Bluetooth niet beschikbaar","Bluetooth nicht verfügbar","Bluetooth indisponible");
  add("Turn on Bluetooth and try again.","Schakel Bluetooth in en probeer opnieuw.","Schalten Sie Bluetooth ein und versuchen Sie es erneut.","Activez le Bluetooth et réessayez.");
  add("Search for HP Gas Alert controller","Zoek HP Gas Alert-controller","HP Gas Alert-Controller suchen","Rechercher un contrôleur HP Gas Alert");
  add("Search again","Opnieuw zoeken","Erneut suchen","Rechercher à nouveau");
  add("Search for HP Gas Alert controller…","Zoeken naar HP Gas Alert-controller…","Suche nach HP Gas Alert-Controller…","Recherche du contrôleur HP Gas Alert…");
  add("No controller found. Check the ESP32-C3 and close the v3 app.","Geen controller gevonden. Controleer de ESP32-C3 en sluit de v3-app.","Kein Controller gefunden. Prüfen Sie den ESP32-C3 und schließen Sie die v3-App.","Aucun contrôleur trouvé. Vérifiez l’ESP32-C3 et fermez l’application v3.");
  add("Tap 'Search again' to scan again.","Kies 'Opnieuw zoeken' om nogmaals te scannen.","Tippen Sie auf 'Erneut suchen', um erneut zu suchen.","Touchez 'Rechercher à nouveau' pour relancer la recherche.");
  add("Bluetooth search failed. Tap 'Search again'.","Bluetooth zoeken mislukt. Kies 'Opnieuw zoeken'.","Bluetooth-Suche fehlgeschlagen. Tippen Sie auf 'Erneut suchen'.","Échec de la recherche Bluetooth. Touchez 'Rechercher à nouveau'.");
  add("Enter a valid number","Vul een geldig getal in","Geben Sie eine gültige Zahl ein","Saisissez un nombre valide");
  add("Controller is not connected.","Controller is niet verbonden.","Controller ist nicht verbunden.","Le contrôleur n’est pas connecté.");
  add("Settings can only be changed in NORMAL (green). Reset the orange alarm first.","Instellingen kunnen alleen worden gewijzigd in NORMAAL (groen). Reset eerst het oranje alarm.","Einstellungen können nur im Modus NORMAL (grün) geändert werden. Setzen Sie zuerst den orangefarbenen Alarm zurück.","Les paramètres ne peuvent être modifiés qu’en mode NORMAL (vert). Réinitialisez d’abord l’alarme orange.");
  add("Settings are blocked while the orange alarm is active. Reset the alarm to NORMAL (green) before changing settings.","Instellingen zijn geblokkeerd tijdens het oranje alarm. Reset eerst naar NORMAAL (groen).","Einstellungen sind während des orangefarbenen Alarms gesperrt. Setzen Sie den Alarm zuerst auf NORMAL (grün) zurück.","Les paramètres sont bloqués pendant l’alarme orange. Réinitialisez d’abord en mode NORMAL (vert).");
  add("Controller busy or not ready to save. Please retry.","Controller is bezig of niet klaar om op te slaan. Probeer opnieuw.","Controller beschäftigt oder nicht bereit zum Speichern. Versuchen Sie es erneut.","Contrôleur occupé ou pas prêt à enregistrer. Réessayez.");
  add("The controller confirmed the setting in NORMAL (green).","De controller heeft de instelling bevestigd in NORMAAL (groen).","Der Controller hat die Einstellung im Modus NORMAL (grün) bestätigt.","Le contrôleur a confirmé le paramètre en mode NORMAL (vert).");
  add("Value unavailable","Waarde niet beschikbaar","Wert nicht verfügbar","Valeur indisponible");
  add("Single","Enkel","Einzeln","Simple");
  add("Double mirrored","Dubbel gespiegeld","Doppelt gespiegelt","Double miroir");
  add("Continuous","Continu","Dauerhaft","Continu");
  add("Pulsed","Pulserend","Pulsierend","Pulsé");
  add("Single strip","Enkele ledstrip","Einzelner LED-Streifen","Ruban LED simple");
  add("Double mirrored strip","Dubbele gespiegelde ledstrip","Doppelter gespiegelter LED-Streifen","Ruban LED double en miroir");
  add("8 × 32 matrix","8 × 32 matrix","8 × 32 Matrix","Matrice 8 × 32");
  add("Effect","Effect","Effekt","Effet");
  add("LEDs","leds","LEDs","LED");
  add("Controller confirmed · state revision","Controller bevestigd · statusrevisie","Controller bestätigt · Statusrevision","Contrôleur confirmé · révision d’état");
  add("Disconnecting…","Verbinding verbreken…","Verbindung wird getrennt…","Déconnexion…");
  add("Made by HB-Electronics.nl · © 2026","Gemaakt door HB-Electronics.nl · © 2026","Erstellt von HB-Electronics.nl · © 2026","Créé par HB-Electronics.nl · © 2026");
  add("Release not confirmed. Controller ownership may remain reserved. Reconnect and retry.","Vrijgave niet bevestigd. Eigenaarschap kan nog gereserveerd zijn. Verbind opnieuw.","Freigabe nicht bestätigt. Die Besitzerreservierung kann bestehen bleiben. Erneut verbinden.","Libération non confirmée. La réservation peut subsister. Reconnectez-vous.");
  add("No confirmation received from controller.","Geen bevestiging ontvangen van controller.","Keine Bestätigung vom Controller erhalten.","Aucune confirmation reçue du contrôleur.");
  add("Controller is disconnecting.","Verbinding met controller wordt verbroken.","Controller wird getrennt.","Déconnexion du contrôleur en cours.");
  add("Opnieuw verbinding maken met geselecteerde controller…","Opnieuw verbinding maken met geselecteerde controller…","Erneute Verbindung zum ausgewählten Controller…","Reconnexion au contrôleur sélectionné…");
  add("Bluetooth-selectie opnieuw instellen.","Bluetooth-selectie opnieuw instellen.","Bluetooth-Auswahl zurücksetzen.","Réinitialiser la sélection Bluetooth.");
  add("Opgeslagen Bluetooth-selectie gewist. Kies de controller opnieuw.","Opgeslagen Bluetooth-selectie gewist. Kies de controller opnieuw.","Gespeicherte Bluetooth-Auswahl gelöscht. Controller erneut auswählen.","Sélection Bluetooth effacée. Sélectionnez à nouveau le contrôleur.");
  add("Owner cannot be released offline. Reconnect to the controller first.","Eigenaarschap kan niet offline worden vrijgegeven. Verbind eerst opnieuw.","Besitzer kann offline nicht freigegeben werden. Zuerst erneut verbinden.","Impossible de libérer le propriétaire hors ligne. Reconnectez-vous d’abord.");
  add("Releasing controller ownership…","Eigenaarschap van controller vrijgeven…","Controller-Besitzer wird freigegeben…","Libération du contrôleur…");
  add("Disconnected — ownership released","Verbinding verbroken · eigenaarschap vrijgegeven","Getrennt · Besitzer freigegeben","Déconnecté · propriétaire libéré");
  add("Connection error","Verbindingsfout","Verbindungsfehler","Erreur de connexion");
  add("Bluetooth connection interrupted before confirmation.","Bluetooth-verbinding verbroken vóór bevestiging.","Bluetooth-Verbindung vor Bestätigung unterbrochen.","Connexion Bluetooth interrompue avant confirmation.");
  add("Link lost before release confirmation; reconnecting to check ownership","Verbinding verbroken vóór bevestigde vrijgave; opnieuw verbinden om eigenaarschap te controleren","Verbindung vor Freigabebestätigung verloren; erneute Verbindung zur Kontrolle","Connexion perdue avant confirmation de libération ; reconnexion pour vérifier.");
  add("Old owner still reserved — automatic retry (up to 30 min)","Vorige eigenaar nog gereserveerd · automatisch opnieuw proberen (max. 30 minuten)","Vorheriger Besitzer noch reserviert · automatischer neuer Versuch (bis 30 Min.)","Ancien propriétaire toujours réservé · nouvelle tentative automatique (jusqu’à 30 min)");
  add("Gas Alert BLE service not found","Gas Alert-BLE-service niet gevonden","Gas Alert-BLE-Dienst nicht gefunden","Service BLE Gas Alert introuvable");
  add("Incomplete Gas Alert firmware protocol","Gas Alert-firmwareprotocol onvolledig","Gas Alert-Firmwareprotokoll unvollständig","Protocole du micrologiciel Gas Alert incomplet");
  add("BLE service found - authorizing phone","BLE-service gevonden · telefoon autoriseren","BLE-Dienst gefunden · Telefon wird autorisiert","Service BLE trouvé · autorisation du téléphone");
  add("Notification unavailable - status polling fallback","Meldingen niet beschikbaar · status wordt actief opgevraagd","Benachrichtigungen nicht verfügbar · Status wird abgefragt","Notifications indisponibles · lecture périodique de l’état");
  add("Notification subscription unsupported - polling fallback","Aanmelden voor meldingen niet ondersteund · status wordt opgevraagd","Benachrichtigungsabo nicht unterstützt · Statusabfrage","Abonnement aux notifications non pris en charge · lecture périodique");
  add("Reading controller…","Controller uitlezen…","Controller wird gelesen…","Lecture du contrôleur…");
  add("Notifications unavailable - using polling","Meldingen niet beschikbaar · status wordt opgevraagd","Benachrichtigungen nicht verfügbar · Statusabfrage","Notifications indisponibles · interrogation périodique");
  add("Failed to read the setting back from controller.","Opgeslagen instelling kon niet worden teruggelezen.","Einstellung konnte nicht vom Controller zurückgelesen werden.","Impossible de relire le paramètre depuis le contrôleur.");
  add("Authorization sent - reading controller","Autorisatie verzonden · controller uitlezen","Autorisierung gesendet · Controller wird gelesen","Autorisation envoyée · lecture du contrôleur");
  add("License invalid. Setting not saved.","Licentie ongeldig. Instelling niet opgeslagen.","Lizenz ungültig. Einstellung nicht gespeichert.","Licence invalide. Paramètre non enregistré.");
  add("License required · control locked","Licentie vereist · bediening geblokkeerd","Lizenz erforderlich · Steuerung gesperrt","Licence requise · commande bloquée");
  add("BLE rejected the settings command.","BLE heeft het instellingencommando geweigerd.","BLE hat den Einstellungsbefehl abgelehnt.","BLE a refusé la commande de paramètres.");
  add("Sending","Verzenden","Senden","Envoi");
  add("Bluetooth disconnected","Bluetooth niet verbonden","Bluetooth getrennt","Bluetooth déconnecté");
  add("Bluetooth reconnecting","Bluetooth opnieuw verbinden","Bluetooth wird erneut verbunden","Reconnexion Bluetooth");
  add("Authentication error","Autorisatiefout","Autorisierungsfehler","Erreur d’autorisation");
  add("Command write failed","Commando verzenden mislukt","Befehl konnte nicht gesendet werden","Échec de l’envoi de la commande");
  add("Owner release failed","Eigenaarschap vrijgeven mislukt","Besitzerfreigabe fehlgeschlagen","Échec de la libération du propriétaire");
  add("BLE read error","BLE-leesfout","BLE-Lesefehler","Erreur de lecture BLE");
  add("BLE write failed","BLE-schrijffout","BLE-Schreibfehler","Échec d’écriture BLE");
  add("Unable to send command","Commando verzenden mislukt","Befehl konnte nicht gesendet werden","Impossible d’envoyer la commande");
  add("Reconnecting to selected controller…","Opnieuw verbinding maken met geselecteerde controller…","Erneute Verbindung zum ausgewählten Controller…","Reconnexion au contrôleur sélectionné…");
  add("Resetting Bluetooth selection.","Bluetooth-selectie opnieuw instellen.","Bluetooth-Auswahl zurücksetzen.","Réinitialisation de la sélection Bluetooth.");
  add("Saved Bluetooth selection cleared. Select the controller again.","Opgeslagen Bluetooth-selectie gewist. Kies de controller opnieuw.","Gespeicherte Bluetooth-Auswahl gelöscht. Controller erneut auswählen.","Sélection Bluetooth effacée. Sélectionnez à nouveau le contrôleur.");
  add("Invalid license. Setting not saved.","Licentie niet geldig. Instelling niet opgeslagen.","Ungültige Lizenz. Einstellung nicht gespeichert.","Licence invalide. Paramètre non enregistré.");
  add("License required · controls locked","Licentie vereist · bediening geblokkeerd","Lizenz erforderlich · Steuerung gesperrt","Licence requise · commandes bloquées");
 }
 public static String code(Context context){
  String code=context.getSharedPreferences("ui_preferences",Context.MODE_PRIVATE).getString("language","en");
  for(String allowed:CODES)if(allowed.equals(code))return code;
  return "en";
 }
 public static int selected(Context context){
  String code=code(context);for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return i;
  return 0;
 }
 public static void save(Context context,int index){
  if(index<0||index>=CODES.length)return;
  context.getSharedPreferences("ui_preferences",Context.MODE_PRIVATE).edit().putString("language",CODES[index]).apply();
 }
 public static String t(Context c,String english){
  if(english==null)return "";
  int selected=selected(c);
  if(selected==0)return english;
  String[] translations=LINES.get(english);
  return translations==null?english:translations[selected-1];
 }
 /** Server API and BLE protocol use fixed, language-neutral tokens; only their display text is translated. */
 public static String licenseStatus(Context c,String status){
  if(status==null)return t(c,"Not activated");
  String[] nl={"Licentie ingetrokken","Licentie geblokkeerd","Licentie of proefperiode verlopen","Heractivatie vereist","Licentiecode ongeldig of verlopen","Nog niet geactiveerd","Licentie verlopen of online controle nodig"};
  String[] en={"License revoked","License blocked","License or trial expired","Reactivation required","Invalid or expired license code","Not activated","License expired or online check required"};
  for(int i=0;i<nl.length;i++)if(nl[i].equals(status))return t(c,en[i]);
  if(status.startsWith("Licentie actief · geldig tot "))return t(c,"License active")+" · "+t(c,"valid until")+" "+status.substring("Licentie actief · geldig tot ".length());
  if(status.startsWith("Proefperiode actief · geldig tot "))return t(c,"Trial active")+" · "+t(c,"valid until")+" "+status.substring("Proefperiode actief · geldig tot ".length());
  return t(c,"The license could not be checked. Please try again later.");
 }
 public static String licenseMessage(Context c,String message){
  if(message==null)return t(c,"The license could not be checked. Please try again later.");
  if(message.startsWith("Licentiecontrole niet gelukt:"))return t(c,"License check failed. Check your internet connection and try again.");
  String[] original={
   "Licentie gecontroleerd: geactiveerd.","Licentie gecontroleerd: proefperiode.",
   "Deze licentie is ingetrokken door HB-Electronics en is niet meer geldig. Neem bij vragen contact op met HB-Electronics.",
   "Deze licentie is geblokkeerd. Neem contact op met HB-Electronics.",
   "De licentie of proefperiode is verlopen. Activeer een geldige licentie om verder te gaan.",
   "Dit product is momenteel niet beschikbaar op de licentieserver.",
   "Deze installatie moet opnieuw worden geactiveerd. Voer je geldige licentiecode in.",
   "Deze app-installatie is nog niet geregistreerd. Start de proefperiode of activeer een licentie.",
   "Het maximum aantal toegestane apparaten voor deze licentie is bereikt.",
   "De ingevoerde licentiecode is ongeldig of verlopen.",
   "De licentieserver ontvangt te veel verzoeken. Probeer het later opnieuw.",
   "De licentie kon niet worden gecontroleerd. Probeer het later opnieuw."
  };
  String[] english={
   "License checked: activated.","License checked: trial.",
   "This license has been revoked by HB-Electronics and is no longer valid. Contact HB-Electronics if you have questions.",
   "This license is blocked. Contact HB-Electronics.",
   "The license or trial has expired. Activate a valid license to continue.",
   "This product is currently unavailable on the license server.",
   "This installation requires reactivation. Enter your valid license code.",
   "This installation is not registered. Start the trial or activate a license.",
   "The maximum number of devices has been reached.",
   "The entered license code is invalid or expired.",
   "The license server is busy. Please try again later.",
   "The license could not be checked. Please try again later."
  };
  for(int i=0;i<original.length;i++)if(original[i].equals(message))return t(c,english[i]);
  return t(c,"The license could not be checked. Please try again later.");
 }
 public static String ble(Context c,String message){
  if(message==null)return t(c,"Bluetooth not connected");
  if(message.startsWith("Signal: ")){
   String v=message.substring(8);
   if(v.equals("unavailable (disconnected)"))return t(c,"Signal: unavailable (disconnected)");
   if(v.equals("measuring…"))return t(c,"Signal: measuring…");
   if(v.endsWith(" (last measurement)"))return t(c,"Signal:")+" "+v.replace(" (last measurement)","")+" "+t(c,"(last measurement)");
   return t(c,"Signal:")+" "+v;
  }
  if(message.startsWith("Connecting to "))return t(c,"Connect Bluetooth")+" · "+message.substring(14);
  if(message.startsWith("Scan error ")||message.startsWith("Bluetooth scan failed"))return t(c,"Bluetooth search failed. Tap 'Search again'.");
  if(message.startsWith("Bluetooth link disconnected"))return t(c,"Bluetooth link disconnected — automatic reconnect");
  if(message.startsWith("Bluetooth connected · waiting"))return t(c,"Bluetooth connected · waiting for fresh controller status");
  if(message.startsWith("Bluetooth connected"))return t(c,"Bluetooth connected · controller confirmed");
  if(message.startsWith("Connection error: "))return t(c,"Connection error")+": "+message.substring("Connection error: ".length());
  if(message.startsWith("Bluetooth disconnected ("))return t(c,"Bluetooth disconnected");
  if(message.startsWith("BLE read error "))return t(c,"BLE read error")+" "+message.substring("BLE read error ".length());
  if(message.startsWith("BLE write failed ("))return t(c,"BLE write failed")+" "+message.substring("BLE write failed ".length());
  if(message.startsWith("BLE authorization failed (")||message.startsWith("BLE authorization write error ("))
   return t(c,"Authentication error");
  if(message.startsWith("Command write failed: "))return t(c,"Command write failed")+" "+message.substring("Command write failed: ".length());
  if(message.startsWith("Owner release failed ("))return t(c,"Owner release failed");
  if(message.startsWith("Unable to send command ("))return t(c,"Unable to send command");
  if(message.startsWith("Sending "))return t(c,"Sending")+" "+message.substring(8);
  if(message.equals("Opnieuw verbinding maken met geselecteerde controller…"))return t(c,"Reconnecting to selected controller…");
  if(message.equals("Bluetooth-selectie opnieuw instellen."))return t(c,"Resetting Bluetooth selection.");
  if(message.equals("Opgeslagen Bluetooth-selectie gewist. Kies de controller opnieuw."))return t(c,"Saved Bluetooth selection cleared. Select the controller again.");
  if(message.equals("Licentie niet geldig. Instelling niet opgeslagen."))return t(c,"Invalid license. Setting not saved.");
  if(message.equals("Licentie vereist · bediening geblokkeerd"))return t(c,"License required · controls locked");
  if(message.startsWith("Licentie niet geldig."))return t(c,"License invalid. Setting not saved.");
  if(message.startsWith("Licentie vereist"))return t(c,"License required · control locked");
  return t(c,message);
 }
}
