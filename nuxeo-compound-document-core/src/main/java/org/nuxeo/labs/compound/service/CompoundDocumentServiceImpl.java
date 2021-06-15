package org.nuxeo.labs.compound.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.nuxeo.ecm.core.io.ExportConstants.MARKER_FILE;

public class CompoundDocumentServiceImpl extends DefaultComponent implements CompoundDocumentService {

    public static final String COMPOUND_ARCHIVE_IMPORTED = "compoundDocumentTreeImported";
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
    public String getTargetCompoundDocumentTypeFromContext(DocumentModel parent, Blob archiveBlob) {
        List<String> entries = new ArrayList<>();
        if (!isSupportedArchiveFile(archiveBlob, entries)) {
            return null;
        }
        AutomationService automationService = Framework.getService(AutomationService.class);
        OperationContext ctx = new OperationContext(parent.getCoreSession());
        Map<String, Object> params = new HashMap<>();
        ctx.setInput(parent);
        ctx.put("blob", archiveBlob);
        ctx.put("entries", entries);
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
    public DocumentModel createCompoundFromArchive(DocumentModel parent, Blob archiveBlob, String documentType) throws IOException {
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
        String targetType = getTargetCompoundDocumentTypeFromContext(parent, archiveBlob);
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
        CoreSession session = compound.getCoreSession();
        FileManagerService fileManager = (FileManagerService) Framework.getService(FileManager.class);

        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(archiveBlob.getStream()));
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            String filename = entry.getName();

            // filter OS crap
            if (!isValidEntry(filename)) {
                continue;
            }

            Path path = Path.of(entry.getName());
            String componentName = path.getFileName().toString();
            String ComponentParentPath = path.getParent() != null ? path.getParent().toString() : "";

            String nuxeoPath = compound.getPathAsString() + "/" + ComponentParentPath;

            if (entry.isDirectory()) {
                DocumentModel folder = fileManager.defaultCreateFolder(
                        session, componentName, nuxeoPath, getSubFolderTypeFromContext(
                                compound, nuxeoPath, componentName), true, true);
                if (folder == null) {
                    throw new NuxeoException("Filemanager couldn't create the folder: " + nuxeoPath);
                }

            } else {
                Blob fileBlob = new FileBlob(new CloseShieldInputStream(zin));
                fileBlob.setFilename(componentName);
                FileImporterContext context = FileImporterContext.builder(session, fileBlob, nuxeoPath)
                        .overwrite(true)
                        .build();
                fileManager.createOrUpdateDocument(context);
            }
        }

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), compound);
        Event event = ctx.newEvent(COMPOUND_ARCHIVE_IMPORTED);
        Framework.getService(EventService.class).fireEvent(event);
        session.saveDocument(compound);
    }

    @Override
    public boolean isSupportedArchiveFile(Blob archiveBlob, List<String> outputEntryList) {
        if (!"application/zip".equals(archiveBlob.getMimeType())) {
            return false;
        }
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(archiveBlob.getStream()))) {
            ZipEntry entry;
            boolean isSupported = false;
            while ((entry = zin.getNextEntry()) != null && (isSupported = !MARKER_BLACKLIST.contains(entry.getName()))) {
                if (outputEntryList != null) {
                    if (isValidEntry(entry.getName())) {
                        outputEntryList.add(entry.getName());
                    }
                }
            }
            return isSupported;
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
        if (entry.startsWith("__MACOSX/") || entry.startsWith(".") || entry.contentEquals("desktop.ini")
                || entry.contentEquals("Thumbs.db") || entry.startsWith("Icon")
                // Avoid hacks trying to access a directory outside the current one
                || entry.contentEquals("../") || entry.endsWith(".DS_Store")) {
            return false;
        } else {
            return true;
        }
    }

}
