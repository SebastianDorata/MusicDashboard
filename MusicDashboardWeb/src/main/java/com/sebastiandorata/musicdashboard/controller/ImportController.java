package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.service.Import.ImportJobRegistry;
import com.sebastiandorata.musicdashboard.service.Import.ImportJobRegistry.Job;
import com.sebastiandorata.musicdashboard.service.Import.ImportJobRegistry.JobSnapshot;
import com.sebastiandorata.musicdashboard.service.Import.ImportOrchestrator;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Web entry point for all import functionality (the desktop "Import Music" screen).
 *
 * <p>The desktop offered two workflows that both fed {@link ImportOrchestrator}: pick a folder
 * with the {@code DirectoryChooser} and press Start Scan, or drop files/folders on a drop zone.
 * On the web the same two entry points exist: {@code POST /api/import/scan} imports a folder that
 * lives on the machine running the app (files stay where they are), and
 * {@code POST /api/import/upload} receives files from the browser, stores them under the
 * configured upload folder and imports them. Both return a job id immediately; the page polls
 * {@code GET /api/import/jobs/{jobId}} to drive the progress bar and the results table.
 *
 * <p>SRP: translates HTTP requests into orchestrator calls and job lookups; the pipeline itself
 * lives in {@link ImportOrchestrator} and the progress bookkeeping in {@link ImportJobRegistry}.
 *
 * <p>DIP: depends on the orchestrator and the registry abstractions, holds no per-user state.
 */
@Controller
public class ImportController {

    private static final String NO_FILES_TEXT = "No supported audio files found (.mp3, .m4a)";

    @Autowired private ImportOrchestrator orchestrator;
    @Autowired private ImportJobRegistry jobRegistry;
    @Autowired private UserSessionService userSessionService;

    @Value("${musicdashboard.upload-dir}")
    private String uploadDir;

    /**
     * Body of {@code POST /api/import/scan}.
     *
     * @param path absolute path of a folder on the machine running the app
     */
    public record ScanRequest(String path) { }

    /**
     * Response of the endpoints that start an import.
     *
     * @param jobId the id to poll at {@code /api/import/jobs/{jobId}}
     */
    public record JobStarted(String jobId) { }

    /**
     * One row of the results table.
     *
     * @param fileName the short file name
     * @param status   the {@code MigrationResult.Status} constant name
     * @param message  the details text
     */
    public record ResultRow(String fileName, String status, String message) { }

    /**
     * Progress of an import job.
     *
     * @param jobId    the job id
     * @param state    {@code RUNNING}, {@code DONE}, {@code NO_FILES} or {@code FAILED}
     * @param current  1-based index of the file being processed
     * @param total    total number of files (0 until known)
     * @param fileName short name of the file being processed
     * @param message  failure text for {@code FAILED}, otherwise {@code null}
     * @param results  results table rows (only filled once the job is done)
     */
    public record JobStatus(String jobId, String state, int current, int total, String fileName,
                            String message, List<ResultRow> results) { }

    /**
     * Renders the Import Music page.
     *
     * <p>The model attribute {@code defaultFolder} holds {@code <user.home>/Music} when that folder
     * exists (the desktop chooser's initial directory), otherwise an empty string.
     *
     * @param model the MVC model
     * @return the {@code import} template name
     */
    @GetMapping("/import")
    public String importPage(Model model) {
        File defaultDir = new File(System.getProperty("user.home"), "Music");
        model.addAttribute("defaultFolder", defaultDir.isDirectory() ? defaultDir.getAbsolutePath() : "");
        return "import";
    }

    /**
     * Starts importing a folder that lives on the server, in place (desktop "Browse + Start Scan").
     *
     * @param request the body holding the folder path
     * @return the id of the new job
     * @throws ResponseStatusException {@code BAD_REQUEST} when the path is blank, missing or not a folder
     */
    @PostMapping("/api/import/scan")
    @ResponseBody
    public JobStarted scan(@RequestBody ScanRequest request) {
        String raw = request == null || request.path() == null ? "" : request.path().trim();
        if (raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a folder path.");
        }
        if (raw.equals("~") || raw.startsWith("~/")) {
            raw = System.getProperty("user.home") + raw.substring(1);
        }
        File folder = new File(raw);
        if (!folder.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder does not exist: " + raw);
        }
        if (!folder.isDirectory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a folder: " + raw);
        }
        return start(List.of(folder.getAbsoluteFile()));
    }

    /**
     * Receives files chosen or dropped in the browser, stores them and starts importing them.
     *
     * <p>Multipart fields: {@code files} (repeated, the audio files) and {@code paths} (repeated,
     * parallel to {@code files}: the relative path of each file, for example
     * {@code Album/01 Song.mp3}). Files are stored under
     * {@code <musicdashboard.upload-dir>/<userId>/<relative path>} so an album stays in one folder
     * and re-uploading a file lands on the same path. Non-audio files are ignored.
     *
     * @param request the multipart request
     * @return the id of the new job
     * @throws ResponseStatusException {@code BAD_REQUEST} for an unsafe path, {@code 500} when a file cannot be stored
     */
    @PostMapping("/api/import/upload")
    @ResponseBody
    public JobStarted upload(MultipartHttpServletRequest request) {
        List<MultipartFile> files = request.getFiles("files");
        String[] paths = request.getParameterValues("paths");
        Long userId = userSessionService.getCurrentUserId();
        Path root = Path.of(uploadDir).toAbsolutePath().normalize().resolve(String.valueOf(userId)).normalize();

        List<File> saved = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile part = files.get(i);
            String relative = paths != null && i < paths.length && paths[i] != null && !paths[i].isBlank()
                    ? paths[i] : part.getOriginalFilename();
            if (!isSupported(relative)) continue;
            Path target = resolveSafely(root, relative);
            try {
                Files.createDirectories(target.getParent());
                try (InputStream in = part.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not store " + target.getFileName() + ": " + e.getMessage());
            }
            saved.add(target.toFile());
        }
        return start(saved);
    }

    /**
     * Returns the progress of an import job to the user who started it.
     *
     * @param jobId the job id returned by the start endpoints
     * @return the job status
     * @throws ResponseStatusException {@code NOT_FOUND} when the job is unknown, expired or not the caller's
     */
    @GetMapping("/api/import/jobs/{jobId}")
    @ResponseBody
    public JobStatus job(@PathVariable String jobId) {
        JobSnapshot s = jobRegistry.snapshot(jobId, userSessionService.getCurrentUserId());
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Import job not found");
        }
        List<ResultRow> rows = s.results().stream()
                .map(r -> new ResultRow(r.fileName(), r.status().name(), r.message()))
                .toList();
        String message = s.state() == ImportJobRegistry.State.NO_FILES ? NO_FILES_TEXT : s.message();
        return new JobStatus(s.jobId(), s.state().name(), s.current(), s.total(), s.fileName(), message, rows);
    }

    /**
     * Creates a job owned by the current user and hands the inputs to the orchestrator.
     *
     * @param inputs files or folders to import
     * @return the started job's id
     */
    private JobStarted start(List<File> inputs) {
        Job job = jobRegistry.create(userSessionService.getCurrentUserId());
        try {
            orchestrator.process(inputs, job);
        } catch (RuntimeException e) {
            job.fail("Import failed: " + e.getMessage());
        }
        return new JobStarted(job.getId());
    }

    /**
     * Resolves a client-supplied relative path below the upload root, sanitising every segment.
     *
     * @param root     the user's upload folder (absolute, normalised)
     * @param relative the client's relative path ({@code /} or {@code \} separated)
     * @return the absolute target path inside {@code root}
     * @throws ResponseStatusException {@code BAD_REQUEST} for absolute paths, {@code ..} segments or escapes
     */
    private Path resolveSafely(Path root, String relative) {
        String trimmed = relative.trim();
        if (trimmed.startsWith("/") || trimmed.startsWith("\\") || trimmed.matches("^[A-Za-z]:.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Absolute paths are not allowed: " + relative);
        }
        Path target = root;
        int segments = 0;
        for (String seg : trimmed.split("[/\\\\]")) {
            if (seg.isBlank() || seg.equals(".")) continue;
            if (seg.equals("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path: " + relative);
            }
            String clean = seg.replaceAll("[\\p{Cntrl}:*?\"<>|]", "_").strip();
            if (clean.isEmpty() || clean.equals("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path: " + relative);
            }
            target = target.resolve(clean);
            segments++;
        }
        target = target.normalize();
        if (segments == 0 || !target.startsWith(root) || target.equals(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path: " + relative);
        }
        return target;
    }

    /**
     * Applies the same extension rule as {@code ImportOrchestrator.isSupportedAudioFile}.
     *
     * @param name a file name or relative path
     * @return {@code true} for {@code .mp3} and {@code .m4a}
     */
    private boolean isSupported(String name) {
        if (name == null) return false;
        return orchestrator.isSupportedAudioFile(new File(name));
    }
}
