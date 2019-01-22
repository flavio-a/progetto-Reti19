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

## Architettura
Il server utilizza multithreading e multiplexing per gestire più client
contemporaneamente, con un'architettura uguale a quella di Chatty (progetto di
SO). Il multithreading viene utilizzato perché la risposta a quasi tutte le
operazioni richiede un'interazione con il filesystem (lento rispetto al
processore).

I parallelismi con Chatty sono i seguenti:
- listener = main thread (TURINGServer)
- workers = ConnectionHandler nella threadpool
- fd = SocketChannel
- coda condivisa queue = coda interna della threadpool
- something to send SocketChannel (fd) back to the main thread (listener)
    (interrupt of the thread + shared queue): freesc
- mappa fd -> username = hashtable socket_to_user
- mappa username -> fd = hashtable user_to_socket

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
a correttezza della password (che deve essere uguale al contenuto del file).

## Protocolli di comunicazione
### Messagi TCP
Il client invia messaggi al server tramite TCP. I messaggi iniziano con 1 byte,
che specifica il tipo di richiesta, seguito dai parametri della richiesta. Il
server risponde a tutte le richieste con un ack, seguito da eventuali altri
dati della risposta. In caso di errore l'ack è un codice di errore. I dati
primitivi sono inviati as-is, le stringhe iniziano con un `int` (4 byte) che ne
specifica la lunghezza, seguito dai byte della stringa.

Le possibile richieste, con i rispettivi parametri, sono:
- `OP_LOGIN`(string, string) i parametri sono (nell'ordine) username e password.
    Un login viene associato ad un SocketChannel; tutte le richieste su quel
    socket vengono eseguite con il login. Un'operazione di login su un socket
    già connesso fallisce.
- `OP_CREATE`(string, int) i parametri sono il nome del documento e il numero di
    sezioni.
- `OP_EDIT`(string, int) i parametri sono il nome completo del documento
    (ie: owner/name) e il numero della sessione

Le possibili risposte sono:
- `RESP_OK` operazione eseguita con successo
- `ERR_RETRY` operazione fallita, riprovare tra poco
- `ERR_UNKNOWN_OP` richiesta un'operazione sconosciuta
- `ERR_UNLOGGED` richiesta un'operazione diversa da login su un socket non
    loggato
- `ERR_INVALID_LOGIN` credenziali errate
- `ERR_USERNAME_BUSY` username già connesso su un altro socket
- `ERR_ALREADY_LOGGED` operazione di login in un socket già connesso

Il primo messaggio di una comunicazione deve essere di `LOGIN`, altrimenti il
server chiude subito la connessione rispondendo con un `ERR_UNLOGGED`.

### Trasferimento sezioni
Per trasferire una sezione (ie: un file) si invia un `long` (8 byte) con la
dimensione del file in byte, seguito dai byte del file

# TODO
Primo tick fatto, secondo tick testato
- [ ] Operazioni sul db:
  - [x] [x] Registrazione
  - [x] [x] Login
  - [x] [x] Creare un documento
  - [x] [x] Iniziare la modifica di una sezione
  - [x] [x] Finire la modifica
  - [x] [ ] Invitare
  - [ ] [ ] Mostrare una sezione
  - [ ] [ ] Mostrare un documento
  - [ ] [ ] Listare i documenti editabili
- [ ] Operazioni esportate in rete:
  - [x] [x] Registrazione
  - [ ] [ ] Login
  - [ ] [ ] Creare un documento
  - [ ] [ ] Modificare una sezione
  - [ ] [ ] Finire la modifica
  - [ ] [ ] Invitare
  - [ ] [ ] Mostrare una sezione
  - [ ] [ ] Mostrare un documento
  - [ ] [ ] Listare i documenti editabili
- [ ] [ ] Chat
