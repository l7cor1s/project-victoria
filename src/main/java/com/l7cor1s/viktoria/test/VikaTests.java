package com.l7cor1s.viktoria.test;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of self tests. Filled in {@link VikaTestCases}.
 *
 * @author vyrriox
 */
public final class VikaTests {
    private VikaTests() {
    }

    public static List<TestRunner.TestCase> all() {
        List<TestRunner.TestCase> tests = new ArrayList<>();
        VikaTestCases.register(tests);
        WorkAreaTests.addTo(tests);
        MiscAreaTests.addTo(tests);
        CombatTests.addTo(tests);
        // One or several parts of test names, separated by commas.
        String only = System.getProperty("viktoria.selftest.only", "");
        if (!only.isBlank()) {
            List<String> parts = List.of(only.split(","));
            tests.removeIf(t -> parts.stream().map(String::trim).noneMatch(p -> !p.isEmpty() && t.name().contains(p)));
        }
        return tests;
    }
}
