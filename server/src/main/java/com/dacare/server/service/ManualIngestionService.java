package com.dacare.server.service;

import com.dacare.server.domain.ManualImport;
import com.dacare.server.repository.ManualImportRepository;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;

@Service
public class ManualIngestionService {
    private static final String SOURCE = "manual/device-troubleshooting.pdf";
    private final ManualImportRepository imports;
    public ManualIngestionService(ManualImportRepository imports) { this.imports = imports; }
    @Transactional
    public void ingest(VectorStore vectorStore) {
        if (imports.existsBySource(SOURCE)) return;
        var reader = new PagePdfDocumentReader(new ClassPathResource(SOURCE));
        vectorStore.write(new TokenTextSplitter().split(reader.read()));
        imports.save(new ManualImport(SOURCE));
    }
}
