package Gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import Client.ClientManager;
import Shared.Message;
import Shared.MessageManager;

/**
 * Classe TSfidaClient
 * Il thread viene attivato nel momento in cui la sfida ha inizio.
 * Nella pagina viene mostrato il numero di parole inviate fino a quel momento.
 * Vi è un text field che permette l'inserimento della risposta.
 * Tramite i bottoni "Submit" e "Skip" è possibile inviare la risposta oppure
 * saltare la parola proposta che verrà segnata come non data.
 * Il thread riceve ad ogni iterazione un messaggio dal server tra i possibili:
 * - nuova parola da tradurre
 * - timer scaduto
 * - attesa dell'esito della partita se l'avversario non ha finito
 * - esito della partita e di conseguenza la fine della sfida e ritorno alla home.
 */

public class TSfidaClient implements Runnable {
  private SocketChannel socket;
  private ClientManager CM;
  private int port;
  private JFrame frame;
  private JFrame challenge;
  private JLabel presentation;
  private JLabel time;
  private JTextField tfWord;
  private JButton submit;
  private JButton skip;
  private static int K = 8;
  int i = 1;

  public TSfidaClient(ClientManager CM, JFrame frame, int port) {
    socket = CM.getSocketC();
    challenge = new JFrame("Word Quizzle");
    this.CM = CM;
    this.frame = frame;
    this.port = port;
  }

  @Override
  public void run() {
    CM.busy.set(true);
    try {
      socket = SocketChannel.open(new InetSocketAddress("localhost", port));
    } catch (IOException e2) {
      JOptionPane.showMessageDialog(frame, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    challenge.setSize((int) dimension.getWidth() / 4, (int) (dimension.getHeight() * (0.6)));
    challenge.setLocation(frame.getX(), frame.getY());
    challenge.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    challenge.getContentPane().setLayout(null);
    challenge.getContentPane().setBackground(Color.blue);
    frame.setVisible(false);

    JLabel title = new JLabel("Word Quizzle", JLabel.CENTER);
    title.setBounds(20, 10, 300, 40);
    title.setFont(new Font("Tahoma", Font.PLAIN, 30));
    title.setForeground(Color.white);
    challenge.getContentPane().add(title);
    
    Message word = null;
    try {
      word = MessageManager.readMsg(socket);
    } catch (IOException e1) {
      JOptionPane.showMessageDialog(challenge, "Errore lettura parole", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    time = new JLabel("60 secondi a disposizione", JLabel.CENTER);
    time.setBounds(70, 60, 200, 30);
    time.setFont(new Font("Tahoma", Font.PLAIN, 12));
    time.setForeground(Color.white);
    challenge.getContentPane().add(time);

    presentation = new JLabel("Challenge " + i + "/" + K + ":" + word.getPayloadString(), JLabel.CENTER);
    presentation.setBounds(70, 90, 200, 30);
    presentation.setFont(new Font("Tahoma", Font.PLAIN, 12));
    presentation.setForeground(Color.white);
    challenge.getContentPane().add(presentation);

    tfWord = new JTextField(20);
    tfWord.setBounds(100, 130, 150, 21);
    tfWord.setForeground(Color.white);
    tfWord.setBackground(Color.blue);
    tfWord.setFont(new Font("Tahoma", Font.PLAIN, 12));
    challenge.getContentPane().add(tfWord);

    submit = new JButton("Submit");
    submit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Message reply = new Message(Message.SFIDA_WORD_REPLY, tfWord.getText());
        sendreciveResponse(reply);
      }
    });
    submit.setBounds(60, 220, 105, 30);
    challenge.getContentPane().add(submit);

    skip = new JButton("Skip");
    skip.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String s = new String("");
        Message reply = new Message(Message.SFIDA_WORD_REPLY, s);
        sendreciveResponse(reply);
      }
    });
    skip.setBounds(180, 220, 105, 30);
    challenge.getContentPane().add(skip);
    
    challenge.setVisible(true);  
  }

  /**
   * Viene gestita l'invio della parola, sia in caso di "Submit" che di "Skip"
   * e di conseguenza le possibili risposte del Server.
   * Vengono modificati anche il campo "presentation" che rappresenta la parola
   * sottoposta dal server, e resettato il campo "tfWord".
   * @param reply = risposta alla parola inviata
   */
  private void sendreciveResponse(Message reply){
    try {
      MessageManager.sendMsg(socket, reply);
      Message word = MessageManager.readMsg(socket);
      i++; //serve solo per la scrittura di presentation
      switch (word.getType()) {
      case Message.SFIDA_WORD:
        presentation.setText("Challenge " + i + "/" + K + ":" + word.getPayloadString());
        tfWord.setText("");
        break;
      case Message.SFIDA_WAIT:
        JOptionPane.showMessageDialog(challenge, word.getPayloadString(), "Attendi", JOptionPane.DEFAULT_OPTION);
        Message result = MessageManager.readMsg(socket);
        JOptionPane.showMessageDialog(challenge, result.getPayloadString(), "Risultato Partita",
            JOptionPane.INFORMATION_MESSAGE);
        CM.busy.set(false);
        socket.close();
        frame.setVisible(true);
        challenge.dispose();
        break;
      case Message.SFIDA_TIMER:
        JOptionPane.showMessageDialog(challenge, word.getPayloadString(), "Timer", JOptionPane.DEFAULT_OPTION);
        Message resultT = MessageManager.readMsg(socket);
        JOptionPane.showMessageDialog(challenge, resultT.getPayloadString(), "Risultato Partita",
            JOptionPane.INFORMATION_MESSAGE);
        CM.busy.set(false);
        socket.close();
        frame.setVisible(true);
        challenge.dispose();
        break;
      case Message.SFIDA_RESULT:
        JOptionPane.showMessageDialog(challenge, word.getPayloadString(), "Risultato Partita",
            JOptionPane.INFORMATION_MESSAGE);
        CM.busy.set(false);
        socket.close();
        frame.setVisible(true);
        challenge.dispose();
        break;
      }
    } catch (IOException e1) {
      JOptionPane.showMessageDialog(challenge, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }
  
}
