package io.switchbit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.springframework.cloud.skipper.client.DefaultSkipperClient;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.hateoas.Resources;

@Mojo(name = "skipper-upload", defaultPhase = LifecyclePhase.PACKAGE)
public class SkipperUploadMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "skipper.workDir", defaultValue = "${project.build.directory}/skipper")
	private File workDirectory;

	@Parameter(property = "skipper.server.uri", defaultValue = "http://localhost:7577/api")
	private String skipperServerUri;

	@Parameter(property = "skipper.repo.name", defaultValue = "local")
	private String repoName;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info(String.format(
				"Uploading application [%s] to Spring Cloud Skipper instance: %s",
				this.project.getArtifactId(), this.skipperServerUri));

		SkipperClient skipperClient = new DefaultSkipperClient(this.skipperServerUri);

		boolean upgrade = false;
		Resources<PackageMetadata> packages = skipperClient
				.search(this.project.getArtifactId(), true);
		for (PackageMetadata packageMetadata : packages) {
			if (packageMetadata.getName().equals(this.project.getArtifactId())
					&& packageMetadata.getVersion().equals(this.project.getVersion())) {
				upgrade = true;
				break;
			}
		}

		if (!upgrade) {
			// see org.springframework.cloud.skipper.shell.command.SkipperCommands#upload
			UploadRequest uploadRequest = new UploadRequest();
			uploadRequest.setRepoName(this.repoName);
			uploadRequest.setName(this.project.getArtifactId());
			uploadRequest.setVersion(this.project.getVersion());
			uploadRequest.setExtension("zip");

			try {
				File packageZipFile = new File(this.workDirectory, String.format("%s-%s.zip",
						this.project.getArtifactId(), this.project.getVersion()));
				uploadRequest.setPackageFileAsBytes(
						Files.readAllBytes(packageZipFile.toPath()));
			}
			catch (IOException e) {
				throw new MojoExecutionException("Error reading compressed package", e);
			}

			PackageMetadata metadata = skipperClient.upload(uploadRequest);
			getLog().info(String.format("Uploaded package: %s", metadata));
		}
		else {
			throw new UnsupportedOperationException(
					"Upgrading package not currently supported by Skipper");
		}
	}
}
