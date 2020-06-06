package anon961.kubert.utils;

import java.util.Random;

public class NameUtils {
    private final static String ALL_CHARS = "abcdefghijklmnopqrstuvwxyz1234567890";
    private static Random random = new Random(54479040);

    public static String randomString(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        while (stringBuilder.length() < length) { // length of the random string.
            int index = (int) (random.nextFloat() * ALL_CHARS.length());
            stringBuilder.append(ALL_CHARS.charAt(index));
        }

        return stringBuilder.toString();
    }
}
