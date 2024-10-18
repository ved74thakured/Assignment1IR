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
        // Setting up the path
        String indexPath = "index";
        String cranfieldPath = "/opt/lucene_project/cran.all.1400";

        // Setting  up analyzer
        StandardAnalyzer analyzer = new StandardAnalyzer();
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, iwc);

        // Reading  and indexing Cranfielcollection
        File cranfieldFile = new File(cranfieldPath);
        BufferedReader reader = new BufferedReader(new FileReader(cranfieldFile));
        String line;
        StringBuilder docContent = new StringBuilder();
        int docId = 0;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(".I")) {  
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
    }

   
    static void indexDoc(IndexWriter writer, String content, int docId) throws Exception {
        Document doc = new Document();
        doc.add(new TextField("docId", String.valueOf(docId), TextField.Store.YES));
        doc.add(new TextField("content", content, TextField.Store.YES));
        writer.addDocument(doc);
    }
}
