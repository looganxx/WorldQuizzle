package Gui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import Client.ClientManager;
import Client.TUdp;
import Shared.Message;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Classe HomeWQ 
 * Presenta un'interfaccia per richiedere tutte le operazioni richieste:
 * aggiugni amico, lista amici, mostra punteggio, mostra classifica, sfida, logout.
 * Premere un bottone tra "aggiungi amico", "lista amici", "mostra classifica" e "sfida"
 * porterà ad un nuovo form per la visualizzazione delle richieste.
 */

public class HomeWQ {
  private JFrame start;
  private ClientManager CM;
  private JFrame home;
  private JButton friendRequest;
  private JButton logout;
  private JButton friendsList;
  private JButton punteggio;
  private JButton classifica;
  private JButton sfida;

  public HomeWQ(ClientManager CM, JFrame start) {
    this.CM = CM;
    this.start = start;
    home = new JFrame("Word Quizzle");
    initialize();
    home.setVisible(true);
  }

  @SuppressWarnings("all")
  private void initialize() {
    TUdp t = new TUdp(CM.getSocket(), home, CM);
    Thread richieste = new Thread(t);
    richieste.start();
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    home.setSize((int) dimension.getWidth() / 4, (int) (dimension.getHeight() * (0.6)));
    home.setLocation(start.getX(), start.getY());
    home.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    home.getContentPane().setLayout(null);
    home.getContentPane().setBackground(Color.blue);

    JLabel title = new JLabel("Word Quizzle", JLabel.CENTER);
    title.setBounds(20, 10, 300, 40);
    title.setFont(new Font("Tahoma", Font.PLAIN, 30));
    title.setForeground(Color.white);
    home.getContentPane().add(title);

    JLabel presentation = new JLabel("Benvenuto " + CM.getUser(), JLabel.CENTER);
    presentation.setBounds(70, 60, 200, 30);
    presentation.setFont(new Font("Tahoma", Font.PLAIN, 20));
    presentation.setForeground(Color.white);
    home.getContentPane().add(presentation);

    friendRequest = new JButton("Aggiungi Amico");
    friendRequest.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        FriendsWQ friendsForm = new FriendsWQ(CM, home);
        home.setVisible(false);
      }
    });
    friendRequest.setBounds(80, 100, 180, 25);
    home.getContentPane().add(friendRequest);

    friendsList = new JButton("Lista Amici");
    friendsList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          Message esito = CM.lista_amici(CM.getUser());
          switch (esito.getType()) {
          case Message.FRIENDS_LIST_ERROR:
            JOptionPane.showMessageDialog(home, esito.getPayloadString(), "Error Message", JOptionPane.ERROR_MESSAGE);
            break;
          case Message.FRIENDS_LIST_OK:
            ListFriendsWQ listFriends = new ListFriendsWQ(CM, home, esito);
            home.setVisible(false);
            break;
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(home, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);        
        }
      }
    });
    friendsList.setBounds(80, 150, 180, 25);
    home.getContentPane().add(friendsList);

    punteggio = new JButton("Mostra Punteggio");
    punteggio.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          Message esito = CM.punteggio(CM.getUser());
          switch(esito.getType()){
            case Message.PUNTEGGIO_ERROR:
              JOptionPane.showMessageDialog(home, esito.getPayloadString(), "Error", JOptionPane.ERROR_MESSAGE);
              break;
            case Message.PUNTEGGIO_OK:
              JOptionPane.showMessageDialog(home, esito.getPayloadInt(), "Punteggio", JOptionPane.INFORMATION_MESSAGE);
              break;
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(home, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    });
    punteggio.setBounds(80, 200, 180, 25);
    home.getContentPane().add(punteggio);

    classifica = new JButton("Mostra Classifica");
    classifica.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          Message esito = CM.classifica(CM.getUser());
          switch (esito.getType()) {
            case Message.CLASSIFICA_OK:
              ClassificaWQ classifica = new ClassificaWQ(CM, home, esito);
              home.setVisible(false);
              break;
            case Message.CLASSIFICA_ERROR:
              JOptionPane.showMessageDialog(home, esito.getPayloadString(), "Error", JOptionPane.ERROR_MESSAGE);
              break;
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(home, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    });
    classifica.setBounds(80, 250, 180, 25);
    home.getContentPane().add(classifica);

    sfida = new JButton("Sfida");
    sfida.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String nickAmico = JOptionPane.showInputDialog(home, "Inserisci Nome Amico", "Sfida", JOptionPane.INFORMATION_MESSAGE);
        if(nickAmico != null){
          nickAmico.trim();
          if(nickAmico.equals(CM.getUser()))
            JOptionPane.showMessageDialog(home, "Non puoi invitare te stesso!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        if(nickAmico != null && !nickAmico.equals(CM.getUser())){
          try {
            CM.busy.set(true);
            Message esito = CM.sfida(CM.getUser(), nickAmico);
            switch (esito.getType()){
              case Message.SFIDA_OK: 
                JOptionPane.showMessageDialog(home, "Inizio Sfida!", "Sfida", JOptionPane.INFORMATION_MESSAGE);
                //in esito c'è la porta per connettersi al socket
                TSfidaClient sClient = new TSfidaClient(CM, home, esito.getPayloadInt());
                Thread t = new Thread(sClient);
                t.start();
                break;
              default:
                CM.busy.set(false);
                JOptionPane.showMessageDialog(home, esito.getPayloadString(), "Error", JOptionPane.ERROR_MESSAGE);
                break;
            }
          } catch (IOException e1) {
            JOptionPane.showMessageDialog(home, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
          }

        }
      }
    });
    sfida.setBounds(80, 300, 180, 25);
    home.getContentPane().add(sfida);

    logout = new JButton("Logout");
    logout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          Message esito = CM.logout(CM.getUser());
          switch (esito.getType()){
            case Message.LOGOUT_ERROR: 
              JOptionPane.showMessageDialog(home, esito.getPayloadString(), "Error Message", JOptionPane.ERROR_MESSAGE);
              break;
            case Message.LOGOUT_OK: 
              JOptionPane.showMessageDialog(home, "Grazie per aver giocato!", "Arrivederci",
                JOptionPane.INFORMATION_MESSAGE);
              start.setLocation(home.getX(), home.getY());
              start.setVisible(true);
              home.setVisible(false);
              break;
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(home, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    });
    logout.setBounds(80, 400, 180, 25);
    home.getContentPane().add(logout);
  }
  
}