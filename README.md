A Groovy, performance enhanced implementation of pHash based off of
https://code.google.com/p/ironchef-team21/source/browse/ironchef_team21/src/ImagePHash.java?r=75856e07bb89645d0e56820d6e79f8219a06bfb7

Notably, this implementation reduces hashing time from ~1.5-2s to ~200ms.  A simple test and
sample image is included.

Some additional ideas for enhancements can be found at:

*  http://nekkidphpprogrammer.blogspot.fi/2014/02/yes-it-is-perfect-now.html
*  http://www.hackerfactor.com/blog/index.php?/archives/432-Looks-Like-It.html (comments section)


Test output:

Old hash took 2265 ms
0111000111001110011100011110111001111111011001100
success
New hash took 244 ms
0111000111001110011100011110111001111111011001100