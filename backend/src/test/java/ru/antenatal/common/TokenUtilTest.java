package ru.antenatal.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUtilTest {

    @Test
    void hashesSha256() {
        assertThat(TokenUtil.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void publicCodeIsEightUnambiguousCharacters() {
        String code = TokenUtil.publicCode();
        assertThat(code).hasSize(8);
        assertThat(code).matches("[2-9A-HJ-NP-Z]{8}");
    }

    @Test
    void tokensAreRandom() {
        String first = TokenUtil.randomToken(16);
        String second = TokenUtil.randomToken(16);
        assertThat(first).isNotEqualTo(second);
        assertThat(first.length()).isGreaterThan(20);
    }
}
