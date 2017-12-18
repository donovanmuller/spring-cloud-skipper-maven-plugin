package io.switchbit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Mojo(name = "skipper-package", defaultPhase = LifecyclePhase.PACKAGE)
public class SkipperPackageMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	@Component(role = MavenFileFilter.class, hint = "default")
	private MavenFileFilter mavenFileFilter;

	@Parameter(property = "skipper.overrideDirectory", defaultValue = "${project.build.directory}/classes/META-INF/skipper")
	private File overrideDirectory;

	@Parameter(property = "skipper.workDir", defaultValue = "${project.build.directory}/skipper")
	private File workDirectory;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void execute() {
		getLog().info(String.format("Packaging application [%s] for Spring Cloud Skipper",
				this.project.getArtifactId()));

		Map<String, File> files = new HashMap<>();
		File packageDirectory = new File(this.workDirectory,
				String.format("%s-%s", this.project.getArtifactId(), this.project.getVersion()));
		packageDirectory.mkdirs();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		try {
			files.putAll(copyPackageFiles(packageDirectory, resolver));
			files.putAll(copyOverriddenPackageFiles(resolver));

			filterPackageFiles(files, packageDirectory);
			File zipPackage = zipPackage(packageDirectory);

			getLog().info(String.format("Skipper package available at %s", zipPackage.getAbsolutePath()));
		}
		catch (IOException e) {
			getLog().error("Error packaging application", e);
		}
	}

	private File zipPackage(File packageDirectory) {
		getLog().debug(String.format("Zipping package in %s", this.workDirectory));

		File packageZipFile = new File(this.workDirectory, String.format("%s-%s.zip",
				this.project.getArtifactId(), this.project.getVersion()));
		ZipUtil.pack(packageDirectory, packageZipFile, true);

		return packageZipFile;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void filterPackageFiles(Map<String, File> files, File packageDirectory) {
		files.forEach((key, value) -> {
			try {
				File file = new File(packageDirectory, key);
				file.getParentFile().mkdirs();
				file.createNewFile();
				this.mavenFileFilter.copyFile(value, file, true, this.project, null, false,
						"utf8", this.session);
			}
			catch (MavenFilteringException | IOException e) {
				getLog().error("Error filtering package files", e);
			}
		});
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Map<String, File> copyPackageFiles(File packageDirectory,
			PathMatchingResourcePatternResolver resolver) throws IOException {
		Map<String, File> files = new HashMap<>();

		Resource[] resources = resolver.getResources("classpath:skipper/**");
		for (Resource resource : resources) {
			String path = org.apache.commons.lang3.StringUtils
					.substringAfterLast(resource.getURL().getPath(), "!/skipper");
			getLog().debug("Writing resource: " + path);
			if (!path.endsWith("/")) {
				File file = new File(packageDirectory, path);
				file.getParentFile().mkdirs();
				file.createNewFile();
				File working = File.createTempFile(path, "working");
				getLog().debug("Writing working file: " + working);
				writeToFile(working, toString(resource), Charset.defaultCharset());
				files.put(path, working);
			}
		}

		return files;
	}

	private Map<String, File> copyOverriddenPackageFiles(
			PathMatchingResourcePatternResolver resolver) throws IOException {
		Map<String, File> files = new HashMap<>();

		Resource[] overriddenResources = resolver
				.getResources(String.format("file:%s/**", this.overrideDirectory.getPath()));
		for (Resource resource : overriddenResources) {
			String path = org.apache.commons.lang3.StringUtils
					.substringAfterLast(resource.getURL().getPath(), "/skipper");
			if (!resource.getFile().isDirectory()) {
				getLog().debug("Overriding file: " + path);
				files.put(path, resource.getFile());
			}
		}

		return files;
	}

	private void writeToFile(File file, String content, Charset charset)
			throws IOException {
		FileOutputStream outputStream;
		OutputStreamWriter writer;
		if (file == null) {
			throw new FileNotFoundException("No file specified.");
		}
		else if (!file.exists() && !file.getParentFile().exists()
				&& !file.getParentFile().mkdirs()) {
			throw new FileNotFoundException(
					"Could not find or create file:" + file.getName());
		}
		outputStream = new FileOutputStream(file);
		writer = new OutputStreamWriter(outputStream, charset);
		writer.write(content, 0, content.length());
		writer.flush();
	}

	private String toString(Resource resource) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(resource.getInputStream()), 1024);
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append('\n');
			}
			reader.close();
			return stringBuilder.toString();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
