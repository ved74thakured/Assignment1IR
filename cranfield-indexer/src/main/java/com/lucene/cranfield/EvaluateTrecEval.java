package com.lucene.cranfield;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

public class EvaluateTrecEval {
    public static void main(String[] args) {
        // Paths to necessary files
        String trecEvalPath = "/opt/lucene_project/cranfield-indexer/trec_eval-9.0.7/trec_eval";  // Path to the trec_eval executable
        String relevanceJudgmentsPath = "/opt/lucene_project/cranqrel_fixed";  // Path to the relevance judgments file (corrected cranqrel)
        String resultsFilePath = "/opt/lucene_project/query_results.txt";  // Path to the system's output (query_results.txt)

        try {
            // Command to run trec_eval and evaluate the system
            String command = trecEvalPath + " " + relevanceJudgmentsPath + " " + resultsFilePath;

            // Log the command being executed
            System.out.println("Executing command: " + command);

            // Execute the command and capture the output
            Process process = Runtime.getRuntime().exec(command);

            // Capture the standard output (from the trec_eval command)
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            System.out.println("TREC Eval Output:");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);  // Print each line of the TREC Eval output
            }

            // Capture the error stream (in case there are errors)
            InputStream errorStream = process.getErrorStream();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
            String errorLine;
            boolean hasError = false;

            System.out.println("TREC Eval Error Output (if any):");
            while ((errorLine = errorReader.readLine()) != null) {
                System.out.println(errorLine);  // Print each error line
                hasError = true;
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0 && !hasError) {
                System.out.println("Evaluation completed successfully.");
            } else {
                System.out.println("Error occurred during evaluation. Exit code: " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
