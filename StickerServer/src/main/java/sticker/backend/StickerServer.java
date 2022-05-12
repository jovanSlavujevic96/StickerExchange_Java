/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sticker.backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jovan
 */
public class StickerServer {
    private ServerSocket acceptSocket;
    private final ArrayList<ConnectedStickerClient> clients;
    
    public void acceptClients() throws IOException {
        Socket client_socket;
        Thread client_thread;
        ConnectedStickerClient client_handler;

        while (true) {
            System.out.println("Waiting for new clients..");
            try {
                client_socket = this.acceptSocket.accept();
            } catch (IOException ex) {
                Logger.getLogger(StickerServer.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            
            if (client_socket == null) {
                System.out.println("Accept client returned null socket");
                break;
            }
            try {
                client_handler = new ConnectedStickerClient(client_socket, clients);
            } catch (IOException ex) {
                Logger.getLogger(StickerServer.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            
            clients.add(client_handler);
            client_thread = new Thread(client_handler);
            client_thread.start();
        }
        
        // safe exit
        for (ConnectedStickerClient client : clients) {
            client.getSocket().shutdownInput();
            client.getSocket().close();
        }
        acceptSocket.close();
    }
    
    public StickerServer(int port) throws IOException {
        try {
            this.acceptSocket = new ServerSocket(port);
        } catch (IOException ex) {
            // just disclaimer
            throw ex;
        }
        clients = new ArrayList();
    }
    
    public static void main(String[] args) {
        StickerServer server;
        final int port = 6001;
        
        try {
            server = new StickerServer(port);
            server.acceptClients();
        } catch (IOException ex) {
            System.out.println("StickerServer create failed: " + ex.toString());
        }
    }
}
