package qatch.calibration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility class responsible for deriving the weights (TQI-Characteristics layer and Characteristics-Properties layer)
 * described by hand-entered comparison matrices as part of the analytical hierarchy process.
 *
 * It should ensure the hand-entered matrices match TQI, characteristic, and properties nodes
 * described in the quality model and the data format in the matrices is valid.
 *
 * The class should provide functionality to return a mapping of weights over the two layers given
 * a quality model description and comparison matrices as input by running R scripts.
 */
public class Weighter {

    // Methods

    public static Set<WeightResult> elicitateWeights(Path comparisonMatricesDirectory, Path tempResultsDirectory) {

        // Precondition checks
        if (!comparisonMatricesDirectory.toFile().isDirectory()) {
            throw new RuntimeException("Parameter comparisonMatricesDirectory must be a directory");
        }
        if (Objects.requireNonNull(comparisonMatricesDirectory.toFile().listFiles()).length < 1) {
            throw new RuntimeException("At least one file must exist in comparisonMatricesDirectory");
        }

        // Create directory for temporary generated file results if not yet exists
        tempResultsDirectory.toFile().mkdirs();

        // Run R script
        RInvoker.executeRScript(RInvoker.Script.AHP, comparisonMatricesDirectory, tempResultsDirectory);

        // Parse node name ordering for each matrix
        // TODO: eventually easiest to just have R map the names to the weights instead of parsing the files again
        Map<String, ArrayList<String>> weightNameOrders = parseNameOrder(comparisonMatricesDirectory);

        // Transform into WeightResults objects
        Set<WeightResult> weightResults = new HashSet<>();
        File weightsJson = new File(tempResultsDirectory.toFile(), "weights.json");

        try {
            FileReader fr = new FileReader(weightsJson);
            JsonObject jsonObject = new JsonParser().parse(fr).getAsJsonObject();
            jsonObject.keySet().forEach(nodeName -> {

                if (!weightNameOrders.containsKey(nodeName)) {
                    throw new RuntimeException("parseNameOrder failed to create ordering for node name " + nodeName);
                }

                JsonArray nodeWeights = jsonObject.getAsJsonArray(nodeName);
                ArrayList<String> nameList = weightNameOrders.get(nodeName);

                if (nodeWeights.size() != nameList.size()) {
                    throw new RuntimeException("nodeWeights and nameList arrays do not match lenghts");
                }

                WeightResult weightResult = new WeightResult(nodeName);
                // zip-like function would be great here if Java eventually supports a one-line version of it
                for (int i = 0; i < nodeWeights.size(); i++) {
                    weightResult.weights.put(nameList.get(i), nodeWeights.get(i).getAsDouble());
                }

                weightResults.add(weightResult);
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return weightResults;
    }


    /**
     * Extract the left-to-right order of the comparison matrix characteristic or property names.
     * Ordering is necessary because the weights.json output from the R script generates weights in order according
     * to the left-to-right comparison matrix it receives as input.
     * Eventually this method should be depreicated by modifying the R-script to automatically attach the names
     * to its output.
     *
     * @param comparisonMatricesDirectory
     *      Path to the directory holding the comparison matrix files to extract name order from.
     *      E.g. "src/comparison_matrices/"
     * @return
     *      Mapping of {
     *          Key: node name of weights receiver,
     *          Value: left-to-right ordered list of characteristic or property names under comparison
     *      }
     */
    static Map<String, ArrayList<String>> parseNameOrder(Path comparisonMatricesDirectory) {

        Map<String, ArrayList<String>> orderedWeightNames = new HashMap<>();
        for (final File matrixFile : Objects.requireNonNull(comparisonMatricesDirectory.toFile().listFiles())) {

            try {
                FileReader fr = new FileReader(matrixFile);
                CSVReader reader = new CSVReader(fr);
                String[] header = reader.readNext();

                String nodeName = header[0];
                ArrayList<String> weightNameOrder = new ArrayList<>(Arrays.asList(header).subList(1, header.length));

                orderedWeightNames.put(nodeName, weightNameOrder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return orderedWeightNames;
    }
}
