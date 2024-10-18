package com.lucene.cranfield;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.CharArraySet;
import java.util.Arrays;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) throws Exception {
        String indexPath = "index";
        String cranfieldPath = "/opt/lucene_project/cran.all.1400";
        String queryPath = "/opt/lucene_project/cran.qry";

        // Choose the analyzer here
        Analyzer analyzer = chooseAnalyzer("standard");  // Use "stop", "whitespace", or "standard"

        // Setup IndexWriter
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, iwc);

        // Reading and indexing the Cranfield dataset
        File cranfieldFile = new File(cranfieldPath);
        BufferedReader reader = new BufferedReader(new FileReader(cranfieldFile));
        String line;
        StringBuilder docContent = new StringBuilder();
        int docId = 0;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(".I")) {  // Document delimiter
                if (docContent.length() > 0) {
                    indexDoc(writer, docContent.toString(), docId);
                    docContent.setLength(0);
                }
                docId++;
            } else {
                docContent.append(line).append("\n");
            }
        }
        if (docContent.length() > 0) {
            indexDoc(writer, docContent.toString(), docId);
        }
        reader.close();
        writer.close();

        // Search the index
        DirectoryReader indexReader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        QueryParser parser = new QueryParser("content", analyzer);
        Query query = parser.parse("aerodynamics");  // Example query

        TopDocs results = searcher.search(query, 10);
        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println("Document ID: " + doc.get("docId") + ", Content: " + doc.get("content"));
        }
        indexReader.close();
    }

    // Helper method to index a document
    static void indexDoc(IndexWriter writer, String content, int docId) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("docId", String.valueOf(docId), Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        writer.addDocument(doc);
    }

    // Helper method to choose an analyzer based on input string
    static Analyzer chooseAnalyzer(String analyzerType) {
   	 switch (analyzerType.toLowerCase()) {
        	case "stop":
            	// Define a custom set of stop words if ENGLISH_STOP_WORDS_SET is not available
           		 CharArraySet stopWords = new CharArraySet(Arrays.asList(
               		 "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", 
	                 "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", 
	                "that", "the", "their", "then", "there", "these", "they", "this", 
	                "to", "was", "will", "with"), true);
        	    return new StopAnalyzer(stopWords);
       		 case "whitespace":
           	    return new WhitespaceAnalyzer();
		 case "standard":
	        default:
        	    return new StandardAnalyzer();
    }
}
}
