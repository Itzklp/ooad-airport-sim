package sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DistributionFactoryTest {

    @Test
    void mannedCheckInFormulaBecomesShiftedWithMean114() {
        Distribution manned = DistributionFactory.parse("54 + EXPO(60)");
        assertInstanceOf(Shifted.class, manned);
        assertEquals(114.0, manned.mean(), 1e-9);
    }

    @Test
    void arrivalFormulaWithNegativeOffsetIsRead() {
        Distribution arrival = DistributionFactory.parse("-0.001 + GAMM(39.2, 1)");
        assertInstanceOf(Shifted.class, arrival);
        assertEquals(39.199, arrival.mean(), 1e-9);
    }

    @Test
    void weibullFormulaIsRead() {
        Distribution selfCheckIn = DistributionFactory.parse("60 + WEIB(40, 1.5)");
        assertEquals(60 + 36.10981171803735, selfCheckIn.mean(), 1e-9);
    }

    @Test
    void noOffsetOrZeroOffsetGivesThePlainDistribution() {
        assertInstanceOf(ExponentialDist.class, DistributionFactory.parse("EXPO(120)"));
        assertInstanceOf(ExponentialDist.class, DistributionFactory.parse("0 + EXPO(120)"));
    }

    @Test
    void extraSpacesAreAllowed() {
        Distribution gamma = DistributionFactory.parse("  GAMM( 5 , 3 )  ");
        assertInstanceOf(GammaDist.class, gamma);
        assertEquals(15.0, gamma.mean(), 1e-9);
    }

    // GAMM(5, 3) and GAMM(3, 5) have the same mean, so samples are compared to check the order is scale, shape
    @Test
    void parameterOrderIsScaleThenShape() {
        Distribution parsed = DistributionFactory.parse("GAMM(5, 3)");
        Distribution direct = new GammaDist(5, 3);
        Random first = new Random(3);
        Random second = new Random(3);
        for (int i = 0; i < 100; i++) assertEquals(direct.sample(first), parsed.sample(second));
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
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, () -> DistributionFactory.parse(text));
        assertTrue(refusal.getMessage().contains(expectedInMessage), refusal.getMessage());
    }

    @Test
    void missingTextIsRefused() {
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, () -> DistributionFactory.parse(null));
        assertTrue(refusal.getMessage().contains("no distribution"), refusal.getMessage());
    }
}
