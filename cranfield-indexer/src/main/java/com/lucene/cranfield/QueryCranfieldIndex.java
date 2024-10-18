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
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class QueryCranfieldIndex {

    public static void main(String[] args) throws Exception {
        // Setting up path 
        String indexPath = "index";
        String queryPath = "/opt/lucene_project/cran.qry";
        String outputPathBM25 = "/opt/lucene_project/query_results_bm25.txt";
        String outputPathTFIDF = "/opt/lucene_project/query_results_tfidf.txt";
        String outputPathLMDirichlet = "/opt/lucene_project/query_results_lmdirichlet.txt";

        // Initializing  the analyzer and searcher
        Analyzer analyzer = new StandardAnalyzer();
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        // Initializing  similarity models
        BM25Similarity bm25Similarity = new BM25Similarity();
        ClassicSimilarity tfidfSimilarity = new ClassicSimilarity(); // Vector Space Model
        LMDirichletSimilarity lmDirichletSimilarity = new LMDirichletSimilarity();

        QueryParser parser = new QueryParser("content", analyzer);

        File queryFile = new File(queryPath);
        BufferedReader queryReader = new BufferedReader(new FileReader(queryFile));

        
        try (FileWriter writerBM25 = new FileWriter(outputPathBM25, false);
             FileWriter writerTFIDF = new FileWriter(outputPathTFIDF, false);
             FileWriter writerLMDirichlet = new FileWriter(outputPathLMDirichlet, false)) {

            String line;
            int queryId = 1;
            StringBuilder queryContent = new StringBuilder();

           
            while ((line = queryReader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (queryContent.length() > 0) {
                        
                        searcher.setSimilarity(bm25Similarity);
                        processQuery(queryId, queryContent.toString(), searcher, parser, writerBM25, "BM25");

                        
                        searcher.setSimilarity(tfidfSimilarity);
                        processQuery(queryId, queryContent.toString(), searcher, parser, writerTFIDF, "TFIDF");

                        
                        searcher.setSimilarity(lmDirichletSimilarity);
                        processQuery(queryId, queryContent.toString(), searcher, parser, writerLMDirichlet, "LMDirichlet");

                        queryContent.setLength(0);
                        queryId++;
                    }
                } else if (line.startsWith(".W")) {
                    queryContent.append("");
                } else {
                    queryContent.append(line).append(" ");
                }
            }

            
            if (queryContent.length() > 0) {
                
                searcher.setSimilarity(bm25Similarity);
                processQuery(queryId, queryContent.toString(), searcher, parser, writerBM25, "BM25");

                
                searcher.setSimilarity(tfidfSimilarity);
                processQuery(queryId, queryContent.toString(), searcher, parser, writerTFIDF, "TFIDF");

               
                searcher.setSimilarity(lmDirichletSimilarity);
                processQuery(queryId, queryContent.toString(), searcher, parser, writerLMDirichlet, "LMDirichlet");
            }

            queryReader.close();
        }
        reader.close();
    }

    private static void processQuery(int queryId, String queryText, IndexSearcher searcher, QueryParser parser, FileWriter writer, String similarityModel) throws Exception {
        try {
            String cleanedQuery = cleanQueryText(queryText);
            Query query = parser.parse(cleanedQuery);
            TopDocs results = searcher.search(query, 1400);

            
            Set<String> writtenDocs = new HashSet<>();

            int rank = 1;
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                if (rank > 50) {
                    break;
                }

                Document doc = searcher.doc(scoreDoc.doc);
                String docId = doc.get("docId");

                
                if (writtenDocs.contains(docId)) {
                    continue;
                }

                float score = scoreDoc.score;

                
                writer.write(queryId + " " + similarityModel + " " + docId + " " + rank + " " + score + " STANDARD\n");
                rank++;

                
                writtenDocs.add(docId);
            }

            System.out.println("Query ID " + queryId + " (" + similarityModel + "): Top " + (rank - 1) + " results recorded.");
        } catch (ParseException e) {
            System.err.println("Error parsing query: " + queryText + " - " + e.getMessage());
        }
    }

    private static String cleanQueryText(String queryText) {
        return queryText.replaceAll("[*?]", " ").trim();
    }
}
