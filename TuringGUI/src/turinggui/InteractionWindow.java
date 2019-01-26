/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinggui;

import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.locks.*;
import javax.swing.*;
import server.lib.*;

/**
 *
 * @author flavio
 */
public class InteractionWindow extends javax.swing.JFrame {
    public static final Charset encoding = Constants.encoding;
    
    private final SocketChannel chnl;
    private Lock sock_lock;
    private final String usr;
    private final DatagramSocket chat_sock;
    private ChatListener chat_listener;
    private InetAddress chat_addr;
    private boolean chat_active;
    
    /**
     * Creates new form InteractionWindow
     * @throws java.io.IOException if an I/O exception occurs while connecting 
     *                             to the socket
     */
    public InteractionWindow() throws Exception, IOException {
        SocketAddress addr = new InetSocketAddress("127.0.0.1", 55000);
        chnl = SocketChannel.open();
        chnl.connect(addr);
        usr = new LoginDialog(this, chnl).showDialog();
        if (usr == null) {
            throw new Exception("No username provided!");
        }
        chnl.configureBlocking(false);
        
        initComponents();
        chat_active = false;
        chat_addr = null;
        chat_sock = new DatagramSocket();
        sock_lock = new ReentrantLock();
        showUsrTextbox.setText(usr);
        // Listener for invitations
        SwingWorker<Void, Void> invitations_listener = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                while (true) {
                    // This function doesn't handle 
                    try {
                        sock_lock.lock();
                        Optional<OpKind> maybeInvite = IOUtils.tryReadOpKind(chnl);
                        if (maybeInvite.isPresent()) {
                            handleInviteOp();
                        }
                    } catch (IOException e) {
                        UserLog("Connection problems reading invitations details", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ChannelClosedException e) {
                        UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        sock_lock.unlock();
                    }
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            }
        };
        invitations_listener.execute();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        showUsrTextbox = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        chatPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        readMessagesTextarea = new javax.swing.JTextArea();
        writeMessageTextbox = new javax.swing.JTextField();
        sendMessageButton = new javax.swing.JButton();
        docsInteractionPanel = new javax.swing.JPanel();
        newDocNameTb = new javax.swing.JTextField();
        createDocBtn = new javax.swing.JButton();
        newDocSecNumSpinner = new javax.swing.JSpinner();
        editDocNameTb = new javax.swing.JTextField();
        editSecNumSpinner = new javax.swing.JSpinner();
        editBtn = new javax.swing.JButton();
        endEditBtn = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        editingTb = new javax.swing.JTextField();
        listDocBtn = new javax.swing.JButton();
        inviteUsrTb = new javax.swing.JTextField();
        inviteDocumentTb = new javax.swing.JTextField();
        inviteBtn = new javax.swing.JButton();
        showSecBtn = new javax.swing.JButton();
        showDocBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(600, 400));

        showUsrTextbox.setEditable(false);
        showUsrTextbox.setToolTipText("");

        jLabel1.setText("Logged as:");

        jLabel2.setText("Chat");

        readMessagesTextarea.setEditable(false);
        readMessagesTextarea.setColumns(20);
        readMessagesTextarea.setRows(5);
        jScrollPane1.setViewportView(readMessagesTextarea);

        writeMessageTextbox.setEditable(false);

        sendMessageButton.setText("Send");
        sendMessageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendMessageButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout chatPanelLayout = new javax.swing.GroupLayout(chatPanel);
        chatPanel.setLayout(chatPanelLayout);
        chatPanelLayout.setHorizontalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chatPanelLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(190, 205, Short.MAX_VALUE))
            .addComponent(jScrollPane1)
            .addGroup(chatPanelLayout.createSequentialGroup()
                .addComponent(writeMessageTextbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sendMessageButton))
        );
        chatPanelLayout.setVerticalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chatPanelLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sendMessageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(writeMessageTextbox))
                .addContainerGap())
        );

        createDocBtn.setText("Create");
        createDocBtn.setToolTipText("Create new document");
        createDocBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createDocBtnActionPerformed(evt);
            }
        });

        editBtn.setText("Edit");
        editBtn.setToolTipText("Create new document");
        editBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editBtnActionPerformed(evt);
            }
        });

        endEditBtn.setText("End edit");
        endEditBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endEditBtnActionPerformed(evt);
            }
        });

        jLabel3.setText("Editing:");

        editingTb.setEditable(false);

        listDocBtn.setText("Show editable documents");
        listDocBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listDocBtnActionPerformed(evt);
            }
        });

        inviteBtn.setText("Invite");
        inviteBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inviteBtnActionPerformed(evt);
            }
        });

        showSecBtn.setText("Show section");
        showSecBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSecBtnActionPerformed(evt);
            }
        });

        showDocBtn.setText("Show document");
        showDocBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDocBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout docsInteractionPanelLayout = new javax.swing.GroupLayout(docsInteractionPanel);
        docsInteractionPanel.setLayout(docsInteractionPanelLayout);
        docsInteractionPanelLayout.setHorizontalGroup(
            docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(docsInteractionPanelLayout.createSequentialGroup()
                .addComponent(newDocNameTb)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(newDocSecNumSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(createDocBtn))
            .addComponent(listDocBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(docsInteractionPanelLayout.createSequentialGroup()
                .addComponent(inviteUsrTb, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inviteDocumentTb)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inviteBtn)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, docsInteractionPanelLayout.createSequentialGroup()
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editDocNameTb, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                    .addGroup(docsInteractionPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editingTb)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(endEditBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(docsInteractionPanelLayout.createSequentialGroup()
                        .addComponent(editSecNumSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editBtn))))
            .addGroup(docsInteractionPanelLayout.createSequentialGroup()
                .addComponent(showSecBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showDocBtn)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        docsInteractionPanelLayout.setVerticalGroup(
            docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(docsInteractionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(newDocNameTb, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(createDocBtn))
                    .addComponent(newDocSecNumSpinner))
                .addGap(18, 18, 18)
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(editDocNameTb, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(editBtn))
                    .addComponent(editSecNumSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editingTb, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(endEditBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(showSecBtn)
                    .addComponent(showDocBtn))
                .addGap(108, 108, 108)
                .addGroup(docsInteractionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inviteUsrTb, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(inviteDocumentTb, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(inviteBtn))
                .addGap(18, 18, 18)
                .addComponent(listDocBtn)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(docsInteractionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chatPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showUsrTextbox))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(showUsrTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(docsInteractionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void createDocBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createDocBtnActionPerformed
        String docname = newDocNameTb.getText();
        int secnum = (Integer)newDocSecNumSpinner.getValue();
        if (!docname.equals("")) {
            if (secnum != 0) {
                try {
                    lockSocket();
                    IOUtils.writeOpKind(OpKind.OP_CREATE, chnl);
                    IOUtils.writeString(docname, chnl);
                    IOUtils.writeInt(secnum, chnl);
                    OpKind resp = getNonInviteOpKind();
                    switch (resp) {
                        case RESP_OK:
                            this.UserLog("Document created succesfully");
                            break;
                        case ERR_DOCUMENT_EXISTS:
                            this.UserLog("Can't create document: already exists", "Error", JOptionPane.ERROR_MESSAGE);
                            break;
                        case ERR_RETRY:
                            throw new IOException();
                        default:
                            this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                } catch (IOException ex) {
                    this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (ChannelClosedException ex) {
                    this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    unlockSocket();
                }
            }
            else {
                this.UserLog("Can't create a document with 0 sections", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            this.UserLog("Can't create a document without a name", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_createDocBtnActionPerformed

    private void editBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editBtnActionPerformed
        String docname = editDocNameTb.getText();
        int secnum = (Integer)editSecNumSpinner.getValue();
        if (!docname.equals("")) {
            try {
                lockSocket();
                IOUtils.writeOpKind(OpKind.OP_EDIT, chnl);
                IOUtils.writeString(docname, chnl);
                IOUtils.writeInt(secnum, chnl);
                OpKind resp = getNonInviteOpKind();
                switch (resp) {
                    case RESP_OK:
                        Path file = this.ChooseFile(false);
                        IOUtils.channelToFile(chnl, file);
                        editingTb.setText(docname + "#" + Integer.toString(secnum));
                        byte chat_byte = IOUtils.readByte(chnl);
                        activateChat(chat_byte);
                        this.UserLog("Section saved to " + file.toString());
                        break;
                    case ERR_NO_DOCUMENT:
                        this.UserLog("Can't edit document: doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_WRONG_DOCNAME:
                        this.UserLog("Can't edit document: wrong document name", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_PERMISSION:
                        this.UserLog("Can't edit document: you don't have the permission", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_NO_SECTION:
                        this.UserLog("Can't edit section: doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_SECTION_BUSY:
                        this.UserLog("Can't edit section: already being edited", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_USER_BUSY:
                        this.UserLog("Can't edit section: you're already editing something", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_RETRY:
                        throw new IOException();
                    default:
                        this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                }
            } catch (IOException ex) {
                this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ChannelClosedException ex) {
                this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
            }
            finally {
                unlockSocket();
            }
        }
        else {
            this.UserLog("Can't create a document without a name", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_editBtnActionPerformed

    private void endEditBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endEditBtnActionPerformed
        try {
            lockSocket();
            IOUtils.writeOpKind(OpKind.OP_ENDEDIT, chnl);
            OpKind resp = getNonInviteOpKind();
            switch (resp) {
                case RESP_OK:
                    Path file = this.ChooseFile(false);
                    editingTb.setText("");
                    IOUtils.fileToChannel(file, chnl);
                    deactivateChat();
                    this.UserLog("Edit finished sucessfully");
                    break;
                case ERR_USER_FREE:
                    this.UserLog("Can't end edit: you aren't editing anything", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
                case ERR_RETRY:
                    throw new IOException();
                default:
                    this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (IOException ex) {
            this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ChannelClosedException ex) {
            this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            unlockSocket();
        }
    }//GEN-LAST:event_endEditBtnActionPerformed

    private void listDocBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listDocBtnActionPerformed
        try {
            lockSocket();
            IOUtils.writeOpKind(OpKind.OP_LISTDOCS, chnl);
            OpKind resp = getNonInviteOpKind();
            switch (resp) {
                case RESP_OK:
                    int ndocs = IOUtils.readInt(chnl);
                    StringBuilder listBuilder = new StringBuilder();
                    for (int i = 0; i < ndocs; ++i) {
                        listBuilder.append(IOUtils.readString(chnl));
                        listBuilder.append("\n");
                    }
                    this.UserLog(listBuilder.toString(), "Elenco documenti", JOptionPane.PLAIN_MESSAGE);
                    break;
                case ERR_RETRY:
                    throw new IOException();
                default:
                    this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (IOException ex) {
            this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ChannelClosedException ex) {
            this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            unlockSocket();
        }
    }//GEN-LAST:event_listDocBtnActionPerformed

    private void inviteBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inviteBtnActionPerformed
        try {
            lockSocket();
            IOUtils.writeOpKind(OpKind.OP_INVITE, chnl);
            IOUtils.writeString(inviteUsrTb.getText(), chnl);
            IOUtils.writeString(inviteDocumentTb.getText(), chnl);
            OpKind resp = getNonInviteOpKind();
            switch (resp) {
                case RESP_OK:
                    this.UserLog("User invited succesfully");
                    break;
                case ERR_RETRY:
                    throw new IOException();
                default:
                    this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (IOException ex) {
            this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ChannelClosedException ex) {
            this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            unlockSocket();
        }
    }//GEN-LAST:event_inviteBtnActionPerformed

    private void showSecBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showSecBtnActionPerformed
        String docname = editDocNameTb.getText();
        int secnum = (Integer)editSecNumSpinner.getValue();
        if (!docname.equals("")) {
            try {
                lockSocket();
                IOUtils.writeOpKind(OpKind.OP_SHOWSEC, chnl);
                IOUtils.writeString(docname, chnl);
                IOUtils.writeInt(secnum, chnl);
                OpKind resp = getNonInviteOpKind();
                switch (resp) {
                    case RESP_OK:
                        Path file = this.ChooseFile(false);
                        if (IOUtils.readBool(chnl)) {
                            UserLog("This section is being edited right now");
                        }
                        IOUtils.channelToFile(chnl, file);
                        this.UserLog("Section saved to " + file.toString());
                        break;
                    case ERR_WRONG_DOCNAME:
                        this.UserLog("Can't show section: wrong document name", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_NO_DOCUMENT:
                        this.UserLog("Can't edit section: document doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_NO_SECTION:
                        this.UserLog("Can't show section: doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_RETRY:
                        throw new IOException();
                    default:
                        this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                }
            } catch (IOException ex) {
                this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ChannelClosedException ex) {
                this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
            }
            finally {
                unlockSocket();
            }
        }
        else {
            this.UserLog("Can't get a section of a document without a name", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_showSecBtnActionPerformed

    private void showDocBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDocBtnActionPerformed
        String docname = editDocNameTb.getText();
        if (!docname.equals("")) {
            try {
                lockSocket();
                IOUtils.writeOpKind(OpKind.OP_SHOWDOC, chnl);
                IOUtils.writeString(docname, chnl);
                OpKind resp = getNonInviteOpKind();
                switch (resp) {
                    case RESP_OK:
                        int nsec = IOUtils.readInt(chnl);
                        Path file = this.ChooseFile(true);
                        for (Integer i = 0; i < nsec; ++i) {
                            if (IOUtils.readBool(chnl)) {
                                UserLog("Section " + Integer.toString(i) + " is being edited right now");
                            }
                            IOUtils.channelToFile(chnl, file.resolve(i.toString()));
                        }
                        this.UserLog("Sections saved to " + file.toString());
                        break;
                    case ERR_WRONG_DOCNAME:
                        this.UserLog("Can't show section: wrong document name", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_NO_DOCUMENT:
                        this.UserLog("Can't edit section: document doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_NO_SECTION:
                        this.UserLog("Can't show section: doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case ERR_RETRY:
                        throw new IOException();
                    default:
                        this.UserLog("Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                }
            } catch (IOException ex) {
                this.UserLog("Connection problems: try again", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ChannelClosedException ex) {
                this.UserLog("Connection to the server lost: restart the application", "Error", JOptionPane.ERROR_MESSAGE);
            }
            finally {
                unlockSocket();
            }
        }
        else {
            this.UserLog("Can't get a document without a name", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_showDocBtnActionPerformed

    private void sendMessageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendMessageButtonActionPerformed
        try {
            sendChatMsg(writeMessageTextbox.getText());           
        }
        catch (IOException e) {
            UserLog("Error sending message to the chat", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_sendMessageButtonActionPerformed

    
    private void lockSocket() throws IOException {
        sock_lock.lock();
        chnl.configureBlocking(true);
    }
    
    private void unlockSocket() {
        try {
            chnl.configureBlocking(false);
        }
        catch (IOException ex) {
            UserLog("Error reconfiguring socket: it may not receive invitations", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            sock_lock.unlock();
        }
    }
    
    private Path ChooseFile(boolean dir) {
        JFileChooser fileChooser = new JFileChooser();
        if (dir) {
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
        }
        else {
            return null;
        }
    }

    private void handleInviteOp() throws IOException, ChannelClosedException {
        String docInvited = IOUtils.readString((chnl));
        UserLog("You have been invited to edit " + docInvited, "Invite", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private OpKind getNonInviteOpKind() throws IOException, ChannelClosedException {
        OpKind resp = IOUtils.readOpKind(chnl);
        if (resp == OpKind.OP_INVITE) {
            handleInviteOp();
            return getNonInviteOpKind();
        }
        else {
            return resp;
        }
    }
    
    private void activateChat(byte last_byte) throws IOException {
        chat_active = true;
        writeMessageTextbox.setEditable(chat_active);
        byte[] ip_addr = new byte[4];
        System.arraycopy(Constants.multicast_base_addr, 0, ip_addr, 0, 3);
        ip_addr[3] = last_byte;
        chat_addr = InetAddress.getByAddress(ip_addr);
        chat_listener = new ChatListener(chat_addr, readMessagesTextarea);
        chat_listener.execute();
    }
    
    private void sendChatMsg(String msg) throws IOException {
        if (chat_active) {
            byte[] buff = (usr + ": " + msg).getBytes(encoding);
            DatagramPacket pk = new DatagramPacket(buff, buff.length, chat_addr, Constants.multicast_port);
            chat_sock.send(pk);
            writeMessageTextbox.setText("");
        }
    }
    
    private void deactivateChat() {
        chat_listener.stop();
        chat_active = false;
        writeMessageTextbox.setEditable(chat_active);
        writeMessageTextbox.setText("");
        chat_addr = null;
    }
    
    private void UserLog(String s) {
        JOptionPane.showMessageDialog(this, s);
    }
    
    private void UserLog(String s, String title, int messageType) {
        JOptionPane.showMessageDialog(this, s, title, messageType);
    }
    
    private static void StaticLog(String s) {
        System.out.println(s);
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
            java.util.logging.Logger.getLogger(InteractionWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(InteractionWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(InteractionWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(InteractionWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new InteractionWindow().setVisible(true);
            } catch (IOException e) {
                StaticLog("Error connecting to the server: " + e.getMessage());
                System.exit(1);
            } catch (Exception e) {
                StaticLog(e.getMessage());
                System.exit(1);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel chatPanel;
    private javax.swing.JButton createDocBtn;
    private javax.swing.JPanel docsInteractionPanel;
    private javax.swing.JButton editBtn;
    private javax.swing.JTextField editDocNameTb;
    private javax.swing.JSpinner editSecNumSpinner;
    private javax.swing.JTextField editingTb;
    private javax.swing.JButton endEditBtn;
    private javax.swing.JButton inviteBtn;
    private javax.swing.JTextField inviteDocumentTb;
    private javax.swing.JTextField inviteUsrTb;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton listDocBtn;
    private javax.swing.JTextField newDocNameTb;
    private javax.swing.JSpinner newDocSecNumSpinner;
    private javax.swing.JTextArea readMessagesTextarea;
    private javax.swing.JButton sendMessageButton;
    private javax.swing.JButton showDocBtn;
    private javax.swing.JButton showSecBtn;
    private javax.swing.JTextField showUsrTextbox;
    private javax.swing.JTextField writeMessageTextbox;
    // End of variables declaration//GEN-END:variables
}
