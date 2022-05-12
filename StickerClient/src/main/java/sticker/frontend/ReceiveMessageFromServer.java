/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sticker.frontend;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReceiveMessageFromServer implements Runnable {

    private static final String OWN_MISSING_KEYWORD = "YourMissing:";
    private static final String OWN_DUPLICATES_KEYWORD = "YourDuplicates:";
    private static final String EXCHANGE_POSSIBILITIES_REPLY_KEYWORD = "ExchangePossibilities:";
    
    final private StickerClientMainWindow parent;
    final private BufferedReader br;
    
    final private StickerCollection missingStickersUI;
    final private StickerCollection duplicateStickersUI;
    
    public ReceiveMessageFromServer(StickerClientMainWindow parent) {
        this.parent = parent;
        this.br = parent.getBr();
        this.missingStickersUI = parent.getMissingStickersUI();
        this.duplicateStickersUI = parent.getDuplicateStickersUI();
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
                if ((positionX + StickerCollection.CHECKBOX_WIDTH + StickerCollection.CHECKBOX_OFFSET) > missingStickersUI.getCbPanelWidth())
                {
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
                if ((positionX + StickerCollection.CHECKBOX_WIDTH + StickerCollection.CHECKBOX_OFFSET) > duplicateStickersUI.getCbPanelWidth())
                {
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
            String[] lines = msg.split(":")[1].split("\r\n");
            for (String line : lines) {
                String[] data = line.split("=");
                String[] stickers = data[1].split("\\|")[0].split(" ");
                parent.appendUser(data[0] + " (" + Integer.toString(stickers.length) + ")");
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
