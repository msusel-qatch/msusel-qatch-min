package pique.calibration;

import pique.analysis.ITool;
import pique.evaluation.Project;
import pique.model.Diagnostic;
import pique.model.Measure;
import pique.model.QualityModel;
import pique.utility.FileUtility;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;

public class NaiveBenchmarker implements IBenchmarker {

    /**
     * Derive thesholds for all {@link Measure} nodes using a naive approach:
     * (1) threshold[0] = the lowest value seen for the {@link Measure}
     * (2) threshold[1] = the highest value seen for the {@link Measure}
     *
     * @param benchmarkRepository The root directory containing the items to be used for benchmarking
     * @param qmDescription       The quality model description file
     * @param tools               The collection of static analysis tools needed to audio the benchmark repository
     * @param projectRootFlag     Option flag to target the static analysis tools
     * @param analysisResults     Location to place benchmarking analysis results
     * @return A dictionary of [ Key: {@link pique.model.ModelNode} name, Value: thresholds ] where
     * thresholds is a size = 2 array of Double[] containing the lowest and highest value
     * seen for the given measure (after normalization).
     */
    @Override
    public Map<String, Double[]> deriveThresholds(Path benchmarkRepository, QualityModel qmDescription, Set<ITool> tools,
                                                  String projectRootFlag) {

        // Collect root paths of each benchmark project
        Set<Path> projectRoots = FileUtility.multiProjectCollector(benchmarkRepository, projectRootFlag);
        ArrayList<Project> projects = new ArrayList<>();

        System.out.println("* Beginning repository benchmark analysis");
        System.out.println(projectRoots.size() + " projects to analyze.\n");

        int totalProjects = projectRoots.size();
        int counter = 0;

        for (Path projectPath : projectRoots) {

            counter++;

            // Instantiate new project object
            Project project = new Project(projectPath.getFileName().toString(), projectPath, qmDescription);

            // Run the static analysis tools process
            Map<String, Diagnostic> allDiagnostics = new HashMap<>();
            tools.forEach(tool -> {
                Path analysisOutput = tool.analyze(projectPath);
                allDiagnostics.putAll(tool.parseAnalysis(analysisOutput));
            });

            // Run LOC tool to set lines of code
            project.setLinesOfCode((int) allDiagnostics.get("loc").getValue());

            // Apply collected diagnostics (containing findings) to the project
            allDiagnostics.forEach((diagnosticName, diagnostic) -> {
                project.addFindings(diagnostic);
            });

            // Evaluate project up to Measure level (normalize does happen first)
            project.evaluateMeasures();

            // Add new project (with tool findings information included) to the list
            projects.add(project);

            // Print information
            System.out.println("\n\tFinished analyzing project " + project.getName());
            System.out.println("\t" + counter + " of " + totalProjects + " analyzed.\n");
        }

        // Map all values audited for each measure
        Map<String, ArrayList<Double>> measureBenchmarkData = new HashMap<>();
        projects.forEach(p -> {
            p.getQualityModel().getMeasures().values().forEach(m -> {
                        if (!measureBenchmarkData.containsKey(m.getName())) {
                            measureBenchmarkData.put(m.getName(), new ArrayList<Double>() {{
                                add(m.getValue());
                            }});
                        } else {
                            measureBenchmarkData.get(m.getName()).add(m.getValue());
                        }
                    }
            );
        });

        // Identify the lowest and highest of each measure value
        Map<String, Double[]> measureThresholds = new HashMap<>();
        measureBenchmarkData.forEach((measureName, measureValues) -> {
            measureThresholds.putIfAbsent(measureName, new Double[2]);
            measureThresholds.get(measureName)[0] = Collections.min(measureValues);
            measureThresholds.get(measureName)[1] = Collections.max(measureValues);

        });

        return measureThresholds;
    }

    @Override
    public String getName() {
        return "NaiveBenchmarker";
    }
}