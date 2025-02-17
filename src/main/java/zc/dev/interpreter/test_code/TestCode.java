package zc.dev.interpreter.test_code;

import static zc.dev.interpreter.Utils.prnt;

public class TestCode {

    public static class A {
        private A a;
        private int level = 0;

        public A getChild() {
            a = a == null ? new A() : a;
            a.level = level + 1;
            prnt(a.level);
            return a;
        }

        private void xx(int x, int y) {
            int b = y == x
                    ? 1
                    : y < x
                    ? y == x * 5
                    ? 2 : 3
                    : 4;
            prnt(b);
        }
    }

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

            if (0 == b % 2) prnt("b % 2 == 0");
            else prnt("b % 2 != 0");

            if (c < 100) prnt("b < 100");
            else if (c > 200) prnt("b > 200");
            else prnt("else ..");
        }
        prnt("done");
        A x = new A().getChild().getChild().getChild().getChild();
        x.xx(1, 2);
    }

    private static int a(int a, int b) {
        return a + a * b;
    }
}
