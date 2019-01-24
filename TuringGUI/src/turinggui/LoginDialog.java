/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinggui;

import java.awt.event.WindowEvent;
import server.lib.*;
import java.io.IOException;
import java.nio.channels.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import javax.swing.JFrame;

/**
 *
 * @author flavio
 */
public class LoginDialog extends javax.swing.JDialog {
    private RegistrationInterface registrationServer;
    private SocketChannel chnl;
    private String result;

    /**
     * Creates new form LoginDialog
     */
    public LoginDialog() {
        super();
        initComponents();
    }
    
    /**
     * Creates new form LoginDialog for the specified SocketChannel
     * @param owner the Frame from which the dialog is displayed
     * @param chnl_set the channel on which try the login. The channel should
     *                 already be connected
     */
    public LoginDialog(JFrame owner, SocketChannel chnl_set) {
        super(owner, true);
        initComponents();
        chnl = chnl_set;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        registerButton = new javax.swing.JButton();
        usernameTextfield = new javax.swing.JTextField();
        pwdTextfield = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        loginButton = new javax.swing.JButton();
        logLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Login");

        registerButton.setText("Register");
        registerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                registerButtonActionPerformed(evt);
            }
        });

        usernameTextfield.setToolTipText("Username");
        usernameTextfield.setMinimumSize(new java.awt.Dimension(4, 24));
        usernameTextfield.setPreferredSize(new java.awt.Dimension(100, 24));

        pwdTextfield.setToolTipText("Password");
        pwdTextfield.setMinimumSize(new java.awt.Dimension(4, 24));
        pwdTextfield.setPreferredSize(new java.awt.Dimension(100, 24));

        jLabel1.setText("Username:");

        jLabel2.setText("Password:");

        loginButton.setText("Login");
        loginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pwdTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(usernameTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(loginButton)
                        .addGap(37, 37, 37)
                        .addComponent(registerButton))
                    .addComponent(logLabel))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(usernameTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pwdTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(logLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(registerButton)
                    .addComponent(loginButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void registerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_registerButtonActionPerformed
        try {
            if (registrationServer == null) {
                Registry r = LocateRegistry.getRegistry(12345);
                registrationServer = (RegistrationInterface)r.lookup("TURING-REGISTRATION");
            }
            String usr = usernameTextfield.getText();
            registrationServer.register(usr, pwdTextfield.getText());
            UserLog("Sucessfully registered username \"" + usr + "\"");
        }
        catch (NotBoundException e) {
            UserLog("Wrong identifier for remote server: " + e.getMessage());
        }
        catch (RemoteException e) {
            UserLog("Remote error during registration: " + e.getMessage());
        }
        catch (UsernameAlreadyInUseException | InternalServerException e) {
            UserLog(e.getMessage());
        }
    }//GEN-LAST:event_registerButtonActionPerformed

    private void loginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginButtonActionPerformed
        try {
            String usr = usernameTextfield.getText();
            IOUtils.writeOpKind(OpKind.OP_LOGIN, chnl);
            IOUtils.writeString(usr, chnl);
            IOUtils.writeString(pwdTextfield.getText(), chnl);
            OpKind response = IOUtils.readOpKind(chnl);
            switch (response) {
                case RESP_OK:
                UserLog("Sucessfully logged in with username \"" + usr + "\"");
                result = usr;
                this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                break;
                case ERR_RETRY:
                UserLog("Server error: try again");
                break;
                case ERR_INVALID_LOGIN:
                UserLog("Wrong credentials");
                break;
                case ERR_USERNAME_BUSY:
                UserLog("Username already connected");
                break;
            }
        }
        catch (IOException e) {
            UserLog("Error connecting to the server: try again");
        } catch (ChannelClosedException ex) {
            UserLog("Connection to the server lost: restart the application");
        }
    }//GEN-LAST:event_loginButtonActionPerformed

    public String showDialog() {
        this.setVisible(true);
        return result;
    }
    
    public void UserLog(String s) {
        logLabel.setText(s);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LoginDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LoginDialog().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel logLabel;
    private javax.swing.JButton loginButton;
    private javax.swing.JTextField pwdTextfield;
    private javax.swing.JButton registerButton;
    private javax.swing.JTextField usernameTextfield;
    // End of variables declaration//GEN-END:variables
}