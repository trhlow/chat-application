package com.chatrealtime.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppCorsPropertiesTest {

    @Test
    void originList_splitsCommaSeparatedOriginsAndTrims() {
        AppCorsProperties props = new AppCorsProperties();
        props.setAllowedOrigins(" https://a.example ,https://b.example ");
        assertThat(props.originList()).containsExactly("https://a.example", "https://b.example");
    }

    @Test
    void originList_blankString_returnsEmptyList() {
        AppCorsProperties props = new AppCorsProperties();
        props.setAllowedOrigins("   ");
        assertThat(props.originList()).isEmpty();
    }
}
