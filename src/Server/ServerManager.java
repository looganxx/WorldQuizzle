package Server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import Shared.Message;
import Shared.User;

/**
 * Classe ServerManager
 * Viene attivato ogni qual volta vi è una richiesta da parte del client.
 * Nel metodo costruttore prende l'attachment della key (UserAttach), la key relativa 
 * e il selector.
 * Nel metodo run viene fatto il controllo su quale richiesta viene fatta dal client
 * e ad ogni richiesta corrisponde la relativa funzione che la implementa.
 */

public class ServerManager implements Runnable {
  private UserAttach state;
  private SelectionKey key;
  private Selector selector;
  // Datagram socket per l'invio della sfida al client
  private DatagramSocket socketServerUdp;
  private static int N = Server.dic.size(); //grandezza dell'array di parole
  private static int K = 8; //numero di parole per la sfida
  // tempo di attesa massimo di risposta del pacchetto UDP inviato dallo sfidato
  private static int T1 = 6000;

  public ServerManager(UserAttach state, SelectionKey key, Selector selector) {
    this.state = state;
    this.key = key;
    this.selector = selector;
  }

  @Override
  public void run() {
    switch (state.request.getType()) {
    case Message.LOGIN:
      login();
      break;
    case Message.PORT:
      takePort();
      break;
    case Message.LOGOUT:
      logout();
      break;
    case Message.FRIEND_REQUEST:
      friendsRequest();
      break;
    case Message.FRIENDS_LIST:
      friendsList();
      break;
    case Message.PUNTEGGIO:
      mostra_punteggio();
      break;
    case Message.CLASSIFICA:
      mostra_classifica();
      break;
    case Message.SFIDA:
      sfida();
      break;
    case Message.SFIDA_ACCEPTED:
      //per lo sfidato
      sfida_accepted();
      break;
    }

  }

  /**
   * Nel caso di login il payload del messaggio conterrà username e password
   * dell'utente. Dopo averli ricavati tramite la funzione checkDB del Server
   * viene fatto un controllo sull'esito: 1 login ok, 3 username error, 4 password
   * error. In base all'esito viene mandato il messaggio opportuno.
   * In caso di avvenuto login viene salvato nell'attachment l'username dell'utente
   * e viene messo nella lista degli utenti online.
   * 
   */
  private void login() {
    String[] split = state.request.getPayloadString().split(" ");
    String username = split[0];
    String psw = split[1];
    int esito = Server.checkDB(username, psw);
    if (esito != 1) {
      if (esito == 3) {
        state.response = new Message(Message.LOGIN_ERROR_USERNAME, "Username inesistente! Riprova");
      } else {
        state.response = new Message(Message.LOGIN_ERROR_PASSWORD, "Password errata! Riprova");
      }
    } else {
      if (Server.checkOnline(username)) {
        state.response = new Message(Message.LOGIN_ERROR_ALREADY_ONLINE, "Utente già online!");
      } else {
        Server.goOnline(username);
        state.username = username;
        state.response = new Message(Message.LOGIN_OK, "Accesso consentito");
      }
    }
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di ricezione della porta UDP viene salvato in una
   * ConcurrentHashMap<String, Integer> la coppia <username, porta>.
   * In base all'esito viene mandata la relativa risposta.
   */
  public void takePort() {
    try {
      Server.ports.put(state.username, state.request.getPayloadInt());
    } catch (NumberFormatException e) {
      e.printStackTrace();
      state.response = new Message(Message.PORT_ERROR, "error port");
      key.interestOps(SelectionKey.OP_WRITE);
      selector.wakeup();
      return;
    }
    state.response = new Message(Message.PORT_OK, "port ok");
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di logout il payload avrà il nome dell'utente che esegue il logout.
   * L'utente viene rimosso sia dalla lista degli utenti online che dall'hash map
   * contenente le porte UDP.
   * In base all'esito del logout viene mandato il relativo messaggio.
   */
  private void logout() {
    try {
      String user = state.request.getPayloadString();
      Server.goOffline(user);
      Server.ports.remove(user);
      state.response = new Message(Message.LOGOUT_OK, "");
    } catch (Exception e) {
      state.response = new Message(Message.LOGOUT_ERROR, "Errore logout");
    }
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di una rihiesta di amicizia il payload conterrà il
   * nickUtente e nickAmico con il quale si vuole creare l'amicizia
   * Viene controllato se l'utente "nickAmico" esiste nel database 
   * e se non fa già parte della lista degli amici.
   * Se le condizioni precedenti sono verificate allora viene creato il
   * legame di amicizia tra i due utenti e aggiornato il database.
   * In base all'esito della richiesta viene inviato il relativo messaggio 
   * con l'opportuno payload.
   */
  private void friendsRequest() {
    String[] split = state.request.getPayloadString().split(" ");
    String nickUtente = split[0];
    String nickAmico = split[1];
    if (nickUtente == null || nickAmico == null)
      throw new NullPointerException();

    if (Server.checkUser(nickAmico)) {
      if (Server.usersDB.get(nickUtente).getFriends().contains(nickAmico)) {
        state.response = new Message(Message.FRIEND_ALREADY_FRIENDS, nickAmico + " è già tuo amico!");
      } else {
        Server.usersDB.get(nickUtente).getFriends().add(nickAmico);
        Server.usersDB.get(nickAmico).getFriends().add(nickUtente);
        try {
          Server.updateFile();
        } catch (IOException e) {
          e.printStackTrace();
        }
        state.response = new Message(Message.FRIEND_REQUEST_OK, "Amico aggiunto!");
      }
    } else {
      state.response = new Message(Message.FRIEND_REQUEST_ERROR, "User non trovato!");
    }
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di richiesta della lista degli amici il payload conterrà
   * il nome dell'utente.
   * La lista degli amici viene trasformata in json e poi mandata come stringa.
   * In caso di errore viene mandato il relativo messaggio.
   */
  private void friendsList() {
    String user = state.request.getPayloadString();
    ArrayList<String> friends = Server.usersDB.get(user).getFriends();
    if(Server.checkUser(user)){
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      @SuppressWarnings("all")
      Type list = new TypeToken<ArrayList<String>>() {
      }.getType();
      String s = gson.toJson(friends);
      state.response = new Message(Message.FRIENDS_LIST_OK, s);
    }else{
      state.response = new Message(Message.FRIENDS_LIST_ERROR, "Utente non trovato!");
    }
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di richiesta del punteggio il payload conterrà l'username 
   * dell'utente.
   * Viene ricavato il punteggio e mandato in un messaggio come intero.
   * In caso di errore viene restituito l'opportuno messaggio.
   */
  private void mostra_punteggio() {
    String user = state.request.getPayloadString();
    if(Server.checkUser(user)){
      int p = Server.usersDB.get(user).getPunteggio();
      state.response = new Message(Message.PUNTEGGIO_OK, p);
    }else{
      state.response = new Message(Message.PUNTEGGIO_ERROR, "Utente non trovato!");
    }
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di richiesta del punteggio il payload conterrà l'username
   * dell'utente.
   * Si ricava la lista degli amici con il relativo punteggio ottenuto,
   * viene trasformato in json e successivamente inviato come stringa.
   * In caso di errore viene restituito l'opportuno messaggio.
   */
  private void mostra_classifica() {
    String user = state.request.getPayloadString();
    if(Server.checkUser(user)){
      ArrayList<User> friendsScore = new ArrayList<User>();
      ArrayList<String> classifica = Server.usersDB.get(user).getFriends();
      for (String s : classifica) {
        friendsScore.add(Server.usersDB.get(s));
      }
      Collections.sort(friendsScore);
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      @SuppressWarnings("all")
      Type classList = new TypeToken<ArrayList<User>>() {
      }.getType();
      String s = gson.toJson(friendsScore);
      state.response = new Message(Message.CLASSIFICA_OK, s);
    }else{
      state.response = new Message(Message.CLASSIFICA_ERROR, "Utente non trovato!");
    }
    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }

  /**
   * Nel caso di sfida il payload conterrà gli username dello sfidante 
   * e dello sfidato.
   * Dato la richiesta di traduzione di parole da effettuare viene fatto
   * prima un test di rete.
   * In seguito viene testato che nickAmico esista nel database, altrimenti
   * viene mandato l'opportuno messaggio d'errore.
   * Successivamente si controlla se nickAmico appartiene alla lista degli amici 
   * di nickUtente, altrimenti viene mandato l'opportuno messaggio d'errore.
   * In seguito viene testato se nickAmico è online, altrimenti viene mandato
   * l'opportuno messaggio d'errore.
   * A questo punto viene creato il datagram socket con il relativo timer e 
   * mandata in UDP la richiesta di sfida contenente come payload nickUtente.
   * Se non viene ricevuta risposta la richiesta di sfida viene rifiutata.
   * Oppure se viene ricevuta una risposta viene controllato il tipo di messaggio ricevuto
   * ovvero se l'utente è occupato in un'altra sfida, se è stata rifiutata
   * oppure se è stata accettata. Nel caso che la sfida sia stata accettata
   * vengono scelte le parole per la sfida e vengono tradotte con una richiesta
   * HTTP al sito, in seguito viene creato un oggetto di tipo "Challenge"
   * che conterrà tutte le informazioni della sfida tra cui gli username degli sfidanti
   * e la lista di parole.
   * Infine viene creata una nuova connessione TCP e un nuovo thread che gestirà la sfida 
   * per lo sfidante che comunicherà sul socket appena creato.
   * Come messaggio di risposta verrà mandato in payload la porta appena creata per la 
   * connesione TCP.
   */
  private void sfida() {
    String[] split = state.request.getPayloadString().split(" ", 2);
    String nickUtente = split[0];
    String nickAmico = split[1];
    try {
      URL url = new URL("http://www.google.com");
      URLConnection connection = url.openConnection();
      connection.connect();
    } catch (Exception e) {
      state.response = new Message(Message.NO_CONNECTION, "Connessione internet assente");
      key.interestOps(SelectionKey.OP_WRITE);
      selector.wakeup();
      return;
    }

    if (nickUtente == null || nickAmico == null)
      throw new NullPointerException();

    if (!Server.checkUser(nickAmico)) {
      state.response = new Message(Message.SFIDA_NOT_EXIST, "L'utente non esiste");
      key.interestOps(SelectionKey.OP_WRITE);
      selector.wakeup();
      return;
    }

    if (Server.usersDB.get(nickUtente).getFriends().contains(nickAmico) == false) {
      state.response = new Message(Message.SFIDA_NOT_FRIEND, "Non sei suo amico");
      key.interestOps(SelectionKey.OP_WRITE);
      selector.wakeup();
      return;
    }

    if (!Server.checkOnline(nickAmico)) {
      state.response = new Message(Message.SFIDA_NOT_ONLINE, "Amico offline");
      key.interestOps(SelectionKey.OP_WRITE);
      selector.wakeup();
      return;
    }

    try {
      // creazione DatagramSocket
      try {
        socketServerUdp = new DatagramSocket();
        socketServerUdp.setSoTimeout(T1);
      } catch (SocketException e1) {
        e1.printStackTrace();
        return;
      }

      Message msg = new Message(Message.SFIDA, nickUtente);
      // conversione byte array
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(bos);) {
        out.writeObject(msg);
        out.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      DatagramPacket packet = new DatagramPacket(bos.toByteArray(), bos.size(), InetAddress.getByName("localhost"), Server.ports.get(nickAmico));
      socketServerUdp.send(packet);
      bos.close();

      byte[] b = new byte[500];
      DatagramPacket res = new DatagramPacket(b, b.length);
      socketServerUdp.receive(res);
      ByteArrayInputStream bis = new ByteArrayInputStream(b);

      try (ObjectInputStream in = new ObjectInputStream(bis);) {
        Message rcv = (Message) in.readObject();
        in.close();
        switch(rcv.getType()){
          case Message.SFIDA_BUSY:
          state.response = new Message(Message.SFIDA_BUSY, rcv.getPayloadString());
          key.interestOps(SelectionKey.OP_WRITE);
          selector.wakeup();
          break;
          case Message.SFIDA_REJECTED:
          state.response = new Message(Message.SFIDA_REJECTED, rcv.getPayloadString());
          key.interestOps(SelectionKey.OP_WRITE);
          selector.wakeup();
          break;
          case Message.SFIDA_ACCEPTED:
          ArrayList<String> wordlList = new ArrayList<String>(K);
          ArrayList<String> wordlListTranslated = new ArrayList<String>(K);
          translate(wordlList, wordlListTranslated);
          System.out.print(wordlList);
          System.out.print(wordlListTranslated);
          Server.sfideLock.lock(); 
          Challenge c = new Challenge(nickUtente, nickAmico);
          c.setWL(wordlList, wordlListTranslated);
          Server.sfide.add(c); 
          Server.sfideLock.unlock();
          ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();          
          serverSocketChannel.socket().bind(new InetSocketAddress(0));
          state.response = new Message(Message.SFIDA_OK, serverSocketChannel.socket().getLocalPort());
          key.interestOps(SelectionKey.OP_WRITE);
          selector.wakeup();
          TSfida sfidanteT = new TSfida(serverSocketChannel, c, key, selector );
          Thread t = new Thread(sfidanteT, state.username);
          t.start();
          break;
        }
      } catch (IOException ex) {
        ex.printStackTrace();
        return;
      } catch (ClassNotFoundException ex) {
        ex.printStackTrace();
        return;
      }
    } catch (SocketTimeoutException e1) {
      // se scade il timeout la richiesta viene rifiutata
      state.response = new Message(Message.SFIDA_REJECTED, "Non accettato in tempo");
      key.interestOps(SelectionKey.OP_WRITE);
      selector.wakeup();
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }

  /**
   * Funzione di traduzione delle parole scelte, con la richiesta in HTTP al sito.
   * @param wordlList = arraylist contenente le parole da tradurre
   * @param wordlListTranslated = arraylist che conterrà le parole tradotte
   */
  private void translate(ArrayList<String> wordlList, ArrayList<String> wordlListTranslated) {
    Random r = new Random();
    for (int i = 0; i < K; i++) {
      wordlList.add(Server.dic.get(r.nextInt(N)));
    }
    for (int j = 0; j < K; j++) {
      URL url;
      try {
        url = new URL("https://api.mymemory.translated.net/get?q=" + wordlList.get(j) + "&langpair=it|en");
        URLConnection con = url.openConnection();
        BufferedReader bin = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = bin.readLine()) != null) {
          content.append(inputLine);
        }
        bin.close();
        String resp = content.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<JModel>() {
        }.getType();
        JModel translated = gson.fromJson(resp, type);
        wordlListTranslated.add(translated.getInfo().getTranslatedText().toLowerCase());
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
  }

  /**
   * Nel caso in cui uno sfidato accetta la sfida ricevuta il payload conterrà
   * il nome dello sfidante.
   * Viene controllato se nella lista di oggetti Challenge esiste un oggetto
   * che possiede il nome dello sfidante e dello sfidato.
   * Se non viene trovata alcuna corrispondenza viene mandato un messaggio di 
   * annullamento sfida, altrimenti viene creato un socket TCP e un thread per
   * la gestione della sfida che utilizzerà il socket appena creato per 
   * comunicare con il client.
   * Nella risposta al client viene mandato nel payload la porta per la connessione TCP.
   */
  private void sfida_accepted(){
    String sfidante = state.request.getPayloadString();
    Challenge appo = new Challenge(sfidante, state.username);
    Challenge c = null;
    ServerSocketChannel serverSocketChannel = null;
    int i = 0;
    Server.sfideLock.lock();
    while (c == null && i < Server.sfide.size()){
      if(appo.equals(Server.sfide.get(i))) c = Server.sfide.get(i);
      i++;
    }
    Server.sfideLock.unlock();
    if(c == null){
      state.response = new Message(Message.SFIDA_REJECTED, "Sfida Annullata");
    }else{
      try {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(0));
      } catch (IOException e) {
        e.printStackTrace();
      }
      state.response = new Message(Message.SFIDA_OK, serverSocketChannel.socket().getLocalPort());
      TSfida sfidatoT = new TSfida(serverSocketChannel, c, key, selector);
      Thread t = new Thread(sfidatoT, state.username);
      t.start();
    }

    key.interestOps(SelectionKey.OP_WRITE);
    selector.wakeup();
  }
}