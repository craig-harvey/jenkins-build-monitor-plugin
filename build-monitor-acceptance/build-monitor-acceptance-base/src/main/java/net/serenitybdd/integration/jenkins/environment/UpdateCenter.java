package net.serenitybdd.integration.jenkins.environment;

import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class UpdateCenter {

    private static final Logger Log = LoggerFactory.getLogger(UpdateCenter.class);

    private static final String Update_Center_URL_Template
            // = "http://updates.jenkins-ci.org/stable-%s/update-center.json";
            = "https://ftp-chi.osuosl.org/pub/jenkins/updates/stable-%s/update-center.json";
    private final Path tempDir;

    private List<Version> jenkinsLTSVersions = Arrays.asList(
      Version.valueOf("2.107.0"),
      Version.valueOf("2.89.0"),
      Version.valueOf("2.73.0"),
      Version.valueOf("2.60.0"),
      Version.valueOf("2.46.0")
    );


  public UpdateCenter() {
        this(Directories.Default_Temp_Dir);
    }

    public UpdateCenter(Path tempDir) {
        this.tempDir = tempDir;
    }

    public String jsonFor(String jenkinsVersion) {
        try {
            return stripJSONPEnvelope(download(updateCenterJSONPFor(jenkinsVersion)));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't download 'update-center.json'.", e);
        }
    }

    private URL updateCenterJSONPFor(String jenkinsVersion) throws MalformedURLException {
        String versionToUse = getUpdateVersionToUse(jenkinsVersion);
        URL url = url(Update_Center_URL_Template, versionToUse);
        Log.info("Jenkins update URL is {}", url.toString());
        return url;

    }

    String getUpdateVersionToUse(String jenkinsVersionString) {
        Version jenkinsVersion = Version.valueOf(jenkinsVersionString);
        Version lowestLTS = jenkinsLTSVersions.get(jenkinsLTSVersions.size() - 1);
        if (jenkinsVersion.lessThan(lowestLTS)) {
            throw new RuntimeException("Can't test Jenkins versions lower than " + lowestLTS + ".");
        }
        Version versionToUse = null;
        for (Version ltsVersion : jenkinsLTSVersions) {
            if (jenkinsVersion.greaterThanOrEqualTo(ltsVersion)) {
                versionToUse = ltsVersion;
                break;
            }
        }
        assert versionToUse != null; //versionToUse should never be null since we already made sure jenkinsVersion isn't lower than the lowest LTS.
        return versionToUse.getMajorVersion() + "." + versionToUse.getMinorVersion();
    }

    private String stripJSONPEnvelope(Path jsonp) throws IOException {
        return Files.readAllLines(jsonp, Charsets.UTF_8).get(1);
    }

    private Path download(URL link) throws IOException {
        Files.createDirectories(tempDir);

        Path destination = Files.createTempFile(tempDir, "", ".tmp");

        ReadableByteChannel rbc = Channels.newChannel(link.openStream());
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(destination.toAbsolutePath().toFile());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            return destination;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    private URL url(String template, String... params) throws MalformedURLException {
        return new URL(String.format(template, params));
    }
}
