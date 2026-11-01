package sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Phase 1: the factory reads the paper's Arena notation and refuses bad text with a readable message. */
class DistributionFactoryTest {

    @Test
    void mannedCheckInFormulaBecomesShiftedWithMean114() {
        Distribution d = DistributionFactory.parse("54 + EXPO(60)");
        assertInstanceOf(Shifted.class, d);
        assertEquals(114.0, d.mean(), 1e-9);
    }

    @Test
    void arrivalFormulaWithNegativeOffsetIsRead() {
        Distribution d = DistributionFactory.parse("-0.001 + GAMM(39.2, 1)");
        assertInstanceOf(Shifted.class, d);
        assertEquals(39.199, d.mean(), 1e-9);
    }

    @Test
    void weibullFormulaIsRead() {
        Distribution d = DistributionFactory.parse("60 + WEIB(40, 1.5)");
        assertEquals(60 + 36.10981171803735, d.mean(), 1e-9);
    }

    @Test
    void noOffsetOrZeroOffsetGivesThePlainDistribution() {
        assertInstanceOf(ExponentialDist.class, DistributionFactory.parse("EXPO(120)"));
        assertInstanceOf(ExponentialDist.class, DistributionFactory.parse("0 + EXPO(120)"));
    }

    @Test
    void extraSpacesAreAllowed() {
        Distribution d = DistributionFactory.parse("  GAMM( 5 , 3 )  ");
        assertInstanceOf(GammaDist.class, d);
        assertEquals(15.0, d.mean(), 1e-9);
    }

    /** GAMM(5, 3) and GAMM(3, 5) have the same mean, so compare samples to be sure the order is scale, shape. */
    @Test
    void parameterOrderIsScaleThenShape() {
        Distribution parsed = DistributionFactory.parse("GAMM(5, 3)");
        Distribution direct = new GammaDist(5, 3);
        Random a = new Random(3);
        Random b = new Random(3);
        for (int i = 0; i < 100; i++) assertEquals(direct.sample(a), parsed.sample(b));
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @CsvSource({
            "'WEIB(30)',      expected 2",
            "'GAMM(1)',       expected 2",
            "'EXPO(1, 2)',    expected 1",
            "'FOO(1)',        unknown distribution FOO",
            "'',              cannot read",
            "'EXPO(60',       cannot read",
            "'54 + EXPO(x)',  is not a number",
            "'EXPO(0)',       greater than 0",
            "'GAMM(0, 0)',    greater than 0"
    })
    void badTextIsRefusedWithAClearMessage(String text, String expectedInMessage) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DistributionFactory.parse(text));
        assertTrue(e.getMessage().contains(expectedInMessage), e.getMessage());
    }

    @Test
    void missingTextIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DistributionFactory.parse(null));
        assertTrue(e.getMessage().contains("no distribution"), e.getMessage());
    }
}
