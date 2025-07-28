import java.io.*;
import java.math.BigInteger;
import java.util.*;
import org.json.JSONObject;

public class SecretSharingSolver {

    public static void main(String[] args) throws Exception {
        // Update these paths as needed
        String[] files = {"testcase1.json", "testcase2.json"};
        
        for (String file : files) {
            JSONObject json = readJsonFile(file);
            BigInteger secret = solveSecret(json);
            System.out.println("Secret for " + file + ": " + secret);
        }
    }

    static JSONObject readJsonFile(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder jsonStr = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonStr.append(line);
        }
        reader.close();
        return new JSONObject(jsonStr.toString());
    }

    static BigInteger solveSecret(JSONObject json) {
        JSONObject keys = json.getJSONObject("keys");
        int n = keys.getInt("n");
        int k = keys.getInt("k");

        List<BigInteger> xVals = new ArrayList<>();
        List<BigInteger> yVals = new ArrayList<>();

        for (String key : json.keySet()) {
            if (key.equals("keys")) continue;

            JSONObject point = json.getJSONObject(key);
            int base = Integer.parseInt(point.getString("base"));
            String valueStr = point.getString("value");
            BigInteger x = new BigInteger(key);
            BigInteger y = new BigInteger(valueStr, base);

            xVals.add(x);
            yVals.add(y);
        }

        // Pick any k combinations of points
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < xVals.size(); i++) indices.add(i);

        BigInteger correctSecret = null;
        Map<BigInteger, Integer> frequencyMap = new HashMap<>();

        combinations(indices, k, combo -> {
            List<BigInteger> xSubset = new ArrayList<>();
            List<BigInteger> ySubset = new ArrayList<>();
            for (int idx : combo) {
                xSubset.add(xVals.get(idx));
                ySubset.add(yVals.get(idx));
            }

            BigInteger secret = lagrangeInterpolationAtZero(xSubset, ySubset);
            frequencyMap.put(secret, frequencyMap.getOrDefault(secret, 0) + 1);
        });

        // Choose the most frequent result (in case of faulty values in future versions)
        int maxFreq = 0;
        for (Map.Entry<BigInteger, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                correctSecret = entry.getKey();
            }
        }

        return correctSecret;
    }

    static BigInteger lagrangeInterpolationAtZero(List<BigInteger> x, List<BigInteger> y) {
        BigInteger result = BigInteger.ZERO;
        int k = x.size();

        for (int i = 0; i < k; i++) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                numerator = numerator.multiply(x.get(j).negate());
                denominator = denominator.multiply(x.get(i).subtract(x.get(j)));
            }

            BigInteger term = y.get(i).multiply(numerator).divide(denominator);
            result = result.add(term);
        }

        return result;
    }

    // Functional interface for combination callback
    interface CombinationCallback {
        void process(List<Integer> combination);
    }

    static void combinations(List<Integer> elements, int k, CombinationCallback callback) {
        generateCombos(0, new ArrayList<>(), elements, k, callback);
    }

    static void generateCombos(int index, List<Integer> current, List<Integer> elements, int k, CombinationCallback callback) {
        if (current.size() == k) {
            callback.process(new ArrayList<>(current));
            return;
        }
        if (index >= elements.size()) return;

        current.add(elements.get(index));
        generateCombos(index + 1, current, elements, k, callback);

        current.remove(current.size() - 1);
        generateCombos(index + 1, current, elements, k, callback);
    }
}
