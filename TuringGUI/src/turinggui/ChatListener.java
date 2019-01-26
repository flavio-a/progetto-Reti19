/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinggui;

import java.beans.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import server.lib.Constants;

/**
 *
 * @author flavio
 */
public class ChatListener extends SwingWorker<Void, String> {
    private final JTextArea outputTa;
    private final MulticastSocket sock;
    
    public ChatListener(InetAddress addr, JTextArea messagesTa) throws IOException {
        outputTa = messagesTa;
        sock = new MulticastSocket(Constants.multicast_port);
        sock.joinGroup(addr);
    }

    @Override
    protected Void doInBackground() {
        byte[] buff = new byte[Constants.chat_msg_length];
        while (!this.isCancelled()) {
            DatagramPacket pk = new DatagramPacket(buff, buff.length);
            try {
                sock.receive(pk);
                String msg = new String(pk.getData(), 0, pk.getLength(), InteractionWindow.encoding);
                publish(msg);
            }
            catch (IOException e) {
                System.out.println("IO error while receiving time datagram");
            }
        }
        return null;
    }
    
    @Override
    protected void process(List<String> chunks) {
        for (String message: chunks) {
            outputTa.append(message + "\n");
        }
    }
    
    public void stop() {
        this.cancel(true);
        sock.close();
    }
}
