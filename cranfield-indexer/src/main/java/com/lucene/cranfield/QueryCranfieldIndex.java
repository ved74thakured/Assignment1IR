package com.lucene.cranfield;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;  // For Vector Space Model (TFIDF)
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;

public class QueryCranfieldIndex {

    public static void main(String[] args) throws Exception {
        // setting up path to the index and query file
        String indexPath = "index";
        String queryPath = "/opt/lucene_project/cran.qry";
        String outputPathBM25 = "/opt/lucene_project/query_results_bm25.txt";
        String outputPathTFIDF = "/opt/lucene_project/query_results_tfidf.txt";

        // Initializing the analyzer and searcher
        Analyzer analyzer = new StandardAnalyzer();
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        // Initialize both similarity models: BM25 and ClassicSimilarity (TFIDF)
        BM25Similarity bm25Similarity = new BM25Similarity();
        ClassicSimilarity tfidfSimilarity = new ClassicSimilarity(); // TFIDF similarity (Vector Space Model)

        // Query parser (analyze query content similarly to how documents were indexed)
        QueryParser parser = new QueryParser("content", analyzer);

        // Open the cran.qry file to read the queries
        File queryFile = new File(queryPath);
        BufferedReader queryReader = new BufferedReader(new FileReader(queryFile));

        // Open separate output files for storing results
        FileWriter writerBM25 = new FileWriter(outputPathBM25, false);  // For BM25 results
        FileWriter writerTFIDF = new FileWriter(outputPathTFIDF, false);  // For TFIDF results

        String line;
        int queryId = 1;
        StringBuilder queryContent = new StringBuilder();  // For holding the actual query text

        // Process the query file
        while ((line = queryReader.readLine()) != null) {
            if (line.startsWith(".I")) {
                // New query starts; flush the previous query (if any) and run it
                if (queryContent.length() > 0) {
                    // Run the query with BM25 similarity
                    searcher.setSimilarity(bm25Similarity);
                    processQuery(queryId, queryContent.toString(), searcher, parser, writerBM25, "BM25");

                    // Run the query with TFIDF similarity
                    searcher.setSimilarity(tfidfSimilarity);
                    processQuery(queryId, queryContent.toString(), searcher, parser, writerTFIDF, "TFIDF");

                    queryContent.setLength(0);  // Clear for the next query
                    queryId++;
                }
            } else if (line.startsWith(".W")) {
                // This line indicates the start of the query text
                queryContent.append("");  // Ignore .W
            } else {
                // Add the actual query text
                queryContent.append(line).append(" ");
            }
        }

        // Process the last query
        if (queryContent.length() > 0) {
            // Run the query with BM25 similarity
            searcher.setSimilarity(bm25Similarity);
            processQuery(queryId, queryContent.toString(), searcher, parser, writerBM25, "BM25");

            // Run the query with TFIDF similarity
            searcher.setSimilarity(tfidfSimilarity);
            processQuery(queryId, queryContent.toString(), searcher, parser, writerTFIDF, "TFIDF");
        }

        // Close all resources
        queryReader.close();
        writerBM25.close();
        writerTFIDF.close();
        reader.close();
    }

    // Helper function to process each query, clean it, run it, and write the top 50 results
    private static void processQuery(int queryId, String queryText, IndexSearcher searcher, QueryParser parser, FileWriter writer, String similarityModel) throws Exception {
        try {
            // Clean the query by removing special characters not allowed in Lucene queries
            String cleanedQuery = cleanQueryText(queryText);
            Query query = parser.parse(cleanedQuery);  // Parse the cleaned query text
            TopDocs results = searcher.search(query, 1400);  // Search over all 1400 documents

            // Write top 50 results for each query and similarity model
            int rank = 1;  // Ranking starts from 1
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                if (rank > 50) {
                    break;  // Only consider the top 50 documents
                }

                Document doc = searcher.doc(scoreDoc.doc);
                String docId = doc.get("docId");  // Retrieve the document ID
                float score = scoreDoc.score;  // Retrieve the similarity score

                // Write the results in TREC Eval format: query_id Q0 doc_id rank score STANDARD
                writer.write(queryId + " " + similarityModel + " " + docId + " " + rank + " " + score + " STANDARD\n");
                rank++;
            }

            System.out.println("Query ID " + queryId + " (" + similarityModel + "): Top " + (rank - 1) + " results recorded.");
        } catch (ParseException e) {
            System.err.println("Error parsing query: " + queryText + " - " + e.getMessage());
        }
    }

    // Method to clean the query text by removing or escaping invalid characters for Lucene
    private static String cleanQueryText(String queryText) {
        // Remove or escape special characters (*, ?, etc.) not allowed in Lucene queries
        return queryText.replaceAll("[*?]", " ").trim();
    }
}
