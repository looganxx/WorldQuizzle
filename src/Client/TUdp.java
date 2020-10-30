package Client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import Gui.TSfidaClient;
import Shared.Message;

/**
 * Classe TUdp
 * Viene attivato il thread al momento del login si occupa di gestire tutte le richieste
 * di sfida che l'utente riceverà
 */
public class TUdp implements Runnable {
  private DatagramSocket socketClientUdp; // socket sul quale avverrà la connessione
  private ClientManager CM; //CM dell'utente
  private JFrame frame; //frame home

  public TUdp(DatagramSocket socketClientUdp, JFrame frame, ClientManager CM) {
    this.socketClientUdp = socketClientUdp;
    this.frame = frame;
    this.CM = CM;
  }

  @Override
  /**
   * Riceve il pacchetto UDP di richiesta di sfida dal server
   * Se l'utente è occupato in una sfida manda in automatico un messaggio
   * che notifica l'utente occupato.
   * Se l'untente non è occupato allora viene presentato un message box
   * dove l'utente può decidere se accettare o meno la richiesta di sfida.
   * Nel caso in cui la richiesta viene accettata viene fatta una richiesta
   * al server se può cominciare effettivamente la partita oppure se è stata 
   * annullata nel caso in cui la risposta non è arrivata in tempo oppure se 
   * la risposta in UDP non è arrivata.
   */
  public void run() {
    while(true){
      byte[] b = new byte[100];
      DatagramPacket packet = new DatagramPacket(b, b.length);
      try {
        socketClientUdp.receive(packet);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
      }
      Message rcv = null;
      ByteArrayInputStream bis = new ByteArrayInputStream(b);
      try (ObjectInputStream in = new ObjectInputStream(bis);) {
        rcv = (Message) in.readObject();
        in.close();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
      }
      Message resp = null;
      DatagramPacket respPacket;
      Boolean busy;
      int result = JOptionPane.NO_OPTION;
      if (busy = CM.busy.get()) {
        resp = new Message(Message.SFIDA_BUSY, "Utente occupato");
      } else {
        result = JOptionPane.showConfirmDialog(frame, "Sfida ricevuta da " + rcv.getPayloadString(), "Sfida",
            JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.NO_OPTION) {
          resp = new Message(Message.SFIDA_REJECTED, "Richiesta Rifiutata!");
        } else if (result == JOptionPane.YES_OPTION) {
          resp = new Message(Message.SFIDA_ACCEPTED, "Richiesta Accettata!");
          CM.busy.set(true);
        }
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(bos);) {
        out.writeObject(resp);
        out.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      respPacket = new DatagramPacket(bos.toByteArray(), bos.size(), packet.getAddress(), packet.getPort());
      try {
        socketClientUdp.send(respPacket);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
      }
      if(!busy){
        if(result == JOptionPane.YES_OPTION){
          try {
            //rcv ha il nome dello sfidante nel payload
            Message esito = CM.sfidaAccepted(rcv.getPayloadString());
            switch(esito.getType()){
              case Message.SFIDA_REJECTED:
                JOptionPane.showMessageDialog(frame, esito.getPayloadString(), "Error", JOptionPane.ERROR_MESSAGE);
                CM.busy.set(false);
                break;
              case Message.SFIDA_OK:
                JOptionPane.showMessageDialog(frame, "Inizio Sfida!", "Sfida", JOptionPane.INFORMATION_MESSAGE);
                TSfidaClient sClient = new TSfidaClient(CM, frame, esito.getPayloadInt());
                Thread t = new Thread(sClient);
                t.start();
                break;
            }
          } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
          }
        }
      }
    }
  }
  
}