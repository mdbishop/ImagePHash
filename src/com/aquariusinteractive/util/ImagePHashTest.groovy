package com.aquariusinteractive.util

import groovy.transform.CompileStatic

/**
 * User: mbishop
 * Date: 5/14/14
 * Time: 7:09 AM
 *
 * (c) 2014 AQUARIUS HOLDINGS INC
 *
 */
@CompileStatic
class ImagePHashTest extends GroovyTestCase {

    InputStream inputStream

    void setUp() {
        super.setUp()
        inputStream = getClass().getResourceAsStream("dsm.jpg")
        assert inputStream
    }

    void tearDown() {
        inputStream?.close()
    }

    void testDistancePhash() {
        def phash = new ImagePHash()
        def hash1 = '01010000'
        def hash2 = '01010001'
        assert phash.distance(hash1, hash2) == 1
        println "success"
    }

    void testDistancePhash2() {
        def phash = new ImagePHash2()
        def hash1 = '01010000'
        def hash2 = '11010001'
        assert phash.distance(hash1, hash2) == 2
        println "success"
    }


    void testGetPHash() {
        long start = System.currentTimeMillis();
        def phash = new ImagePHash()
        def hash = phash.getHash(inputStream)
        println "Old hash took " + (System.currentTimeMillis() - start) + " ms"
        println hash
    }

    void testGetPHash2() {
        long start = System.currentTimeMillis();

        def phash = new ImagePHash2()
        def hash = phash.getHash(inputStream)
        println "New hash took " + (System.currentTimeMillis() - start) + " ms"

        println hash
    }

    void testOldAndNewHashesMatch() {
        def phash = new ImagePHash()
        def phash2 = new ImagePHash()

        def hash1 = phash.getHash(inputStream)
        setUp() // Inputstream is getting closed
        def hash2 = phash2.getHash(inputStream)

        assertEquals(hash1, hash2)
        println "success"
    }
}
