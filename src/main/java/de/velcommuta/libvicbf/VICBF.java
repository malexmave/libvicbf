package de.velcommuta.libvicbf;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.math.BigInteger;
import java.lang.Math;

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
 *
 * Copyright 2015 Max Maass
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class VICBF {
    private BigInteger mSlotsBI;
    private int mHashFunctions;
    private Hashtable<Integer, Byte> mBloomFilter;
    private int L = 4;
    private BigInteger LBI = BigInteger.valueOf(4);
    private int mCount = 0;

    /**
     * Constructor for new VICBF
     * @param slots Number of slots in the VICBF
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
    public void insert(byte[] key) {
    	for (int i = 0; i < mHashFunctions; i++) {
    		// Calculate slot and increment value
    		int slot = calculateSlot(key, i);
    		byte increment = calculateIncrement(key, i);
    		if (mBloomFilter.containsKey(slot)) {
    			// We have an existing value for that slot. Retrieve it and add the increment
    			byte existing = mBloomFilter.get(slot);
    			byte newvalue = (byte) (existing + increment);
    			// This calculation may have experienced an overflow. Check the result
    			if (newvalue < existing) {
    				// We have experienced a rollover. Fix value to maximum value of byte, 255
    				newvalue = (byte) 127;
    			}
    			// Put the value into the bloom filter
    			mBloomFilter.put(slot, newvalue);
    		} else {
    			// No existing value in the bloom filter. We can just insert the new value
    			mBloomFilter.put(slot, increment);
    		}
    	}
    	mCount = mCount + 1;
    }
    
    
    /**
     * Query the bloom filter for a key
     * @param key The key to query for
     * @return True if the key may have been inserted into the bloom filter, false if it definitely has not.
     */
    public boolean query(byte[] key) {
    	for (int i = 0; i < mHashFunctions; i++) {
    		// Calculate slot and increment
    		int slot = calculateSlot(key, i);
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
    
    
    public void remove(byte[] key) throws Exception {
    	List<String>  opList = new LinkedList<>();
    	List<Integer> slotList = new LinkedList<>();
    	List<Byte> valueList = new LinkedList<>();
    	for (int i = 0; i < mHashFunctions; i++) {
    		int slot = calculateSlot(key, i);
    		byte decrement = calculateIncrement(key, i);
    		if (!mBloomFilter.containsKey(slot)) {
    			throw new Exception("Trying to delete key not contained in bloom filter");
    		}
    		byte bfval = mBloomFilter.get(slot);
    		if (bfval == (byte) 127) {
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
    	mCount = mCount-1;
    }
    
    public int getSize() {
    	return mCount;
    }


    /**
     * Private helper function to calculate the slot for a key and hash functions
     * @param key The key
     * @param i The index of the hash function
     * @return The slot (as short)
     */
    protected int calculateSlot(byte[] key, int i) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] ctr = (""+i).getBytes("UTF8");
			byte[] input = new byte[key.length + ctr.length];
            System.arraycopy(key, 0, input, 0, key.length);
            System.arraycopy(ctr, 0, input, key.length, ctr.length);
            byte[] digest = md.digest(input);
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
            return new BigInteger(dst).mod(mSlotsBI).intValue();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return -1;
        }
    }
    

    /**
     * Calculate the increment value, based on key and hash function
     * @param key The key
     * @param i index of the hash function
     * @return Increment, as byte
     */
    protected Byte calculateIncrement(byte[] key, int i) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] ctr = ("-"+i).getBytes("UTF8");
            byte[] input = new byte[key.length + ctr.length];
            System.arraycopy(ctr, 0, input, 0, ctr.length);
            System.arraycopy(key, 0, input, ctr.length, key.length);
            byte[] digest = md.digest(input);
            byte rv = new BigInteger(digest).mod(LBI).byteValue();
            return (byte) (rv + L);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Helper function for deserialization, used to set the state of the counters
     * @param slot Slot of which the counter should be set
     * @param value Value of the counter
     */
    protected void setCounter(int slot, byte value) {
    	mBloomFilter.put(slot, value);
    }
    
    protected void setCount(int count) {
    	mCount = count;
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
    

	public static VICBF deserialize(String ser) throws IOException {
        // Convert String to byte[]
        byte[] hex = hexStringToByteArray(ser);
        return deserialize(hex);
    }

    public static VICBF deserialize(byte[] hex) throws IOException {
		// Wrap byte[] in DataInputStream
		DataInputStream di = new DataInputStream(new ByteArrayInputStream(hex));
		// The data format is:
		// - 1 bit mode flag (full dump vs. partial dump)
		// - 7 bit unsigned hash function count
		// - 32 bit unsigned slot count
		// - 32 bit unsigned number of entries
		// - 4 bit unsigned L base (see paper)
		// - 4 bit unsigned bits per counter (should always be 8)
		int flagAndHf = di.readUnsignedByte();
		boolean isFullDump = (flagAndHf & 128) == 0;
		int hashFunctions = flagAndHf & 127;
		int slots = di.readInt();
		int members = di.readInt();
		int vibaseAndBpc = di.readUnsignedByte();
		int vibase = (vibaseAndBpc & 240) >> 4; // 1111 0000 => First four bits of the byte
		int bpc = vibaseAndBpc & 15;     // 0000 1111 => Last four bits of the byte
		// TODO Check values for sanity
		// Create bloom filter and set count
		VICBF rv = new VICBF(slots, hashFunctions);
		rv.setCount(members);
		// Read counters and set values
		if (isFullDump) {
			for (int i = 0; i < slots; i++) {
				rv.setCounter(i, di.readByte());
			}
		} else {
			int bpi = (int) (Math.ceil((Math.log(slots) / Math.log(2)) / 8) * 8);
			try {
				if (bpi <= 32) {
					byte[] b = new byte[bpi / 8];
					while (true) {
						di.read(b);
						int slot = bytesToInt(b);
						if (slot < 0) {
							throw new IOException("Too many slots, do not fit into signed integer");
						}
						byte value = di.readByte();
						rv.setCounter(slot, value);
					}
				} else {
					throw new IOException("Too many slots, do not fit into signed integer");
				}
			} catch (EOFException e) {
				// End of file reached. This is to be expected, do nothing
			}
		}
		return rv;
	}
	
	protected static int bytesToInt(byte[] bytes) {
		int res = 0;
		for (int i = 0; i < bytes.length; i++) {
			res = res | (bytes[bytes.length - 1 - i] & 0xFF) << (8*i);
		}
		return res;
	}
	
	/**
	 * Convert a String containing a hexadecimal representation of a byte[] into a byte[].
	 * Credit: Dave L. on StackOverflow: http://stackoverflow.com/a/140861/1232833
	 * @param s The hex string
	 * @return The byte[]
	 */
	protected static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
}

