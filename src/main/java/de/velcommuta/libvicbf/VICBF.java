package de.velcommuta.libvicbf;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

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
    private int mSlots;
    private int mExpectedEntries;
    private int mHashFunctions;
    private Hashtable<Short, Byte> mBloomFilter;
    private int L = 4;

    /**
     * Constructor for new VICBF
     * @param slots Number of slots in the VICBF
     * @param expected_entries Expected number of entries
     * @param hash_functions Number of hash functions to use
     */
    public VICBF(int slots, int expected_entries, int hash_functions) {
        if (slots < 1) {
            throw new IllegalArgumentException("slots must be at least 1");
        } else if (expected_entries < 1) {
            throw new IllegalArgumentException("Expected Entries must be at least 1");
        } else if (hash_functions < 1) {
            throw new IllegalArgumentException("Hash functions must be at least 1");
        }
        mSlots = slots;
        mExpectedEntries = expected_entries;
        mHashFunctions = hash_functions;
        mBloomFilter = new Hashtable<>();
    }


    /**
     * Add a key to the bloom filter
     * @param key The key, as string
     */
    public void add(String key) {

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
            byte[] digest = md.digest((key + i).getBytes());
            return (short) (ByteBuffer.wrap(digest).getDouble() % mSlots);
        } catch (NoSuchAlgorithmException e) {
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
            byte[] digest = md.digest((-i + key).getBytes());
            return (byte) (ByteBuffer.wrap(digest).getDouble() % L);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
