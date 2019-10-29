package qatch.analysis;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class MeasureTests {

    private Finding finding01, finding02, finding03;
    private Diagnostic diagnostic01, diagnostic02;
    private Measure measure01;

    @Before
    public void initialize() {
        finding01 = new Finding("A", 1, 1, 1);
        finding02 = new Finding("B", 2, 2, 2);
        finding03 = new Finding("C", 3, 3, 3);
        diagnostic01 = new Diagnostic("Diag01");
        diagnostic02 = new Diagnostic("Diag02");

        diagnostic01.setFinding(finding01);
        diagnostic02.setFinding(finding02);
        diagnostic02.setFinding(finding03);
    }

    @Test
    public void testEvaluate_defaultFunction() {
        Measure m = new Measure();
        m.setDiagnostics(Arrays.asList(diagnostic01, diagnostic02));
        double v = m.getValue();

        Assert.assertEquals(3, v, 0);

    }

    @Test
    public void testEvaluate_customFunction() {
        Function<List<Diagnostic>, Double> customFunct = (List<Diagnostic> diagnostics) -> {
            double value = 0.0;
            for (Diagnostic d : diagnostics) {
                for (Finding f : d.getFindings()) {
                    value += f.getSeverity();
                }
            }
            return value;
        };

        Measure m = new Measure(null, null, null, customFunct);
        m.setDiagnostics(Arrays.asList(diagnostic01, diagnostic02));

        double v = m.getValue();
        Assert.assertEquals(6, v, 0);
    }

}