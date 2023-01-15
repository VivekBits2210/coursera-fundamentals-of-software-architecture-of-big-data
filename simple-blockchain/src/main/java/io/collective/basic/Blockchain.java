package io.collective.basic;

import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Objects;

public class Blockchain {
    LinkedList<Block> chain = new LinkedList<>();
    boolean isValid = true;
    public boolean isEmpty() {
        return this.size()==0;
    }

    public void add(Block block) throws NoSuchAlgorithmException {
        if(!isMined(block) || !block.calculatedHash().equals(block.getHash()) || (!isEmpty() && !chain.getLast().getHash().equals(block.getPreviousHash())))
            isValid = false;
        chain.add(block);
    }

    public int size() {
        return chain.size();
    }

    public boolean isValid() {
        return isValid;
    }

    /// Supporting functions that you'll need.
    public static Block mine(Block block) throws NoSuchAlgorithmException {
        Block mined = new Block(block.getPreviousHash(), block.getTimestamp(), block.getNonce());

        while (!isMined(mined)) {
            mined = new Block(mined.getPreviousHash(), mined.getTimestamp(), mined.getNonce() + 1);
        }
        return mined;
    }

    public static boolean isMined(Block minedBlock) {
        return minedBlock.getHash().startsWith("00");
    }
}