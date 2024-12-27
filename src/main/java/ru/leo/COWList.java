package ru.leo;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class COWList<T> implements List<T> {
    private final Object[] lock = new Object[0];
    private volatile Object[] array;

    final void setArray(Object[] a) {
        array = a;
    }

    final Object[] getArray() {
        return array;
    }

    public COWList() {
        setArray(new Object[0]);
    }

    public COWList(Collection<? extends T> c) {
        Object[] copy;
        if (c.getClass() == COWList.class) {
            copy = ((COWList<?>) c).getArray();
        } else {
            copy = c.toArray();
        }
        setArray(copy);
    }

    public COWList(T[] arr) {
        setArray(Arrays.copyOf(arr, arr.length, Object[].class));
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public boolean add(T t) {
        synchronized (lock) {
            Object[] es = getArray();
            int len = es.length;
            es = Arrays.copyOf(es, len + 1);
            es[len] = t;
            setArray(es);
            return true;
        }
    }

    @Override
    public void add(int index, T element) {
        synchronized (lock) {
            Object[] es = getArray();
            int len = es.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException();
            }
            Object[] newElements;
            int numMoved = len - index;
            if (numMoved == 0) {
                newElements = Arrays.copyOf(es, len + 1);
            } else {
                // Сдвигаем элементы вправо.
                newElements = new Object[len + 1];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index, newElements, index + 1,
                    numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        }
    }

    @Override
    public T get(int index) {
        return elementAt(getArray(), index);
    }

    @Override
    public T set(int index, T element) {
        synchronized (lock) {
            Object[] es = getArray();
            T oldValue = elementAt(es, index);

            if (oldValue != element) {
                es = es.clone();
                es[index] = element;
            }
            // Ensure volatile write semantics even when oldvalue == element
            setArray(es);
            return oldValue;
        }
    }

    @Override
    public T remove(int index) {
        synchronized (lock) {
            Object[] es = getArray();
            int len = es.length;
            T oldValue = elementAt(es, index);
            int numMoved = len - index - 1;
            Object[] newElements;
            if (numMoved == 0) {
                newElements = Arrays.copyOf(es, len - 1);
            } else {
                newElements = new Object[len - 1];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index + 1, newElements, index,
                    numMoved);
            }
            setArray(newElements);
            return oldValue;
        }
    }

    @Override
    public boolean remove(Object o) {
        Object[] snapshot = getArray();
        int index = indexOfRange(o, snapshot, 0, snapshot.length);
        return index != -1 && remove(o, snapshot, index);
    }

    private boolean remove(Object o, Object[] snapshot, int index) {
        synchronized (lock) {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                findIndex:
                {
                    int prefix = Math.min(index, len);
                    for (int i = 0; i < prefix; i++) {
                        if (current[i] != snapshot[i]
                            && Objects.equals(o, current[i])) {
                            index = i;
                            break findIndex;
                        }
                    }
                    if (index >= len) {
                        return false;
                    }
                    if (current[index] == o) {
                        break findIndex;
                    }
                    index = indexOfRange(o, current, index, len);
                    if (index < 0) {
                        return false;
                    }
                }
            }
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1,
                newElements, index,
                len - index - 1);
            setArray(newElements);
            return true;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Object[] es = getArray();
        int len = es.length;
        for (Object e : c) {
            if (indexOfRange(e, es, 0, len) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        Object[] cs = (c.getClass() == COWList.class) ?
            ((COWList<?>) c).getArray() : c.toArray();
        if (cs.length == 0) {
            return false;
        }
        synchronized (lock) {
            Object[] es = getArray();
            int len = es.length;
            Object[] newElements;
            if (len == 0) {
                newElements = cs;
            } else {
                newElements = Arrays.copyOf(es, len + cs.length);
                System.arraycopy(cs, 0, newElements, len, cs.length);
            }
            setArray(newElements);
            return true;
        }
    }

    /**
     *
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c collection containing elements to be added to this list
     */
    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        Object[] cs = c.toArray();
        synchronized (lock) {
            Object[] es = getArray();
            int len = es.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (cs.length == 0) {
                return false;
            }
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0) {
                newElements = Arrays.copyOf(es, len + cs.length);
            } else {
                newElements = new Object[len + cs.length];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index,
                    newElements, index + cs.length,
                    numMoved);
            }
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(c::contains);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }

    @Override
    public void clear() {
        synchronized (lock) {
            setArray(new Object[0]);
        }
    }

    /**
     * @return -1 if not presented, index otherwise.
     */
    @Override
    public int indexOf(Object o) {
        Object[] cur = getArray();
        return indexOfRange(o, cur, 0, cur.length);
    }

    @Override
    public int lastIndexOf(Object o) {
        Object[] es = getArray();
        return lastIndexOfRange(o, es, 0, es.length);
    }

    @Override
    public Object[] toArray() {
        return getArray().clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1> T1[] toArray(T1[] a) {
        Object[] es = getArray();
        int len = es.length;
        if (a.length < len) {
            return (T1[]) Arrays.copyOf(es, len, a.getClass());
        } else {
            System.arraycopy(es, 0, a, 0, len);
            if (a.length > len) {
                a[len] = null;
            }
            return a;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new COWIterator<>(getArray(), 0);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new COWIterator<>(getArray(), 0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        Object[] es = getArray();
        int len = es.length;
        if (index < 0 || index > len) {
            throw new IndexOutOfBoundsException();
        }

        return new COWIterator<>(es, index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }

        List<?> list = (List<?>) o;
        Iterator<?> it = list.iterator();
        for (Object element : getArray()) {
            if (!it.hasNext() || !Objects.equals(element, it.next())) {
                return false;
            }
        }
        return !it.hasNext();
    }

    private boolean bulkRemove(Predicate<? super T> filter) {
        synchronized (lock) {
            return bulkRemove(filter, 0, getArray().length);
        }
    }

    private boolean bulkRemove(Predicate<? super T> filter, int i, int end) {
        assert Thread.holdsLock(lock);

        final Object[] es = getArray();

        // Optimize for initial run of survivors
        for (; i < end && !filter.test(elementAt(es, i)); i++) {
            // Ignored
        }
        if (i < end) {
            final int beg = i;
            final long[] deathRow = nBits(end - beg);
            int deleted = 1;
            deathRow[0] = 1L;   // set bit 0
            for (i = beg + 1; i < end; i++) {
                if (filter.test(elementAt(es, i))) {
                    setBit(deathRow, i - beg);
                    deleted++;
                }
            }
            // Did filter reentrantly modify the list?
            if (es != getArray()) {
                throw new ConcurrentModificationException();
            }
            final Object[] newElts = Arrays.copyOf(es, es.length - deleted);
            int w = beg;
            for (i = beg; i < end; i++) {
                if (isClear(deathRow, i - beg)) {
                    newElts[w++] = es[i];
                }
            }
            System.arraycopy(es, i, newElts, w, es.length - i);
            setArray(newElts);
            return true;
        } else {
            if (es != getArray()) {
                throw new ConcurrentModificationException();
            }
            return false;
        }
    }

    private static long[] nBits(int n) {
        return new long[((n - 1) >> 6) + 1];
    }

    private static void setBit(long[] bits, int i) {
        bits[i >> 6] |= 1L << i;
    }

    private static boolean isClear(long[] bits, int i) {
        return (bits[i >> 6] & (1L << i)) == 0;
    }

    /**
     * @return -1 if not presented, index otherwise.
     */
    private static int indexOfRange(Object o, Object[] arr, int from, int to) {
        if (o == null) {
            for (int i = from; i < to; i++) {
                if (arr[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = from; i < to; i++) {
                if (o.equals(arr[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int lastIndexOfRange(Object o, Object[] es, int from, int to) {
        if (o == null) {
            for (int i = to - 1; i >= from; i--) {
                if (es[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = to - 1; i >= from; i--) {
                if (o.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <E> E elementAt(Object[] a, int index) {
        return (E) a[index];
    }

    static final class COWIterator<E> implements ListIterator<E> {
        /** Snapshot of the array */
        private final Object[] snapshot;
        /** Index of element to be returned by subsequent call to next.  */
        private int cursor;

        COWIterator(Object[] es, int initialCursor) {
            cursor = initialCursor;
            snapshot = es;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return (E) snapshot[cursor++];
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return (E) snapshot[--cursor];
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final int size = snapshot.length;
            int i = cursor;
            cursor = size;
            for (; i < size; i++) {
                action.accept(elementAt(snapshot, i));
            }
        }
    }
}
