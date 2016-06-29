package org.adsnative;

import java.util.HashMap;

/**
 * Created by kushagra on 6/18/16.
 */
public class Test {

    public static void main(String[] args) {
        
        String s = "a";
        String t = "a";

        System.out.println(isAnagram(s, t));
        
    }

    private static boolean isAnagram(String s, String t) {
        // build HashMap
        HashMap<Character, Integer> map = new HashMap<Character, Integer>();

        for (int i=0; i<s.length(); i++) {

            char x = s.charAt(i);

            if (map.containsKey(x)) {
                int count = map.get(x);
                map.put(x, count + 1);
            }
            else {
                map.put(x, 1);
            }
        }

        // de-build map
        for (int i=0; i<t.length(); i++) {
            char y = t.charAt(i);

            if (map.containsKey(y)) {
                int count = map.get(y);
                map.put(y, count - 1);
            }
            else {
                return false;
            }
        }


        // check validity
        for (char x : map.keySet()) {

            int count = map.get(x);
            if (x != 0) {
                return false;
            }
        }

        return true;

    }
}
