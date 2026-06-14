package com.uxplima.uxmlib.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * {@link Reflect} reads any static field by name, so it can be exercised without the server runtime by reading
 * a well-known JDK constant and by pointing it at a field that does not exist. The accessor-on-NMS path is
 * compile-gated and boot-smoked; the read mechanism and its fail-loud contract are what is unit-testable here.
 */
class ReflectTest {

    @SuppressWarnings("unused") // read reflectively by Reflect.accessor in the test below
    private static final String KNOWN_VALUE = "value";

    @Test
    void readsAStaticFieldByName() {
        String value = Reflect.accessor(ReflectTest.class, "KNOWN_VALUE");
        assertThat(value).isEqualTo("value");
    }

    @Test
    void failsLoudlyNamingTheOwnerAndFieldOnAMiss() {
        assertThatThrownBy(() -> Reflect.accessor(ReflectTest.class, "MISSING_FIELD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ReflectTest.class.getName())
                .hasMessageContaining("MISSING_FIELD");
    }
}
