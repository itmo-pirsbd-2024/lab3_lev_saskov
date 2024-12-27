package ru.leo.sample;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;
import ru.leo.COWList;

public class ListTests {
    @JCStressTest
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @State
    public static class ArrayListTest {
        List<String> list = new ArrayList<>();

        @Actor
        public void actor1() {
            list.add("a");
        }

        @Actor
        public void actor2() {
            list.add("b");
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = list.size();
        }
    }

    @JCStressTest
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @State
    public static class CopyOnWriteArrayListTest {
        List<String> list = new CopyOnWriteArrayList<>();

        @Actor
        public void actor1() {
            list.add("a");
        }

        @Actor
        public void actor2() {
            list.add("b");
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = list.size();
        }
    }

    @JCStressTest
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @State
    public static class MyCowListTest {
        List<String> list = new COWList<>();

        @Actor
        public void actor1() {
            list.add("a");
        }

        @Actor
        public void actor2() {
            list.add("b");
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = list.size();
        }
    }
}
