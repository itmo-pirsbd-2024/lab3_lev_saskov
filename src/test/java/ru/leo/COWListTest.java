package ru.leo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

class COWListTest {
    @COWTest
    void testGetByIndex(List<String> cow, List<String> simple) {
        assertThat(cow).isEqualTo(simple);
        for (int i = 0; i < simple.size(); i++) {
            assertThat(cow.get(i)).isEqualTo(simple.get(i));
        }
    }

    @COWTest
    void testIterator(List<String> cow, List<String> simple) {
        var iterator = cow.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            assertThat(iterator.next()).isEqualTo(simple.get(i));
            i++;
        }
    }

    @COWTest
    void testSet(List<String> cow, List<String> simple) {
        int index = cow.size() / 2;
        cow.set(index, "testSet");
        assertThat(cow.get(index)).isEqualTo("testSet");
        for (int i = 0; i < index; i++) {
            assertThat(cow.get(i)).isEqualTo(simple.get(i));
        }
        for (int i = index + 1; i < cow.size(); i++) {
            assertThat(cow.get(i)).isEqualTo(simple.get(i));
        }
    }

    @COWTest
    void testAdd(List<String> cow, List<String> simple) {
        cow.add("testAdd");
        assertThat(cow.getLast()).isEqualTo("testAdd");
        assertThat(cow.size()).isEqualTo(simple.size() + 1);
        for (int i = 0; i < simple.size(); i++) {
            assertThat(cow.get(i)).isEqualTo(simple.get(i));
        }
    }

    @COWTest
    void testAddAll(List<String> cow, List<String> simple) {
        var toAdd = List.of("toAdd1", "abec", "abs", "dmg");
        cow.addAll(toAdd);
        simple.addAll(toAdd);
        assertThat(cow).isEqualTo(simple);
    }

    @COWTest
    void testRemove(List<String> cow, List<String> simple) {
        int index = cow.size() / 2;
        var removed = cow.remove(index);
        assertThat(cow.contains(removed)).isFalse();
        assertThat(cow.size()).isEqualTo(simple.size() - 1);
        for (int i = 0; i < index; i++) {
            assertThat(cow.get(i)).isEqualTo(simple.get(i));
        }
        for (int i = index + 1; i < simple.size(); i++) {
            assertThat(simple.get(i)).isEqualTo(cow.get(i - 1));
        }
    }

    @COWTest
    void testRemoveAll(List<String> cow, List<String> simple) {
        var toRemove = new ArrayList<>(simple.subList(simple.size() / 4, simple.size() / 2));
        simple.removeAll(toRemove);
        cow.removeAll(toRemove);
        assertThat(cow).isEqualTo(simple);
    }
}