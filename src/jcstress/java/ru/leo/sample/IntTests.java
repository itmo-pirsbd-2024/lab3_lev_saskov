/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package ru.leo.sample;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

public class IntTests {
    @JCStressTest
    // These are the test outcomes.
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @State
    public static class IntTest {
        int v;

        @Actor
        public void actor1() {
            v++;
        }

        @Actor
        public void actor2() {
            v++;
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = v;
        }
    }

    @JCStressTest
    // These are the test outcomes.
    @Outcome(id = "1", expect = ACCEPTABLE_INTERESTING, desc = "One update lost: atomicity failure.")
    @Outcome(id = "2", expect = ACCEPTABLE, desc = "Actors updated independently.")
    @State
    public static class AtomicIntTest {
        AtomicInteger v = new AtomicInteger();

        @Actor
        public void actor1() {
            v.incrementAndGet();
        }

        @Actor
        public void actor2() {
            v.incrementAndGet();
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = v.get();
        }
    }
}