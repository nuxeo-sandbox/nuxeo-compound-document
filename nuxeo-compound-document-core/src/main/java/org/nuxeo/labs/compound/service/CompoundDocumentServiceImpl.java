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
import org.nuxeo.labs.compound.api.CompoundArchive;
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
        createStructureFromArchive(compound,archive);
    }

    public void createStructureFromArchive(DocumentModel compound, CompoundArchive archive) throws IOException {
        CoreSession session = compound.getCoreSession();
        FileManagerService fileManager = (FileManagerService) Framework.getService(FileManager.class);

        String prefix = archive.getPathPrefix();

        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(archive.getBlob().getStream()));
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            String entryName = entry.getName();

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

            String normalizedEntryName = entryName.endsWith("/") ? entryName.substring(0,entryName.length()-1) : entryName;
            String componentName = FilenameUtils.getName(normalizedEntryName);
            String ComponentParentPath = FilenameUtils.getPath(normalizedEntryName);

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
    public boolean isSupportedArchiveFile(Blob archiveBlob) {
        return toCompoundArchive(archiveBlob) != null;
    }

    /**
     * Turn the archive blob into an object
     * @param archiveBlob
     * @return
     */
    public CompoundArchive toCompoundArchive(Blob archiveBlob) {
        List<String> validEntryList = new ArrayList<>();
        String prefix = null;
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(archiveBlob.getStream()))) {
            ZipEntry entry;
            boolean isSupported = false;
            while ((entry = zin.getNextEntry()) != null && (isSupported = !MARKER_BLACKLIST.contains(entry.getName()))) {
                String name = entry.getName();
                if (isValidEntry(name)) {
                    validEntryList.add(name);

                    //get path prefix
                    String folderPath;
                    if (entry.isDirectory()) {
                        folderPath = name;
                    } else {
                        folderPath = FilenameUtils.getPath(name);
                    }
                    if (prefix == null) {
                        prefix = folderPath;
                    } else if (StringUtils.isNotBlank(prefix)) {
                        prefix = longestSubstr(prefix,folderPath);
                    }
                }
            }
            if (!isSupported) {
                throw new NuxeoException("Unsupported archive");
            } else {
                return new CompoundArchive(archiveBlob,validEntryList,prefix);
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

        Path path = Path.of(entry);
        String filename = path.getFileName().toString();
        if (filename.startsWith(".") || filename.contentEquals("desktop.ini")
                || filename.contentEquals("Thumbs.db") || filename.startsWith("Icon")) {
            return false;
        }

        return true;
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

        return maxLen > 0 ? s.substring(0,maxLen) : "";
    }


}
