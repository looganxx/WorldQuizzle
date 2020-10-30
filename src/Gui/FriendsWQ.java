package Gui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import Client.ClientManager;
import Shared.Message;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Classe FriendsWQ
 * Presenta un interfaccia con il campo dell'username amico da inserire
 * con il quale verrà creata l'amicizia.
 * Dopo aver inserito il nome dell'utente basta cliccare sul bottone "Aggiungi"
 * per creare l'amicizia.
 * In fondo al form vi è il bottone "backHome" per tornare al form "home".
 */


public class FriendsWQ {
  private JFrame home;
  private ClientManager CM;
  private JFrame friends;
  private JTextField username;
  private JButton submitRequest;
  private JButton backHome;

  public FriendsWQ(ClientManager CM, JFrame home) {
    this.CM = CM;
    this.home = home;
    friends = new JFrame("Word Quizzle");
    initialize();
    friends.setVisible(true);
  }

  private void initialize() {
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    friends.setSize((int) dimension.getWidth() / 4, (int) (dimension.getHeight() * (0.6)));
    friends.setLocation(home.getX(), home.getY());
    friends.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    friends.getContentPane().setLayout(null);
    friends.getContentPane().setBackground(Color.blue);

    JLabel title = new JLabel("Word Quizzle", JLabel.CENTER);
    title.setBounds(20, 10, 300, 40);
    title.setFont(new Font("Tahoma", Font.PLAIN, 30));
    title.setForeground(Color.white);
    friends.getContentPane().add(title);

    JLabel lbUsername = new JLabel("Username Amico");
    lbUsername.setBounds(100, 60, 150, 100);
    lbUsername.setFont(new Font("Tahoma", Font.PLAIN, 16));
    lbUsername.setForeground(Color.white);
    friends.getContentPane().add(lbUsername);

    username = new JTextField(20);
    username.setBounds(115, 130, 105, 21);
    username.setForeground(Color.white);
    username.setBackground(Color.blue);
    friends.getContentPane().add(username);

    submitRequest = new JButton("Aggiungi");
    submitRequest.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (username.getText().isEmpty() || username.getText().equals(CM.getUser())) {
          JOptionPane.showMessageDialog(friends, "Username non valido! Riprova", "Error Message",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
        try {
          Message esito = CM.aggiungi_amico(CM.getUser(), username.getText());
          switch (esito.getType()) {
          case Message.FRIEND_REQUEST_OK:
            // * amicizia creata
            JOptionPane.showMessageDialog(friends, esito.getPayloadString(), "Success",
                JOptionPane.INFORMATION_MESSAGE);
            break;
          case Message.FRIEND_REQUEST_ERROR:
            JOptionPane.showMessageDialog(friends, esito.getPayloadString(), "Error Message",
                JOptionPane.ERROR_MESSAGE);
            break;
          case Message.FRIEND_ALREADY_FRIENDS:
            JOptionPane.showMessageDialog(friends, esito.getPayloadString(), "Message",
                JOptionPane.INFORMATION_MESSAGE);
            break;
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(friends, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    });
    submitRequest.setBounds(115, 200, 105, 30);
    friends.getContentPane().add(submitRequest);

    backHome = new JButton("Home");
    backHome.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        home.setLocation(friends.getX(), friends.getY());
        home.setVisible(true);
        friends.setVisible(false);
      }
    });
    backHome.setBounds(80, 400, 180, 25);
    friends.getContentPane().add(backHome);
  }

}