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
- workers = OperationHandler nella threadpool
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

## Operazioni
### Registrazione
Come da consegna, la registrazione avviene tramite RMI, secondo l'interfaccia
`RegistrationInterface`, comune a client e server. L'esistenza di un utente è
data dall'esistenza della relativa cartella.
La creazione della directory di un utente e dei suoi file base (ie: user_info)
avviene solo durante la registrazione; la cancellazione invece non avviene mai.

### Login
Per il login il server controlla l'esistenza del file `DB_ROOT/usr/user_info` e
a correttezza della password (che deve essere uguale al contenuto del file).

### Inviti
Dato che gli inviti possono essere inviati ad un client da un thread che sta
servendo un altro client, il server si assicura che un solo server alla volta
scriva su ogni SocketChannel, in modo da garantire che due comunicazioni non si
inframezzino, rendendole entrambe illeggibili.

## Protocolli di comunicazione
### Messagi TCP
Il client invia messaggi al server tramite TCP. I messaggi iniziano con 1 byte,
che specifica il tipo di richiesta, seguito dai parametri della richiesta. Il
server risponde a tutte le richieste con un ack, seguito da eventuali altri
dati della risposta. In caso di errore l'ack è un codice di errore. I dati
primitivi sono inviati as-is, le stringhe iniziano con un `int` (4 byte) che ne
specifica la lunghezza (in byte), seguito dai byte della stringa.

Segue un elenco delle possibili richieste, con i rispettivi parametri e
possibili risposte di errore. Tutte le richieste ricevono come risposta
`RESP_OK` in caso di successo e possono ricevere `ERR_RETRY` come codice
d'errore.
- `OP_LOGIN`(string, string) i parametri sono (nell'ordine) username e password.
    Un login viene associato ad un SocketChannel; tutte le richieste su quel
    socket vengono eseguite con il login. Un'operazione di login su un socket
    già connesso fallisce. Può rispondere `ERR_UNLOGGED`, `ERR_INVALID_LOGIN`,
    `ERR_USERNAME_BUSY`, `ERR_ALREADY_LOGGED`
- `OP_CREATE`(string, int) i parametri sono il nome del documento e il numero di
    sezioni. Può rispondere `ERR_DOCUMENT_EXISTS`.
- `OP_EDIT`(string, int) i parametri sono il nome completo del documento
    (ie: owner/name) e il numero della sessione. Può rispondere
    `ERR_WRONG_DOCNAME`, `ERR_NO_DOCUMENT`, `ERR_PERMISSION`, `ERR_NO_SECTION`,
    `ERR_SECTION_BUSY`, `ERR_USER_BUSY`. In caso di successo, dopo `RESP_OK`
    invia il file della sezione, seguito dall'ultimo byte dell'indirizzo IP.
- `OP_ENDEDIT` non deve comunicare niente perché l'utente e l'edit corrente
    sono già noti al server. Può rispondere `ERR_USER_FREE`. In caso di
    successo, dopo `RESP_OK` il server si aspetta di ricevere il file
    modificato dal client.
- `OP_SHOWSEC`(string, int) i parametri sono il nome completo del documento e
    il numero della sessione. Può rispondere `ERR_WRONG_DOCNAME`,
    `ERR_NO_DOCUMENT`, `ERR_NO_SECTION`. In caso di successo, dopo `RESP_OK`
    invia un booleano che indica se la sezione sta venendo modificata, poi il
    file della sezione.
- `OP_SHOWDOC`(string) il parametro è il nome completo del documento. Può
    rispondere `ERR_NO_DOCUMENT`. In caso di successo, dopo `RESP_OK` invia il
    numero di sezioni (un intero) e poi per ogni sezione invia in ordine un
    booleano (se sta venendo editata) e il file.
- `OP_INVITE`(string, string) i parametri sono il nome dell'utente da invitare
    e il nome del (proprio) documento a cui invitarlo. Può rispondere
    `ERR_NO_DOCUMENT`. Se l'utente invitato non esiste o ha già il permesso di
    modificare il documento l'operazione ha comunque successo anche se non fa
    nulla.
- `OP_LISTDOCS`() senza parametri. In caso di successo, dopo `RESP_OK` invia il
    numero di documenti (un intero) seguito dai nomi dei singoli documenti (una
    stringa ognuno).

Le possibili risposte sono:
- `RESP_OK` operazione eseguita con successo
- `ERR_RETRY` operazione fallita, riprovare tra poco
- `ERR_UNKNOWN_OP` richiesta un'operazione sconosciuta
- `ERR_UNLOGGED` richiesta un'operazione diversa da login su un socket non
    loggato
- `ERR_INVALID_LOGIN` credenziali errate
- `ERR_USERNAME_BUSY` username già connesso su un altro socket
- `ERR_ALREADY_LOGGED` operazione di login in un socket già connesso
- `ERR_DOCUMENT_EXISTS` documento già esistente, non si può ricreare
- `ERR_WRONG_DOCNAME` nome del documento non valido
- `ERR_NO_DOCUMENT` documento inesistente
- `ERR_PERMISSION` permessi insufficienti
- `ERR_NO_SECTION` sezione inesistente
- `ERR_SECTION_BUSY` sezione editata da qualcun altro
- `ERR_USER_BUSY` utente modificante un'altra sezione
- `ERR_USER_FREE` utente senza modifiche in corso


Il primo messaggio su un socket deve essere di `LOGIN`, altrimenti il server
chiude subito la connessione rispondendo con un `ERR_UNLOGGED`.

Il server può inviare un solo messaggio al client, `OP_INVITE`(string) per
notificargli un invito appena ricevuto. Il parametro è il nome del documento a
cui è stato invitato.

### Trasferimento sezioni (file)
Per trasferire una sezione (ie: un file) si invia un `long` (8 byte) con la
dimensione del file in byte, seguito dai byte del file

## Chat
La chat viene implementata tramite multicast UDP (come richiesto nelle
specifiche). Il multicast avviene direttamente tra i client e non interessa il
server. Il multicast avviene su un indirizzo IP assegnato dal server, tutti
uguali per i primi tre byte, e sempre sulla stessa porta. Il server comunica
solo l'ultimo byte dell'indirizzo al client quando inizia una modifica.

Il server assegna gli IP incrementalmente, tornando a 0 quando arriva a 255.
Si suppone che nel tempo in cui iniziano le modifiche di 255 documenti tutti
i client abbiano terminato la loro modifica, quindi non c'è controllo che un
indirizzo sia già in uso. Gli indirizzi vengono rilasciati quando l'ultimo
client termina l'edit di una sezione.

Il contatore dell'ultimo indirizzo usato e la mappa documento -> chatInfo
viene mantenuta da DBInterface in memoria (tutte le chat sono perse se il
server crasha).

Nei client è presente un thread che ascolta sul socket UDP della chat,
notificando alla GUI l'arrivo di nuovi messaggi. Data la natura di UDP, i
messaggi inviati sulla chat hanno una lunghezza massima (specificata tra le
costanti), un messaggio che superi quella lunghezza viene troncato.

# TODO
Test like no tomorrow
Write the report
Check that at registration a user doesn't contains a / in its name.
Notify pending invitations at login.

Known bugs:
NONE :)
