/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.sort;


import java.util.LinkedList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Herald Kllapi <br>
 *         University of Athens /
 *         Department of Informatics and Telecommunications.
 * @since 1.0
 */
public class SortTest {

    private static Logger log = LoggerFactory.getLogger(SortTest.class);

    public static void main(String[] args) {
        long[] values = new long[10000];

        Random rand = new Random();
        for (int i = 0; i < values.length; i++) {
            values[i] = rand.nextInt();
            //   values[i] = values[i] & 0x00000000ffffffff;
            //   log.debug(values[i]);
        }

        LinkedList<LinkedList<Long>> lists = new LinkedList<LinkedList<Long>>();
        lists.add(new LinkedList<Long>());
        lists.add(new LinkedList<Long>());

        for (int i = 0; i < values.length; i++) {
            lists.get(getBucket(values[0], 33)).add(values[i]);
        }

        for (LinkedList<Long> l : lists) {
        }
    }

    public static int getBucket(long number, int bits) {
        return (int) (number >> (64 - bits)) & 0x0fffffff;
    }
}
