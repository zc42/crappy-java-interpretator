package interpreter.test_code;

import static interpreter.Utils.prnt;

public class TestCode {

    public static void main(String[] args) {
        int a = 1 + 2;
        int b = 1 + a(a + 1, a(1, 2));
        b = a + b * (a + 1) - (a / (1 + 2));
        int c = a(b + 1, a(1, 2)) + 1;

        while (b > 0 || b + 1 > 0) {
            boolean d = !(a < c) && (b > c);
            prnt(d);
            prnt(b);
            prnt(c);
            boolean o = b > 0 || b + 1 > 0;
            prnt(o);
            b = b - 1;
        }
        prnt("done");
    }

    private static int a(int a, int b) {
        return a + a * b;
    }
}
