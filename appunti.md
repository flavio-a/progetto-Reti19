# Appunti
Questi sono appunti presi durante la realizzazione di TURING per documentare le
scelte di progetto. La relazione verrà scritta anche sulla base di questi
appunti.

- Nella directory `server/lib` si trovano tutti sorgenti condivisi tra client e
server (ie: intefacce).

## Storage
Il server tiene una cartella per ogni utente, al cui interno si trovano:
- `user_info`: file di informazioni sull'utente. La prima riga contiene la
    password (rigorosamente in chiaro perché abbiamo a cuore la sicurezza), le
    successive contengono, uno per riga, gli inivit pendenti (ie: da notificare)
- una cartella per ogni suo documento con lo stesso nome del documento:
  - `docs_info`: elenco degli utenti che possono modificarlo, uno per riga
  - `sectionN`: contenuto dell'N-esima sezione (come richiesto dalla consegna)

La gestione della sincronizzazione (ie: garantire che ogni sezione sia
modificata da al più un utente alla volta) non avviene salvando informazioni su
file ma solo nella memoria del programma. Questo significa che in caso di crash
del server tutte le sezioni aperte vengono automaticamente chiuse senza salvare
nessuna modifica.

## Registrazione
Come da consegna, la registrazione avviene tramite RMI, secondo l'interfaccia
`RegistrationInterface`, comune a client e server. L'esistenza di un utente è
data dall'esistenza della relativa cartella.
La creazione della directory di un utente e dei suoi file base (ie: user_info)
avviene solo durante la registrazione; la cancellazione invece non avviene mai.
Queste assunzioni aiutano a garantire la sincronizzazione.
