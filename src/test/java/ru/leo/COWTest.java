package ru.leo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ParameterizedTest
@ArgumentsSource(COWTest.COWSource.class)
@interface COWTest {
    class COWSource implements ArgumentsProvider {
        private final List<String> EXPECTED = new ArrayList<>(
            List.of("str1", "str2", "abs", "dmg", "yok_makarek", "10", "1", "2", "3", "4")
        );
        private final List<String> ACTUAL_COW = new COWList<>(EXPECTED);

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(Arguments.of(ACTUAL_COW, EXPECTED));
        }
    }
}
