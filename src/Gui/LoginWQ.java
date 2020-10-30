package Gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import Client.ClientManager;
import Shared.Message;

/**
 * Classe LoginWQ
 * Presenta l'interfaccia per effettuare il login o la registrazione.
 * L'interfaccia presenta i campi username e password da inserire
 * e due bottoni che performano rispettivamente il login e la registrazione.
 * La classe utilizza le funzioni messe a disposizione dal Client Manager per 
 * mandare le richieste al Server.
 * A registrazione o login avvenuti con successo si passa alla home
 * implementata nella classe HomeWQ.
 */
public class LoginWQ {
  private JFrame start;
  private JTextField username;
  private JPasswordField psw;
  private JButton btnLog;
  private JButton btnReg;
  private ClientManager CM;

  public LoginWQ(ClientManager CM) {
    this.CM = CM;
    start = new JFrame("Word Quizzle");
    initialize();
    start.setVisible(true);
  }

  public void initialize() {
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    start.setSize((int) dimension.getWidth() / 4, (int) (dimension.getHeight() * (0.6)));
    int x = (int) ((dimension.getWidth() - start.getWidth()) / 2);
    int y = (int) ((dimension.getHeight() - start.getHeight()) / 2);
    start.setLocation(x, y);
    start.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    start.getContentPane().setLayout(null);
    start.getContentPane().setBackground(Color.blue);

    JLabel title = new JLabel("Word Quizzle", JLabel.CENTER);
    title.setBounds(20, 10, 300, 40);
    title.setFont(new Font("Tahoma", Font.PLAIN, 30));
    title.setForeground(Color.white);
    start.getContentPane().add(title);

    JLabel lbUsername = new JLabel("Username");
    lbUsername.setBounds(60, 100, 105, 21);
    lbUsername.setForeground(Color.white);
    start.getContentPane().add(lbUsername);

    username = new JTextField(20);
    username.setBounds(180, 100, 105, 21);
    username.setForeground(Color.white);
    username.setBackground(Color.blue);
    start.getContentPane().add(username);

    JLabel lbPssw = new JLabel("Password");
    lbPssw.setBounds(60, 130, 105, 21);
    lbPssw.setForeground(Color.white);
    start.getContentPane().add(lbPssw);

    psw = new JPasswordField(20);
    psw.setBounds(180, 130, 105, 21);
    psw.setForeground(Color.white);
    psw.setBackground(Color.blue);
    start.getContentPane().add(psw);

    btnLog = new JButton("Login");
    btnLog.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        // Controllo username/password immessi dall'utente
        if (username.getText().trim().isEmpty()) {
          JOptionPane.showMessageDialog(start, "Utente non valido! Riprova", "Error Message",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        if (psw.getPassword().length == 0) {
          JOptionPane.showMessageDialog(start, "Password non valida! Riprova", "Error Message",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        try {
          Message esito = CM.login(username.getText().trim(), new String(psw.getPassword()));
          switch (esito.getType()){
            case Message.LOGIN_OK : 
              // login eseguito con successo
              //mando la portaUdp al server
              Message portM = CM.sendPortUdp(username.getText());
              switch(portM.getType()){
                case Message.PORT_ERROR :  JOptionPane.showMessageDialog(start, portM.getPayloadString(), "Error Message",
                  JOptionPane.ERROR_MESSAGE);
                  break;
                case Message.PORT_OK :
                  @SuppressWarnings("all")
                  HomeWQ homeForm = new HomeWQ(CM, start);
                  start.setVisible(false);
                  break;
              }
              break;
            case Message.LOGIN_ERROR_USERNAME:
              JOptionPane.showMessageDialog(start, esito.getPayloadString(), "Error Message",
                JOptionPane.ERROR_MESSAGE);
                break;
            case Message.LOGIN_ERROR_PASSWORD:
              JOptionPane.showMessageDialog(start, esito.getPayloadString(), "Error Message",
                JOptionPane.ERROR_MESSAGE);
                break;
            case Message.LOGIN_ERROR_ALREADY_ONLINE:
              JOptionPane.showMessageDialog(start, esito.getPayloadString(), "Error Message",
                JOptionPane.ERROR_MESSAGE);
                break;
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(start, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    });
    btnLog.setBounds(60, 200, 105, 30);
    start.getContentPane().add(btnLog);

    btnReg = new JButton("Register");
    btnReg.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // Controllo username/password immessi dall'utente
        if (username.getText().isEmpty()) {
          JOptionPane.showMessageDialog(start, "Utente non valido! Riprova", "Error Message",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (psw.getPassword().length == 0) {
          JOptionPane.showMessageDialog(start, "Password non valida! Riprova", "Error Message",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        try {
          int esito = CM.register(username.getText(), new String(psw.getPassword()));
          if(esito == 0){
            JOptionPane.showMessageDialog(start, "Username già in uso", "Error Message", JOptionPane.ERROR_MESSAGE);
          }else{
            btnLog.doClick();
          }
        } catch (RemoteException e1) {
          JOptionPane.showMessageDialog(start, "Server disconnesso!", "Error Message", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
      }
    });
    btnReg.setBounds(180, 200, 105, 30);
    start.getContentPane().add(btnReg);
  }
}
