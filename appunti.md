# Appunti
Questi sono appunti presi durante la realizzazione di TURING per documentare le
scelte di progetto. La relazione verrà scritta anche sulla base di questi
appunti.

- Nella directory `server/lib` si trovano tutti sorgenti condivisi tra client e
server (ie: intefacce).
- Alcuni dati vengono salvati su file, altri in memoria. La scelta avviene
considerando se in caso di crash del server questi dati debbano essere
mantenuti o possano essere persi, seguendo queste assunzioni:
    - Registrazione, creazione di documenti, inviti, modifiche completate
    devono essere permanenti.
    - Login e modifiche in corso possono essere volatili.

## Storage
Il server tiene una cartella per ogni utente, al cui interno si trovano:
- `pwd`: password (rigorosamente in chiaro perché abbiamo a cuore la sicurezza)
    senza spazi prima o dopo (trimmed)
- `pending_invitations`: elenco degli inviti pendenti (ie: da notificare), uno
    per riga, nel formato `owner/doc_name`.
- `editable_docs`: elenco dei documenti che può modificare, uno per riga, nel
    formato `owner/doc_name`.
- una cartella per ogni suo documento con lo stesso nome del documento:
    - `editors`: elenco di chi ha i permessi di modificare questo documento,
        uno per riga.
    - `sectionN`: contenuto dell'N-esima sezione.

In caso di crash tutte le sezioni in modifica vengono chiuse come se l'utente
avesse terminato la modifica rimandando la versione originale del file.

## Registrazione
Come da consegna, la registrazione avviene tramite RMI, secondo l'interfaccia
`RegistrationInterface`, comune a client e server. L'esistenza di un utente è
data dall'esistenza della relativa cartella.
La creazione della directory di un utente e dei suoi file base (ie: user_info)
avviene solo durante la registrazione; la cancellazione invece non avviene mai.

## Login
Per il login il server controlla l'esistenza del file `DB_ROOT/usr/user_info` e
a correttezza della password (che deve essere uguale alla prima riga del file).

## Protocolli di comunicazione
### Trasferimento sezioni
Per trasferire una sezione (ie: un file) il protocollo è:
- Inviare un `long` (8 byte) con la dimensione del file in byte
- Inviare tutti i byte del file

# TODO
Primo tick fatto, secondo tick testato
- [ ] Operazioni locali da implementare:
  - [x] [x] Registrazione
  - [x] [x] login
  - [x] [x] Creare un documento
  - [x] [x] Iniziare la modifica di una sezione
  - [x] [x] Finire la modifica
  - [ ] [ ] Invitare
  - [ ] [ ] Mostrare una sezione
  - [ ] [ ] Mostrare un documento
  - [ ] [ ] Listare i documenti editabili
- [ ] Esportare servizi in rete:
  - [x] [x] Registrazione
  - [ ] [ ] Login
  - [ ] [ ] Operazioni di sopra:
    - [ ] [ ] Creare un documento
    - [ ] [ ] Modificare una sezione
    - [ ] [ ] Finire la modifica
    - [ ] [ ] Invitare
    - [ ] [ ] Mostrare una sezione
    - [ ] [ ] Mostrare un documento
    - [ ] [ ] Listare i documenti editabili
- [ ] [ ] Chat
