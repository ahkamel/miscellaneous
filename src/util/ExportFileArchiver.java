package util;

import static java.io.File.separator;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static java.nio.file.Files.move;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.mutable.MutableLong;


public class ExportFileArchiver {

	private static final Logger logger = Logger.getLogger(ExportFileArchiver.class.getName());

	private final String fileArchivedPath;
	private final Long maxSizeOfArchiveInBytes;
	private final int maxNumberOfArchivedFiles;

	public ExportFileArchiver(String fileArchivedPath, Long maxSizeOfArchiveInBytes, int maxNumberOfArchivedFiles) {
		this.fileArchivedPath = fileArchivedPath;
		this.maxSizeOfArchiveInBytes = maxSizeOfArchiveInBytes;
		this.maxNumberOfArchivedFiles = maxNumberOfArchivedFiles;
	}

	public void archive(File file) {
		try {
			while (getTotalNumberOfFiles() >= maxNumberOfArchivedFiles || (getTotalArchivedFolderSize() + file.length()) >= maxSizeOfArchiveInBytes) {
				deleteOldestFileCreated();
			}
			moveFile(file);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not archive file " + file, e);
			throw new RuntimeException(e);
		}
	}

	private Long getTotalArchivedFolderSize() throws IOException {
		return getArchiveFolderSize();
	}

	private int getTotalNumberOfFiles() {
		return getFilesNumber() + 1;
	}

	private void deleteOldestFileCreated() throws IOException {
		Path dir = get(fileArchivedPath);
		Optional<Path> lastFilePath = list(dir).filter(f -> !isDirectory(f)).max((f1, f2) -> (int) (f2.toFile().lastModified() - f1.toFile().lastModified()));
		if (lastFilePath.isPresent()) {
			lastFilePath.get().toFile().delete();
		}
	}

	private void moveFile(File file) {
		Path source = get(file.toURI());
		Path target = get(fileArchivedPath + separator + file.getName());
		try {
			move(source, target, REPLACE_EXISTING);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not move file " + file.getAbsolutePath() + " to " + fileArchivedPath, e);
		}
	}

	private int getFilesNumber() {
		return new File(fileArchivedPath).listFiles().length;
	}

	private Long getArchiveFolderSize() throws IOException {
		MutableLong size = new MutableLong();
		Path directoryPath = get(fileArchivedPath);
		walkFileTree(directoryPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				size.add(attrs.size());
				return CONTINUE;
			}
		});
		return size.toLong();
	}
}