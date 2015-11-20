package de.velcommuta.libvicbf;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.math.BigInteger;

/**
 * Implementation of a variable-increment counting bloom filter, as proposed by Rottenstreich et al.
 * in their paper "The Variable-Increment Counting Bloom Filter", IEEE INFOCOM 2012,
 *
 *     http://www.cs.technion.ac.il/~ykanizo/papers/tr11-05_variable.pdf
 *
 * The VI-CBF is an improvement over the regular CBF, as it provides a lower False Positive Rate
 * with the same number of bits. It works by incrementing the counters of the bloom filter with
 * variable values when inserting elements, as opposed to simply incrementing by one. This allows
 * more accurate statements about the likelyhood that a certain element is in a certain filter.
 *
 * For more details, read the original paper (linked above).
 * 
 * This implementation is optimized for readability, not for efficiency. If you need an efficient
 * implementation, you'll have to build your own. :)
 */
public class VICBF {
    private BigInteger mSlotsBI;
    private int mHashFunctions;
    private Hashtable<Short, Byte> mBloomFilter;
    private int L = 4;
    private BigInteger LBI = BigInteger.valueOf(4);

    /**
     * Constructor for new VICBF
     * @param slots Number of slots in the VICBF
     * @param expected_entries Expected number of entries
     * @param hash_functions Number of hash functions to use
     */
    public VICBF(int slots, int hash_functions) {
        if (slots < 1) {
            throw new IllegalArgumentException("slots must be at least 1");
        } else if (hash_functions < 1) {
            throw new IllegalArgumentException("Hash functions must be at least 1");
        }
        mSlotsBI = BigInteger.valueOf(slots);
        mHashFunctions = hash_functions;
        mBloomFilter = new Hashtable<>();
    }


    /**
     * Insert a key to the bloom filter
     * @param key The key, as string
     */
    public void insert(String key) {
    	for (int i = 0; i < mHashFunctions; i++) {
    		// Calculate slot and increment value
    		short slot = calculateSlot(key, i);
    		byte increment = calculateIncrement(key, i);
    		if (mBloomFilter.containsKey(slot)) {
    			// We have an existing value for that slot. Retrieve it and add the increment
    			byte existing = mBloomFilter.get(slot);
    			byte newvalue = (byte) (existing + increment);
    			// This calculation may have experienced an overflow. Check the result
    			if (newvalue < existing) {
    				// We have experienced a rollover. Fix value to maximum value of byte, 255
    				newvalue = (byte) 255;
    			}
    			// Put the value into the bloom filter
    			mBloomFilter.put(slot, newvalue);
    		} else {
    			// No existing value in the bloom filter. We can just insert the new value
    			mBloomFilter.put(slot, increment);
    		}
    	}
    }
    
    
    /**
     * Query the bloom filter for a key
     * @param key The key to query for
     * @return True if the key may have been inserted into the bloom filter, false if it definitely has not.
     */
    public boolean query(String key) {
    	for (int i = 0; i < mHashFunctions; i++) {
    		// Calculate slot and increment
    		short slot = calculateSlot(key, i);
    		byte decrement = calculateIncrement(key, i);
    		if (mBloomFilter.containsKey(slot)) {
    			 byte slotvalue = mBloomFilter.get(slot);
    			 int diff = slotvalue - decrement;
    			 if (diff < 0) {
    				 // The decrement value was larger than the value of the slot. This means that
    				 // the slot cannot have been incremented by it.
    				 return false;
    			 } else if (diff > 0 && diff < L) {
    				 // The difference is smaller than L. This is impossible if the key was inserted into
    				 // the bloom filter. Thus, it cannot have been inserted.
    				 return false;
    			 }
    		} else {
    			// No value in this slot means that the key cannot be contained in the bloom filter
    			return false;
    		}
    	}
    	// We have made it through all checks. This means that we cannot rule out that the key is
    	// in the bloom filter.
    	return true;
    }
    
    
    public void remove(String key) throws Exception {
    	List<String>  opList = new LinkedList<>();
    	List<Short> slotList = new LinkedList<>();
    	List<Byte> valueList = new LinkedList<>();
    	for (int i = 0; i < mHashFunctions; i++) {
    		short slot = calculateSlot(key, i);
    		byte decrement = calculateIncrement(key, i);
    		if (!mBloomFilter.containsKey(slot)) {
    			throw new Exception("Trying to delete key not contained in bloom filter");
    		}
    		byte bfval = mBloomFilter.get(slot);
    		if (bfval == (byte) 255) {
    			// The counter is at its maximum. We need to leave it fixed there, otherwise
    			// bad things can happen. Ignore this slot and move on.
    			continue;
    		} else if (bfval - decrement < 0) {
    			// Decrementing by this value would make the counter negative. Abort.
    			throw new Exception("Trying to delete key not contained in bloom filter");
    		} else if (bfval - decrement == 0) {
    			// Decrementing would set the counter to zero. Schedule deletion of the key.
    			opList.add("del");
    			slotList.add(slot);
    			valueList.add((byte) 0);
    		} else {
    			// Decrementing would result in a non-zero value. Schedule decrement
    			opList.add("decr");
    			slotList.add(slot);
    			valueList.add((byte) (bfval - decrement));
    		}
    	}
    	// Perform deferred operations
    	for (int i = 0; i < opList.size(); i++) {
    		if (opList.get(i).equals("del")) {
    			mBloomFilter.remove(slotList.get(i));
    		} else if (opList.get(i).equals("decr")) {
    			mBloomFilter.put(slotList.get(i), valueList.get(i));
    		}
    	}
    }


    /**
     * Private helper function to calculate the slot for a key and hash functions
     * @param key The key
     * @param i The index of the hash function
     * @return The slot (as short)
     */
    protected Short calculateSlot(String key, int i) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((key + i).getBytes("utf8"));
            // BigInteger interprets the input as signed, but we want unsigned,
            // as the python counterpart uses unsigned numbers. So, we need to
            // make sure that the number is always interpreted as positive. We
            // can achieve this by prepending a 0x00 to the hex value, thereby
            // nulling the sign.
            byte[] dst = new byte[digest.length + 1];
            System.arraycopy(new byte[] {0x00}, 0, dst, 0, 1);
            System.arraycopy(digest, 0, dst, 1, digest.length);
            // Interpret as a number and get the result modulo the number of
            // slots to determine the slot.
            return new BigInteger(dst).mod(mSlotsBI).shortValue();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    

    /**
     * Calculate the increment value, based on key and hash function
     * @param key The key
     * @param i index of the hash function
     * @return Increment, as byte
     */
    protected Byte calculateIncrement(String key, int i) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((-i + key).getBytes("utf8"));
            byte rv = new BigInteger(digest).mod(LBI).byteValue();
            return (byte) (rv + L);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // TODO Debugging helper, remove
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
