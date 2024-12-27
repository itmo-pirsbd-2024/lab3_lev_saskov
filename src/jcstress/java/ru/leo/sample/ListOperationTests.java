package ru.leo.sample;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import java.util.ArrayList;
import java.util.List;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;
import ru.leo.COWList;

public class ListOperationTests {
    private static final List<String> NOT_CHANGED_LIST = List.of("a", "b", "c", "d", "e", "f");

    @JCStressTest
    @Outcome(id = "0", expect = FORBIDDEN, desc = "List not changed")
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @Outcome(id = "3", expect = FORBIDDEN, desc = "List corrupted")
    @State
    public static class TestSet {
        private final List<String> list = new COWList<>(NOT_CHANGED_LIST);

        @Actor
        public void actor1() {
            list.set(2, "2");
        }

        @Actor
        public void actor2() {
            list.set(3, "3");
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = result(list, List.of("a", "b", "2", "3", "e", "f"),
                l -> l.equals(List.of("a", "b", "2", "d", "e", "f"))
                    || l.equals(List.of("a", "b", "c", "3", "e", "f"))
            );
        }
    }

    @JCStressTest
    @Outcome(id = "0", expect = FORBIDDEN, desc = "List not changed")
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @Outcome(id = "3", expect = FORBIDDEN, desc = "List corrupted")
    @State
    public static class TestAddAll {
        private static final List<String> EXPECTED = new ArrayList<>(NOT_CHANGED_LIST) {{
            addAll(NOT_CHANGED_LIST);
            addAll(NOT_CHANGED_LIST);
        }};
        private final List<String> list = new COWList<>(NOT_CHANGED_LIST);

        @Actor
        public void actor1() {
            list.addAll(NOT_CHANGED_LIST);
        }

        @Actor
        public void actor2() {
            list.addAll(NOT_CHANGED_LIST);
        }

        @Arbiter
        public void arbiter(I_Result r) {
            List<String> interesing = new ArrayList<>(NOT_CHANGED_LIST) {{
                addAll(NOT_CHANGED_LIST);
            }};

            r.r1 = result(list, EXPECTED, l -> l.equals(interesing));
        }
    }

    @JCStressTest
    @Outcome(id = "0", expect = FORBIDDEN, desc = "List not changed")
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @Outcome(id = "3", expect = FORBIDDEN, desc = "List corrupted")
    @State
    public static class TestRemoveAll {
        private final List<String> list = new COWList<>(NOT_CHANGED_LIST);

        @Actor
        public void actor1() {
            list.removeAll(List.of("b", "c"));
        }

        @Actor
        public void actor2() {
            list.removeAll(List.of("e", "f"));
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = result(list, List.of("a", "d"),
                l -> l.equals(List.of("a", "b", "c", "d")) ||
                    l.equals(List.of("a", "b", "e", "f"))
            );
        }
    }

    private static int result(List<String> actual, List<String> expected, ListPredicate interesting) {
        if (actual.equals(NOT_CHANGED_LIST)) {
            return 0;
        } else if (interesting.is(actual)) {
            return 1;
        } else if (actual.equals(expected)) {
            return 2;
        } else {
            return 3;
        }
    }

    @FunctionalInterface
    private interface ListPredicate {
        boolean is(List<String> list);
    }
}
