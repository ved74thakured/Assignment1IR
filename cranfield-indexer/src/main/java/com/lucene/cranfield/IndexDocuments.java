package com.lucene.cranfield;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;

public class IndexDocuments {
    public static void main(String[] args) throws Exception {
        // Path to index and Cranfield collection
        String indexPath = "index";
        String cranfieldPath = "/opt/lucene_project/cran.all.1400";

        // Set up analyzer and writer config
        StandardAnalyzer analyzer = new StandardAnalyzer();
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, iwc);

        // Read and index Cranfield collection
        File cranfieldFile = new File(cranfieldPath);
        BufferedReader reader = new BufferedReader(new FileReader(cranfieldFile));
        String line;
        StringBuilder docContent = new StringBuilder();
        int docId = 0;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(".I")) {  // Document delimiter
                if (docContent.length() > 0) {
                    indexDoc(writer, docContent.toString(), docId);
                    docContent.setLength(0);  // Clear for next document
                }
                docId++;
            } else {
                docContent.append(line).append("\n");
            }
        }
        
        // Index the last document
        if (docContent.length() > 0) {
            indexDoc(writer, docContent.toString(), docId);
        }
        reader.close();
        writer.close();
    }

    // Helper method to index a document
    static void indexDoc(IndexWriter writer, String content, int docId) throws Exception {
        Document doc = new Document();
        doc.add(new TextField("docId", String.valueOf(docId), TextField.Store.YES));
        doc.add(new TextField("content", content, TextField.Store.YES));
        writer.addDocument(doc);
    }
}
