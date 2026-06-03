package com.urlshortener.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Base62EncoderTest {

    @Test
    void shouldEncodeKnownValues() {
        assertThat(Base62Encoder.encode(0)).isEqualTo("0");
        assertThat(Base62Encoder.encode(61)).isEqualTo("z");
        assertThat(Base62Encoder.encode(62)).isEqualTo("10");
        assertThat(Base62Encoder.encode(238328)).isEqualTo("1000");
    }
}
