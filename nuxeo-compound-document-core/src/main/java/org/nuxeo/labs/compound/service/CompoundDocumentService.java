package org.nuxeo.labs.compound.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.io.IOException;
import java.util.List;

public interface CompoundDocumentService {

    /**
     *
     * @param doc
     * @return if true if doc is a compound document
     */
    boolean isCompoundDocument(DocumentModel doc);

    /**
     * Determine what compound document type the archive blob can be turned into the parent document
     * @param parent
     * @param archiveBlob
     * @return a compound document type if one can be created in parent from the archive
     */
    String getTargetCompoundDocumentTypeFromContext(DocumentModel parent, Blob archiveBlob);

    /**
     * Create a compound document in parent
     * @param parent
     * @param archiveBlob
     * @param documentType
     * @return the newly created compound document
     * @throws IOException
     */
    DocumentModel createCompoundFromArchive(DocumentModel parent, Blob archiveBlob, String documentType) throws IOException;

    /**
     * Create a compound document in parent
     * @param parent
     * @param archiveBlob
     * @return the newly created compound document
     * @throws IOException
     */
    DocumentModel createCompoundFromArchive(DocumentModel parent, Blob archiveBlob) throws IOException;

    /**
     * Create a document structure from the archive blob in the input document
     * @param compound
     * @param archiveBlob
     * @throws IOException
     */
    void createStructureFromArchive(DocumentModel compound, Blob archiveBlob) throws IOException;

    /**
     *
     * @param archiveBlob
     * @param outputEntryList an optional parameter to store the list of entries from teh archive blob
     * @return true if the archive file is supported by this service
     */
    boolean isSupportedArchiveFile(Blob archiveBlob, List<String> outputEntryList);

}
