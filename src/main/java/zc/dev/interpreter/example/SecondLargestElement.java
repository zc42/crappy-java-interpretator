package zc.dev.interpreter.example;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import static zc.dev.interpreter.Utils.prnt;


class SecondLargestElement {

    static void print2largest(int[] arr) {
        Arrays.stream(arr)
                .boxed()
                .distinct()
                .sorted((o1, o2) -> Integer.compare(o2, o1))
                .limit(2)
                .findFirst()
                .ifPresent(e -> System.out.printf("The second largest element is %d\n", e));
    }

    private static Object compare(Object o2, Object o1) {
        return null;
    }

    private static IntUnaryOperator aaa() {
        return operand -> {
            prnt(operand);
            return operand;
        };
    }


    public static void main(String[] args) {
        int[] arr = {12, 35, 1, 10, 34, 1};
        print2largest(arr);
    }
}
