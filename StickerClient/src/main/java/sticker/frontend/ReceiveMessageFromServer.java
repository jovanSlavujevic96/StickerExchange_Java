/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sticker.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ReceiveMessageFromServer implements Runnable {

    private static final String OWN_MISSING_KEYWORD = "YourMissing:";
    private static final String OWN_DUPLICATES_KEYWORD = "YourDuplicates:";
    private static final String EXCHANGE_POSSIBILITIES_REPLY_KEYWORD = "ExchangePossibilities:";
    private static final String EXCHANGE_OFFER_KEYWORD = "ExchangeOffer:";
    private static final String EXCHANGE_REPLY_KEYWORD = "ExchangeReply:";
    private static final String EXCHANGE_OFFER_REPLY_KEYWORD = "ExchangeOfferReply:";
    private static final String EXCHANGE_OFFER_ACCEPT_KEYWORD = "ACCEPT";
    private static final String EXCHANGE_OFFER_REFUSE_KEYWORD = "REFUSE";
    
    private static final String ERROR_CODE_NOT_FOUND = "404";
    private static final String ERROR_CODE_BAD_AMOUNT = "500";
    private static final String ERROR_CODE_BAD_VALUE = "501";
    
    final private StickerClientMainWindow parent;
    final private BufferedReader br;
    final private PrintWriter pw;
    
    final private StickerCollection missingStickersUI;
    final private StickerCollection duplicateStickersUI;
    
    public ReceiveMessageFromServer(StickerClientMainWindow parent) {
        this.parent = parent;
        this.br = parent.getBr();
        this.pw = parent.getPw();
        this.missingStickersUI = parent.getMissingStickersUI();
        this.duplicateStickersUI = parent.getDuplicateStickersUI();
    }
    
    public static <T, U> List<U> convertStringListToIntList(List<T> listOfString, Function<T, U> function)
    {
        return listOfString.stream()
            .map(function)
            .collect(Collectors.toList());
    }
    
    private void processMessage(String msg) {
        // fill initial missing & duplicate stickers to UI
        if (msg.startsWith(OWN_MISSING_KEYWORD)) {
            final String Missing = msg.substring(OWN_MISSING_KEYWORD.length(), msg.indexOf(OWN_DUPLICATES_KEYWORD));
            final String Duplicates = msg.substring(msg.indexOf(OWN_DUPLICATES_KEYWORD) + OWN_DUPLICATES_KEYWORD.length());
            String[] missingStickers = Missing.split(" ");
            String[] duplicateStickers = Duplicates.split(" ");

            int positionX = 0;
            int positionY = 0;
            
            // filling missing
            for (String missing : missingStickers) {
                if ((positionX + StickerCollection.CHECKBOX_WIDTH + StickerCollection.CHECKBOX_OFFSET) > missingStickersUI.getCbPanelWidth()) {
                    positionX = 0;
                    positionY += StickerCollection.CHECKBOX_HEIGHT + StickerCollection.CHECKBOX_OFFSET;
                }
                missingStickersUI.appendCheckbox(missing, positionX, positionY);
                positionX += StickerCollection.CHECKBOX_WIDTH + StickerCollection.CHECKBOX_OFFSET;
            }

            positionX = 0;
            positionY = 0;
            
            // filling duplicates
            for (String duplicate : duplicateStickers) {
                if ((positionX + StickerCollection.CHECKBOX_WIDTH + StickerCollection.CHECKBOX_OFFSET) > duplicateStickersUI.getCbPanelWidth()) {
                    positionX = 0;
                    positionY += StickerCollection.CHECKBOX_HEIGHT + StickerCollection.CHECKBOX_OFFSET;
                }
                duplicateStickersUI.appendCheckbox(duplicate, positionX, positionY);
                positionX += StickerCollection.CHECKBOX_WIDTH + StickerCollection.CHECKBOX_OFFSET;
            }
            
            missingStickersUI.refreshWindow();
            duplicateStickersUI.refreshWindow();
        }
        // fill matching users for potential exchanges
        else if (msg.startsWith(EXCHANGE_POSSIBILITIES_REPLY_KEYWORD)) {
            String[] lines = msg.split(":")[1].split("____");
            for (String line : lines) {
                String[] data = line.split("=");
                String[] stickerGroups = data[1].split("\\|");
                String[] otherDuplicateStickers = stickerGroups[0].split(" ");
                String[] otherMissingStickers = stickerGroups[1].split(" ");
                parent.appendUser(data[0] + " (" + Integer.toString(otherMissingStickers.length) + ")", otherDuplicateStickers, otherMissingStickers);
            }
        }
        // handle exchange reply
        else if (msg.startsWith(EXCHANGE_REPLY_KEYWORD)) {
            final String errorCode = msg.split(":")[1];
            switch (errorCode) {
                case ERROR_CODE_NOT_FOUND:
                    JOptionPane.showMessageDialog(this.parent, "Trazeni korisnik nije pronadjen");
                    break;
                case ERROR_CODE_BAD_AMOUNT:
                    JOptionPane.showMessageDialog(this.parent, "Razmena je samo 1:1");
                    break;
                case ERROR_CODE_BAD_VALUE:
                    JOptionPane.showMessageDialog(this.parent, "U razmeni je losa slicica");
                    break;
                default:
                    break;
            }
            
        }
        // handle exchange offer
        else if (msg.startsWith(EXCHANGE_OFFER_KEYWORD)) {
            String[] lines = msg.split(":")[1].split("____");
            String[] users = lines[1].split("=");
            final String[] stickerGroups = lines[0].split("\\|");
            final List<String> otherDuplicateStickers = Arrays.asList(stickerGroups[0].split(" "));
            final List<String> otherMissingStickers = Arrays.asList(stickerGroups[1].split(" "));
            
            JPanel offerPanel = new JPanel();
            offerPanel.add(new JLabel(users[1]));
            offerPanel.add(new JLabel("Nudi: "  + stickerGroups[0]));
            offerPanel.add(new JLabel("Trazi: " + stickerGroups[1]));
            
            int result = JOptionPane.showConfirmDialog(
                this.parent,
                offerPanel,
                "Ponuda od " + users[1],
                JOptionPane.OK_CANCEL_OPTION
            );
            
            if (result == JOptionPane.OK_OPTION) {
                final List<Integer> deleteOwnDuplicate = convertStringListToIntList(otherMissingStickers, Integer::parseInt);
                final List<Integer> deleteOwnMissing = convertStringListToIntList(otherDuplicateStickers, Integer::parseInt);
                
                duplicateStickersUI.removeCheckboxes(deleteOwnDuplicate);
                missingStickersUI.removeCheckboxes(deleteOwnMissing);
                
                msg = EXCHANGE_OFFER_REPLY_KEYWORD + EXCHANGE_OFFER_ACCEPT_KEYWORD + ":";
                msg += stickerGroups[0] + "|" + stickerGroups[1] + ":" + lines[1];
            } else {
                msg = EXCHANGE_OFFER_REPLY_KEYWORD + EXCHANGE_OFFER_REFUSE_KEYWORD;
            }
            this.pw.println(msg);
        }
        /// handle accepted offer
        else if (msg.startsWith(EXCHANGE_OFFER_REPLY_KEYWORD)) {
            String[] lines = msg.split(":");
            boolean offerAccepted = lines[1].equals(EXCHANGE_OFFER_ACCEPT_KEYWORD);
            if (offerAccepted) {
                final String[] stickerGroups = lines[2].split("\\|");
                final String otherUsername = lines[3].split("=")[0];

                final List<String> ownDuplicateStickers = Arrays.asList(stickerGroups[0].split(" "));
                final List<String> ownMissingStickers = Arrays.asList(stickerGroups[1].split(" "));
                
                final List<Integer> deleteOwnDuplicate = convertStringListToIntList(ownDuplicateStickers, Integer::parseInt);
                final List<Integer> deleteOwnMissing = convertStringListToIntList(ownMissingStickers, Integer::parseInt);
                
                duplicateStickersUI.removeCheckboxes(deleteOwnDuplicate);
                missingStickersUI.removeCheckboxes(deleteOwnMissing);

                JOptionPane.showMessageDialog(this.parent, "Razmena sa " + otherUsername + " je uspesna!");
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            String msg;
            
            // recv
            try {
                msg = this.br.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ReceiveMessageFromServer.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            if (msg == null) {
                break;
            }
            
            // process
            processMessage(msg);
        }
    }

}
