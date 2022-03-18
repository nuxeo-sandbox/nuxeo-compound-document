package org.nuxeo.labs.compound.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.compound.api.CompoundArchive;

import java.io.IOException;

public interface CompoundDocumentService {

    /**
     *
     * @param doc
     * @return if true if doc is a compound document
     */
    boolean isCompoundDocument(DocumentModel doc);

    /**
     * Determine what compound document type the archive can be turned into the parent document
     * @param parent
     * @param archive
     * @return a compound document type if one can be created in parent from the archive
     */
    String getTargetCompoundDocumentTypeFromContext(DocumentModel parent, CompoundArchive archive);

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
     * Update a document structure from the archive blob in the input document
     * @param compound
     * @param archiveBlob
     * @throws IOException
     */
    void updateStructureFromArchive(DocumentModel compound,  Blob archiveBlob) throws IOException;

    /**
     * Update a document structure from the archive blob in the input document
     * @param compound
     * @param archive
     * @throws IOException
     */
    void updateStructureFromArchive(DocumentModel compound,  CompoundArchive archive) throws IOException;

    /**
     *
     * @param archiveBlob
     * @return true if the archive file is supported by this service
     */
    boolean isSupportedArchiveFile(Blob archiveBlob);

    /**
     *
     * @param document a compound document
     * @return a zip archive containing the compound document file structure
     */
    Blob toZip(DocumentModel document) throws IOException;

    /**
     *
     * @param document a compound document
     * @param pageproviderName the name of the page provider to use to fetch the components to export
     * @return a zip archive containing the compound document file structure
     */
    Blob toZip(DocumentModel document, String pageproviderName) throws IOException;

}
