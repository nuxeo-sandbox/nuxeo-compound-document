package org.nuxeo.labs.compound.service;

import static org.nuxeo.ecm.core.io.ExportConstants.MARKER_FILE;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.labs.compound.api.CompoundArchive;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class CompoundDocumentServiceImpl extends DefaultComponent implements CompoundDocumentService {

    public static final String COMPOUND_ARCHIVE_IMPORTED = "compoundDocumentTreeImported";

    public static final String STRUCTURE_IMPORTED = "structureUpdated";

    protected static final Log log = LogFactory.getLog(CompoundDocumentServiceImpl.class);

    public static String TYPE_FILTER_KEY = "org.nuxeo.labs.compound.service.type.filter";

    public static String COMPOUND_TYPE_SCRIPT = "javascript.utils_get_compound_type";

    public static String SUB_FOLDER_TYPE_SCRIPT = "javascript.utils_get_compound_sub_folder_type";

    public static List<String> MARKER_BLACKLIST = Arrays.asList(MARKER_FILE, "meta-data.csv");

    @Override
    public boolean isCompoundDocument(DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        ActionContext actionContext = new ELActionContext();
        actionContext.setCurrentDocument(doc);
        return actionService.checkFilter(TYPE_FILTER_KEY, actionContext);
    }

    @Override
    public String getTargetCompoundDocumentTypeFromContext(DocumentModel parent, CompoundArchive archive) {
        AutomationService automationService = Framework.getService(AutomationService.class);
        OperationContext ctx = new OperationContext(parent.getCoreSession());
        Map<String, Object> params = new HashMap<>();
        ctx.setInput(parent);
        ctx.put("blob", archive.getBlob());
        ctx.put("entries", archive.getValidEntryList());
        try {
            return (String) automationService.run(ctx, COMPOUND_TYPE_SCRIPT, params);
        } catch (OperationException e) {
            throw new NuxeoException(e);
        }
    }

    public String getSubFolderTypeFromContext(DocumentModel compound, String parentPath, String name) {
        AutomationService automationService = Framework.getService(AutomationService.class);
        OperationContext ctx = new OperationContext(compound.getCoreSession());
        Map<String, Object> params = new HashMap<>();
        params.put("parentPath", parentPath);
        params.put("folderName", name);
        ctx.setInput(compound);
        try {
            return (String) automationService.run(ctx, SUB_FOLDER_TYPE_SCRIPT, params);
        } catch (OperationException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public DocumentModel createCompoundFromArchive(DocumentModel parent, Blob archiveBlob, String documentType)
            throws IOException {
        CoreSession session = parent.getCoreSession();
        String compoundName = FilenameUtils.removeExtension(archiveBlob.getFilename());
        DocumentModel compound = session.createDocumentModel(parent.getPathAsString(), compoundName, documentType);
        compound.setPropertyValue("dc:title", compoundName);
        compound = session.createDocument(compound);
        createStructureFromArchive(compound, archiveBlob);
        return compound;
    }

    @Override
    public DocumentModel createCompoundFromArchive(DocumentModel parent, Blob archiveBlob) throws IOException {
        CompoundArchive archive = toCompoundArchive(archiveBlob);
        String targetType = getTargetCompoundDocumentTypeFromContext(parent, archive);
        if (StringUtils.isNotEmpty(targetType)) {
            TypeManager typeService = Framework.getService(TypeManager.class);
            Type containerType = typeService.getType(parent.getType());
            if (containerType == null || !typeService.isAllowedSubType(targetType, parent.getType(), parent)) {
                return null;
            } else {
                return createCompoundFromArchive(parent, archiveBlob, targetType);
            }
        } else {
            return null;
        }
    }

    @Override
    public void createStructureFromArchive(DocumentModel compound, Blob archiveBlob) throws IOException {
        CompoundArchive archive = toCompoundArchive(archiveBlob);
        createStructureFromArchive(compound, archive);
    }

    public void createStructureFromArchive(DocumentModel compound, CompoundArchive archive) throws IOException {
        CoreSession session = compound.getCoreSession();
        FileManagerService fileManager = (FileManagerService) Framework.getService(FileManager.class);

        String prefix = archive.getPathPrefix();

        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(archive.getBlob().getStream()));
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            String entryName = entry.getName();

            log.debug("ZIP Entry: " + entry);

            // filter OS crap
            if (!isValidEntry(entryName)) {
                continue;
            }

            if (StringUtils.isNotEmpty(prefix)) {
                if (prefix.length() >= entryName.length()) {
                    continue;
                } else {
                    entryName = entryName.substring(prefix.length());
                }
            }

            Path entryPath = new Path(entryName);
            log.debug("Entry converted to Path: " + entryPath);

            String componentName = entryPath.lastSegment();
            log.debug("ComponentName: " + componentName);

            Path componentParentPath = entryPath.removeLastSegments(1);
            log.debug("Relative Component Parent Path: " + componentParentPath);

            Path nuxeoPath = compound.getPath();
            DocumentModel parent;
            if (!componentParentPath.isEmpty()) {
                nuxeoPath = nuxeoPath.append(componentParentPath);
                log.debug("Full Component Parent Path: " + nuxeoPath);
                parent = getOrCreateFolder(compound, nuxeoPath);
            } else {
                parent = compound;
            }

            if (entry.isDirectory()) {
                DocumentModel folder = fileManager.defaultCreateFolder(session, componentName, parent.getPathAsString(),
                        getSubFolderTypeFromContext(compound, parent.getPathAsString(), componentName), true, true);
                if (folder == null) {
                    throw new NuxeoException("Filemanager couldn't create the folder: " + nuxeoPath);
                }
            } else {
                Blob fileBlob = new FileBlob(new CloseShieldInputStream(zin));
                fileBlob.setFilename(componentName);
                FileImporterContext context = FileImporterContext.builder(session, fileBlob, parent.getPathAsString())
                                                                 .overwrite(true)
                                                                 .build();
                fileManager.createOrUpdateDocument(context);
            }
        }

        compound.putContextData(STRUCTURE_IMPORTED, true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), compound);
        Event event = ctx.newEvent(COMPOUND_ARCHIVE_IMPORTED);
        Framework.getService(EventService.class).fireEvent(event);
        session.saveDocument(compound);
    }

    public DocumentModel getOrCreateFolder(DocumentModel compound, Path folderPath) {
        CoreSession session = compound.getCoreSession();
        if (compound.getPath().equals(folderPath)) {
            return compound;
        } else if (!session.exists(new PathRef(folderPath.toString()))) {
            Path ancestorPath = folderPath.removeLastSegments(1);
            String folderName = folderPath.lastSegment();
            DocumentModel ancestorFolder = getOrCreateFolder(compound, ancestorPath);
            FileManagerService fileManager = (FileManagerService) Framework.getService(FileManager.class);
            return fileManager.defaultCreateFolder(session, folderName, ancestorFolder.getPathAsString(),
                    getSubFolderTypeFromContext(compound, ancestorFolder.getPathAsString(), folderName), true, true);
        } else {
            return session.getDocument(new PathRef(folderPath.toString()));
        }
    }

    @Override
    public void updateStructureFromArchive(DocumentModel compound, Blob archiveBlob) throws IOException {
        CompoundArchive archive = toCompoundArchive(archiveBlob);
        updateStructureFromArchive(compound, archive);
    }

    @Override
    public void updateStructureFromArchive(DocumentModel compound, CompoundArchive archive) throws IOException {
        CoreSession session = compound.getCoreSession();
        String query = String.format(
                "Select * FROM Document Where ecm:isVersion = 0 AND ecm:isProxy = 0 AND ecm:isTrashed = 0 AND ecm:ancestorId = '%s'",
                compound.getId());
        DocumentModelList children = session.query(query);

        List<DocumentModel> orphans = children.stream().filter(document -> {
            String relativePath = document.getPathAsString().substring(compound.getPathAsString().length() + 1);
            // add trailing / for folders
            relativePath = document.hasFacet(FOLDERISH) ? relativePath + '/' : relativePath;
            return !archive.getValidEntryList().contains(archive.getPathPrefix() + relativePath);
        }).collect(Collectors.toList());

        // delete orphans
        session.removeDocuments(orphans.stream().map(DocumentModel::getRef).toArray(DocumentRef[]::new));

        // update structure
        createStructureFromArchive(compound, archive);
    }

    @Override
    public boolean isSupportedArchiveFile(Blob archiveBlob) {
        return toCompoundArchive(archiveBlob) != null;
    }

    @Override
    public Blob toZip(DocumentModel compound) throws IOException {
        return toZip(compound, "compound_exportable_components");
    }

    @Override
    public Blob toZip(DocumentModel compound, String pageproviderName) throws IOException {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) compound.getCoreSession());
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                pageproviderName, null, null, null, null, props, new Object[] { compound.getId() });

        File zipFile = Framework.createTempFile("zip", null);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            do {
                List<DocumentModel> children = pp.getCurrentPage();
                for (DocumentModel current : children) {
                    if (current.isFolder()) {
                        continue;
                    }
                    Blob blob = current.getAdapter(BlobHolder.class).getBlob();
                    if (blob != null) {
                        String folderPath = current.getPath()
                                                   .removeLastSegments(1)
                                                   .toString()
                                                   .substring(compound.getPath().toString().length());
                        String entryPath = folderPath.length() > 0 ? folderPath + "/" + blob.getFilename()
                                : blob.getFilename();
                        if (entryPath.startsWith("/")) {
                            entryPath = entryPath.substring(1);
                        }
                        ZipEntry entry = new ZipEntry(entryPath);
                        zipOut.putNextEntry(entry);
                        try (InputStream in = blob.getStream()) {
                            IOUtils.copy(in, zipOut);
                        }
                    }
                }
                pp.nextPage();
            } while (pp.isNextEntryAvailable());
        }
        return new FileBlob(zipFile, "application/zip", null, compound.getName() + ".zip", null);
    }

    /**
     * Turn the archive blob into an object
     *
     * @param archiveBlob
     * @return
     */
    public CompoundArchive toCompoundArchive(Blob archiveBlob) {
        List<String> validEntryList = new ArrayList<>();
        String prefix = null;
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(archiveBlob.getStream()))) {
            ZipEntry entry;
            boolean isSupported = false;
            while ((entry = zin.getNextEntry()) != null
                    && (isSupported = !MARKER_BLACKLIST.contains(entry.getName()))) {
                String name = entry.getName();
                if (isValidEntry(name)) {
                    validEntryList.add(name);

                    // get path prefix
                    String folderPath;
                    if (entry.isDirectory()) {
                        folderPath = name;
                    } else {
                        folderPath = FilenameUtils.getPath(name);
                    }

                    // initialize the prefix with the first file in order to manage multiple prefix folder levels
                    if (prefix == null && !entry.isDirectory()) {
                        prefix = folderPath;
                    } else if (StringUtils.isNotBlank(prefix)) {
                        prefix = longestSubstr(prefix, folderPath);
                    }
                }
            }
            if (!isSupported) {
                throw new NuxeoException("Unsupported archive");
            } else {
                return new CompoundArchive(archiveBlob, validEntryList, prefix);
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * filter OS crap
     *
     * @param entry
     * @return
     */
    public boolean isValidEntry(String entry) {
        if (entry.contains("__MACOSX/") || entry.contains("../") || entry.endsWith(".DS_Store")) {
            return false;
        }

        String filename = FilenameUtils.getName(entry);
        return !filename.startsWith(".") && !filename.contentEquals("desktop.ini") && !filename.contentEquals("Thumbs.db")
                && !filename.startsWith("Icon");
    }

    public String longestSubstr(String s, String t) {
        if (s.isEmpty() || t.isEmpty()) {
            return "";
        }

        int m = s.length();
        int n = t.length();
        int cost = 0;
        int maxLen = 0;
        int[] p = new int[n];
        int[] d = new int[n];

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                // calculate cost/score
                if (s.charAt(i) != t.charAt(j)) {
                    cost = 0;
                } else {
                    if ((i == 0) || (j == 0)) {
                        cost = 1;
                    } else {
                        cost = p[j - 1] + 1;
                    }
                }
                d[j] = cost;

                if (cost > maxLen) {
                    maxLen = cost;
                }
            } // for {}

            int[] swap = p;
            p = d;
            d = swap;
        }

        return maxLen > 0 ? s.substring(0, maxLen) : "";
    }
}
