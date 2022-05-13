/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sticker.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jovan
 */
public class ConnectedStickerClient implements Runnable {
    private Socket socket;
    private String userName;
    private BufferedReader br;
    private PrintWriter pw;
    private ArrayList<ConnectedStickerClient> clients;
    private final ArrayList<Integer> duplicateStickers;
    private final ArrayList<Integer> missingStickers;
    
    private static final String OWN_MISSING_KEYWORD = "YourMissing:";
    private static final String OWN_DUPLICATES_KEYWORD = "YourDuplicates:";
    private static final String USERNAME_KEYWORD = "MyUserName:";
    private static final String REMOVED_MISSING_KEYWORD = "RemovedMissing:";
    private static final String REMOVED_DUPLICATES_KEYWORD = "RemovedDuplicates:";
    private static final String EXCHANGE_POSSIBILITIES_REQ_KEYWORD = "ExchangePossibilities.";
    private static final String EXCHANGE_POSSIBILITIES_REPLY_KEYWORD = "ExchangePossibilities:";
    private static final String EXCHANGE_REQ_KEYWORD = "ExchangeReq:";
    private static final String EXCHANGE_REPLY_KEYWORD = "ExchangeReply:";
    private static final String EXCHANGE_OFFER_KEYWORD = "ExchangeOffer:";
    private static final String EXCHANGE_OFFER_REPLY_KEYWORD = "ExchangeOfferReply:";
    private static final String EXCHANGE_OFFER_ACCEPT_KEYWORD = "ACCEPT";
    private static final String EXCHANGE_OFFER_REFUSE_KEYWORD = "REFUSE";
    
    private static final String ERROR_CODE_NOT_FOUND = "404";
    private static final String ERROR_CODE_BAD_AMOUNT = "500";
    private static final String ERROR_CODE_BAD_VALUE = "501";
    
    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public Socket getSocket() {
        return this.socket;
    }
    
    public void setSocket(Socket socket) {
        this.socket = socket;
    }
    
    public PrintWriter getPw() {
        return pw;
    }
    
    public ArrayList<Integer> getDuplicateStickers() {
        return duplicateStickers;
    }
    
    public ArrayList<Integer> getMissingStickers() {
        return missingStickers;
    }
    
    private int generateRandomNum(int min, int max) {
        return (int)Math.floor(Math.random()*(max-min+1) + min);
    }
    
    public ConnectedStickerClient(Socket socket, ArrayList<ConnectedStickerClient> clients) throws IOException {
        this.socket = socket;
        this.clients = clients;
        userName = "";
        
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException ex) {
            // just disclaimer
            throw ex;
        }
        
        final int duplicatesNum = generateRandomNum(35, 50);
        final int missingNum = generateRandomNum(34, 49);
        
        duplicateStickers = new ArrayList();
        missingStickers = new ArrayList();
        
        for (int i = 0; i < duplicatesNum; i++) {
            int tmp;
            do {
                tmp = generateRandomNum(1,99);
            } while(duplicateStickers.contains(tmp));
            duplicateStickers.add(tmp);
        }
        
        for (int i = 0; i < missingNum; i++) {
            int tmp;
            do {
                tmp = generateRandomNum(1,99);
            } while(duplicateStickers.contains(tmp) || missingStickers.contains(tmp));
            missingStickers.add(tmp);
        }
        
        Collections.sort(duplicateStickers);
        Collections.sort(missingStickers);
    }
    
    private ConnectedStickerClient checkUserWithinList(String username) {
        for (ConnectedStickerClient client : clients) {
            if (client.getUserName().equals(username)) {
                return client;
            }
        }
        return null;
    }
    
    private boolean checkUserStickers(ConnectedStickerClient user, String[] userDuplicates, String[] userMissing) {
        var duplicatesRef = user.getDuplicateStickers();
        var missingRef = user.getMissingStickers();

        for (String duplicate : userDuplicates) {
            if (!duplicatesRef.contains((Integer)Integer.parseInt(duplicate))) {
                return false;
            }
        }
        
        for (String missing : userMissing) {
            if (!missingRef.contains((Integer)Integer.parseInt(missing))) {
                return false;
            }
        }
        return true;
    }
    
    public String processMessage(String msg) {
        String out = "";

        // read username & pass generated missing and duplicate stickers
        if (userName.equals("") && msg.startsWith(USERNAME_KEYWORD)) {
            userName = msg.substring(USERNAME_KEYWORD.length());

            out = OWN_MISSING_KEYWORD;
            for (int miss : missingStickers) {
                out += Integer.toString(miss) + " ";
            }

            out += OWN_DUPLICATES_KEYWORD;
            for (int duplicate : duplicateStickers) {
                out += Integer.toString(duplicate) + " ";
            }
        }
        // read which missing stickers should be removed from list
        else if (msg.startsWith(REMOVED_MISSING_KEYWORD)) {
            String[] numbers = msg.split(":")[1].split(" ");
            for (String number : numbers) {
                missingStickers.remove((Integer)Integer.parseInt(number));
            }
        }
        // read which duplicate stickers should be removed from list
        else if (msg.startsWith(REMOVED_DUPLICATES_KEYWORD)) {
            String[] numbers = msg.split(":")[1].split(" ");
            for (String number : numbers) {
                duplicateStickers.remove((Integer)Integer.parseInt(number));
            }
        }
        // check exchange possibilities
        else if (msg.equals(EXCHANGE_POSSIBILITIES_REQ_KEYWORD)) {
            ArrayList<Integer> missingMatches;
            ArrayList<Integer> duplicateMatches;
            for (ConnectedStickerClient client : clients) {
                if (client.getUserName().equals(this.userName)) {
                    continue;
                }

                missingMatches = new ArrayList(client.getDuplicateStickers());
                missingMatches.retainAll(missingStickers);
                
                if (missingMatches.isEmpty()) {
                    continue;
                }
                
                duplicateMatches = new ArrayList(client.getMissingStickers());
                duplicateMatches.retainAll(duplicateStickers);
                
                if (!duplicateMatches.isEmpty()) {
                    if (out.equals("")) {
                        out = EXCHANGE_POSSIBILITIES_REPLY_KEYWORD;
                    }
                    
                    out += client.getUserName() + "=";
                    for (int match : missingMatches) {
                        out += Integer.toString(match) + " ";
                    }
                    out += "|";
                    for (int match : duplicateMatches) {
                        out += Integer.toString(match) + " ";
                    }
                    out += "____";
                }
            }
        }
        // check and pass exchange request
        else if (msg.startsWith(EXCHANGE_REQ_KEYWORD)) {
            String[] users = msg.split("____")[1].split("=");
            String[] stickerGroups = msg.split("____")[0].split(":")[1].split("\\|");
            String[] otherUserDuplicates = stickerGroups[0].split(" ");
            String[] otherUserMissing = stickerGroups[1].split(" ");
            
            ConnectedStickerClient otherUser;

            if (otherUserDuplicates.length != otherUserMissing.length) {
                out = EXCHANGE_REPLY_KEYWORD + ERROR_CODE_BAD_AMOUNT;
            } else if (checkUserWithinList(users[0]) == null || (otherUser = checkUserWithinList(users[1])) == null) {
                out = EXCHANGE_REPLY_KEYWORD + ERROR_CODE_NOT_FOUND;
            } else if (!checkUserStickers(otherUser, otherUserDuplicates, otherUserMissing)) {
                out = EXCHANGE_REPLY_KEYWORD + ERROR_CODE_BAD_VALUE;
            } else {
                // forward offer to other user
                msg = EXCHANGE_OFFER_KEYWORD;
                msg += stickerGroups[1] + "|" + stickerGroups[0] + "____";
                msg += users[1] + "=" + users[0];

                otherUser.getPw().println(msg); 
            }
        }
        // check offer response
        else if (msg.startsWith(EXCHANGE_OFFER_REPLY_KEYWORD)) {
            String[] lines = msg.split(":");
            boolean offerAccepted = lines[1].equals(EXCHANGE_OFFER_ACCEPT_KEYWORD);
            final String[] users = lines[3].split("=");
            
            // find other user
            ConnectedStickerClient otherUser = checkUserWithinList(users[1]);

            if (offerAccepted) {
                final String[] stickerGroups = lines[2].split("\\|");
                final String[] otherDuplicate = stickerGroups[0].split(" ");
                final String[] otherMissing = stickerGroups[1].split(" ");
                
                for (String number : otherDuplicate) {
                    Integer tmp = Integer.parseInt(number);
                    missingStickers.remove(tmp);
                    otherUser.getDuplicateStickers().remove(tmp);
                }

                for (String number : otherMissing) {
                    Integer tmp = Integer.parseInt(number);
                    duplicateStickers.remove(tmp);
                    otherUser.getMissingStickers().remove(tmp);
                }

                msg = EXCHANGE_OFFER_REPLY_KEYWORD + EXCHANGE_OFFER_ACCEPT_KEYWORD + ":";
                msg += stickerGroups[0] + "|" + stickerGroups[1] + ":" + lines[3];
            } else {
                msg = EXCHANGE_OFFER_REPLY_KEYWORD + EXCHANGE_OFFER_REFUSE_KEYWORD;
            }
            
            otherUser.getPw().println(msg); 
        }

        return out;
    }
    
    @Override
    public void run() {
        while (true) {
            String msg;

            // recv
            try {
                msg = this.br.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ConnectedStickerClient.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            if (msg == null) {
                System.out.println("Message rcv for client \"" + userName + "\" is null");
                break;
            }

            // process
            msg = processMessage(msg);

            // send
            if (!msg.equals("")) {
                pw.println(msg);
            }
        }

        // exit
        try {
            this.socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ConnectedStickerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        clients.remove(this);
    }
}
