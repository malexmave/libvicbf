package de.velcommuta.libvicbf;

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
}
