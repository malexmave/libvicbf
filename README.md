# libvicbf
A java implementation of a Variable-Increment Counting Bloom Filter.

The VI-CBF was originally proposed by Rottenstreich *et al.* in their paper "[The Variable-Increment Counting Bloom Filter](http://www.cs.technion.ac.il/~ykanizo/papers/tr11-05_variable.pdf)", IEEE INFOCOM 2012,

The VI-CBF is an improvement over the regular CBF, as it provides a lower False Positive Rate with the same number of bits. It works by incrementing the counters of the bloom filter with variable values when inserting elements, as opposed to simply incrementing by one. This allows more accurate statements about the likelyhood that a certain element is in a certain filter.

For more details, check the [original paper](http://www.cs.technion.ac.il/~ykanizo/papers/tr11-05_variable.pdf).

This code is built for readability, not efficiency. If you need an efficient implementation, build your own :).

This implementation was built to be compatible with its python counterpart, [pyVICBF](https://github.com/malexmave/pyVICBF).
