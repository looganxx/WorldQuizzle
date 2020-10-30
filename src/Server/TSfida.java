package Server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import Shared.Message;
import Shared.MessageManager;

/**
 * Classe TSfida
 * Thread che viene attivato ogni qual volta si avvia una sfida, vengono avviati due
 * thread di questa classe: uno per lo sfidante e uno per lo sfidato.
 * Viene controllato se il thread è dello sfidante o dello sfidato.
 * In serguito viene attivato il timer e comincia l'invio delle parole da tradurre al client.
 * Alla fine della sfida se l'avversario non ha finito viene fatta attesa per sapere il punteggio.
 * Una volta che entrambi hanno concluso la sfida viene inviato un messaggio con l'esito
 * della partita e viene rimosso l'oggetto challenge dall'arraylist delle sfide.
 * Viene, in seguito aggiornato il punteggio dell'utente al database e aggiornato il File .json
 */
public class TSfida implements Runnable {
  ServerSocketChannel socketChallenge;
  ArrayList<String> wordlList;
  ArrayList<String> wordlListTranslated;
  SelectionKey key;
  Challenge c;
  Selector selector;
  private static int X = 3;
  private static int Y = -1;
  private static int Z = 3;
  private static int T2 = 60000;
  public static int TIMER_CODE = -2;
  AtomicInteger i = new AtomicInteger(0);

  public TSfida(ServerSocketChannel socketChallenge, Challenge c, SelectionKey key, Selector selector){
    this.socketChallenge = socketChallenge;
    this.key = key;
    this.selector = selector;
    this.c = c;
    this.wordlList = c.getWL();
    this.wordlListTranslated = c.getWLT();
  }

  @Override
  public void run(){
    int correct = 0;
    int wrong = 0;
    boolean challenger;
    SocketChannel socket = null;
    try {
      socket = socketChallenge.accept();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if(Thread.currentThread().getName().equals(c.getSfidante())){
      challenger = true;
    }else{
      challenger = false;
    }
    Timer t = new Timer();
    t.schedule(new MyTimer(i), T2);
    //i sarà sempre 0 all'inizio del ciclo
    int iwhile = 0;
    while((iwhile = i.get()) < wordlList.size() &&  iwhile >= 0) {
      Message word = new Message(Message.SFIDA_WORD, wordlList.get(iwhile));
      try {
        MessageManager.sendMsg(socket, word);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      if(i.get() == TIMER_CODE){
        try {
          @SuppressWarnings("all")
          Message respIgnored = MessageManager.readMsg(socket);
          Message error = new Message(Message.SFIDA_TIMER, "Tempo scaduto!");
          MessageManager.sendMsg(socket, error);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }else{
        Message wordTranslated;
        try {
          wordTranslated = MessageManager.readMsg(socket);
          if(wordTranslated.getType() == Message.SFIDA_WORD_REPLY){
            if (i.get() == TIMER_CODE) {
              Message error = new Message(Message.SFIDA_TIMER, "Tempo scaduto!");
              try {
                MessageManager.sendMsg(socket, error);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }else{
              if (wordTranslated.getPayloadString().trim().toLowerCase().equals(wordlListTranslated.get(iwhile))) {
                if (challenger) {
                  c.setsfidanteP(X);
                } else {
                  c.setsfidatoP(X);
                }
                correct++;
              } else if (!wordTranslated.getPayloadString().trim().equals("")) {
                if (challenger) {
                  c.setsfidanteP(Y);
                } else {
                  c.setsfidatoP(Y);
                }
                wrong++;
              }
            }
          }
        } catch (IOException e) {
          if(challenger){
            c.setTsfidante(true);
            if(c.getTsfidato() == true){
              Server.sfide.remove(c);
            }
          }else{
            c.setTsfidato(true);
            if (c.getTsfidante() == true) {
              Server.sfide.remove(c);
            }
          }
          e.printStackTrace();
        }
      }
      i.incrementAndGet();
    }
    if(i.get() >= 0){
      t.cancel();
      if (challenger){
        c.setTsfidante(true);
        if(!c.getTsfidato()){
        Message waiting = new Message(Message.SFIDA_WAIT, "Aspetta che anche il tuo avversario finisca!");
        try {
          MessageManager.sendMsg(socket, waiting);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      while(!c.getTsfidato()){}
      }else{
        c.setTsfidato(true);
        if(!c.getTsfidante()){
          Message waiting = new Message(Message.SFIDA_WAIT, "Aspetta che anche il tuo avversario finisca!");
          try {
            MessageManager.sendMsg(socket, waiting);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        while(!c.getTsfidante()){}
      }
    }
    c.setTsfidante(true);
    c.setTsfidato(true);
    
    int x = 0;
    int y = 0;
    if(challenger){
      x = c.getsfidanteP();
      y = c.getsfidatoP();
    }else{
      x = c.getsfidatoP();
      y = c.getsfidanteP();
    }
    try {
      //controllo esito partita
      if(x == y){
          MessageManager.sendMsg(socket,
          new Message(Message.SFIDA_RESULT,
          "Pareggio! Hai dato " + correct + " risposte corrette, " + wrong + " risposte sbagliate, "
          + (wordlList.size() - correct - wrong) + " risposte non date. Punti ottenuti: " + x));
      }else{
        if(x < y){
          MessageManager.sendMsg(socket, new Message(Message.SFIDA_RESULT, 
          "Hai Perso! Hai dato " + correct + " risposte corrette, "+ wrong + 
          " risposte sbagliate, " + (wordlList.size()-correct-wrong) + 
          " risposte non date. Punti ottenuti: "+ x));
        }else{
          x = x + Z;
          MessageManager.sendMsg(socket, new Message(Message.SFIDA_RESULT, "Hai Vinto! +" + Z + 
          " punti extra. Totale punti: " + x + " Hai dato " + correct + " risposte corrette, "+ wrong + 
          " risposte sbagliate, " + (wordlList.size()-correct-wrong) + 
          " risposte non date."));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    Server.sfideLock.lock();
    Server.sfide.remove(c);
    Server.sfideLock.unlock();
    if(challenger) Server.updatePoint(x, c.getSfidante());
    else Server.updatePoint(x, c.getSfidato());
    key.interestOps(SelectionKey.OP_READ);
    try {
      Server.updateFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    selector.wakeup();
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}