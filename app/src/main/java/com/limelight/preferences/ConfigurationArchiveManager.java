package com.limelight.preferences;

import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;

import com.limelight.BuildConfig;
import com.limelight.LimeLog;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsBackupCodec;
import com.limelight.settings.SettingsKeyCatalog;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.transfer.TransferSettingKeys;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Android storage adapter for the versioned Moonlight configuration archive.
 *
 * <p>All selected components are fully staged and validated before any live
 * state is changed. App settings commit through the typed schema, pairing data
 * restores through the portable host database, and the client identity is
 * validated as a matching certificate/private-key pair.</p>
 */
final class ConfigurationArchiveManager {
    static final long MAXIMUM_ARCHIVE_BYTES = 96L * 1024L * 1024L;

    private static final String WORK_DIRECTORY = "configuration-archives";
    private static final String CERTIFICATE_FILE_NAME = "client.crt";
    private static final String PRIVATE_KEY_FILE_NAME = "client.key";
    private static final String SETTINGS_ENTRY_NAME = "app-settings.json";
    private static final String HOSTS_ENTRY_NAME = "paired-hosts.db";
    private static final long MAXIMUM_MANIFEST_BYTES = 64L * 1024L;
    private static final long MAXIMUM_SETTINGS_BYTES = 2L * 1024L * 1024L;
    private static final long MAXIMUM_HOST_DATABASE_BYTES =
            64L * 1024L * 1024L;
    private static final long MAXIMUM_IDENTITY_FILE_BYTES =
            4L * 1024L * 1024L;
    private static final int BUFFER_SIZE = 32 * 1024;
    private static final Collection<SettingKey<?>> PORTABLE_SETTINGS =
            createPortableSettings();

    private final Context context;
    private final SettingsRepository repository;

    ConfigurationArchiveManager(
            Context context,
            SettingsRepository repository) {
        Context checkedContext = Objects.requireNonNull(context, "context");
        Context applicationContext = checkedContext.getApplicationContext();
        this.context = applicationContext == null
                ? checkedContext
                : applicationContext;
        this.repository = Objects.requireNonNull(
                repository,
                "repository");
    }

    File createExportArchive() throws Exception {
        File work = createWorkDirectory("export");
        File output = null;
        ComputerDatabaseManager databaseManager = null;
        try {
            File settings = new File(work, SETTINGS_ENTRY_NAME);
            try (OutputStream stream = new BufferedOutputStream(
                    new FileOutputStream(settings))) {
                SettingsBackupCodec.write(
                        exportSettingsRepository(),
                        PORTABLE_SETTINGS,
                        stream);
            }

            File hosts = new File(work, HOSTS_ENTRY_NAME);
            databaseManager = new ComputerDatabaseManager(context);
            databaseManager.writePortableSnapshot(hosts);

            ensureClientIdentityExists();
            File certificate = new File(
                    context.getFilesDir(),
                    CERTIFICATE_FILE_NAME);
            File privateKey = new File(
                    context.getFilesDir(),
                    PRIVATE_KEY_FILE_NAME);
            validateIdentity(certificate, privateKey);

            EnumMap<ConfigurationArchiveComponent, Map<String, File>>
                    sources = new EnumMap<>(
                            ConfigurationArchiveComponent.class);
            sources.put(
                    ConfigurationArchiveComponent.APP_SETTINGS,
                    singletonFiles(SETTINGS_ENTRY_NAME, settings));
            LinkedHashMap<String, File> pairing = new LinkedHashMap<>();
            pairing.put(HOSTS_ENTRY_NAME, hosts);
            pairing.put(CERTIFICATE_FILE_NAME, certificate);
            pairing.put(PRIVATE_KEY_FILE_NAME, privateKey);
            sources.put(
                    ConfigurationArchiveComponent.PAIRING_DATA,
                    pairing);

            ConfigurationArchiveManifest manifest = createManifest(sources);
            output = uniqueArchiveFile();
            writeArchive(output, manifest, sources);
            return output;
        }
        catch (Exception error) {
            if (output != null &&
                    output.exists() &&
                    !output.delete()) {
                LimeLog.warning(
                        "Unable to remove incomplete configuration archive");
            }
            throw error;
        }
        finally {
            closeDatabase(databaseManager);
            deleteRecursively(work);
        }
    }

    PreparedImport prepareImport(Uri source) throws Exception {
        Objects.requireNonNull(source, "source");
        File archive = File.createTempFile(
                "configuration-import-",
                ".zip",
                requireWorkRoot());
        boolean successful = false;
        try {
            try (InputStream input = openInput(source);
                 OutputStream output = new BufferedOutputStream(
                         new FileOutputStream(archive))) {
                copy(input, output, MAXIMUM_ARCHIVE_BYTES);
                output.flush();
            }
            ConfigurationArchiveManifest manifest = validateArchive(archive);
            successful = true;
            return new PreparedImport(archive, manifest);
        }
        finally {
            if (!successful && archive.exists() && !archive.delete()) {
                LimeLog.warning(
                        "Unable to remove rejected configuration archive");
            }
        }
    }

    ImportResult importSelected(
            PreparedImport preparedImport,
            Set<ConfigurationArchiveComponent> selected)
            throws Exception {
        Objects.requireNonNull(preparedImport, "preparedImport");
        Objects.requireNonNull(selected, "selected");
        preparedImport.ensureOpen();
        EnumSet<ConfigurationArchiveComponent> selection = selected.isEmpty()
                ? EnumSet.noneOf(ConfigurationArchiveComponent.class)
                : EnumSet.copyOf(selected);
        if (selection.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one configuration component is required");
        }
        if (!preparedImport.getComponents().containsAll(selection)) {
            throw new IllegalArgumentException(
                    "Selected configuration component is unavailable");
        }

        File work = createWorkDirectory("import");
        try {
            extractSelected(
                    preparedImport.archive,
                    preparedImport.manifest,
                    selection,
                    work);

            SettingsBackupCodec.Snapshot importedSettings = null;
            String importedLanguage = null;
            if (selection.contains(
                    ConfigurationArchiveComponent.APP_SETTINGS)) {
                try (InputStream input = new BufferedInputStream(
                        new FileInputStream(
                                new File(work, SETTINGS_ENTRY_NAME)))) {
                    importedSettings = SettingsBackupCodec.read(
                            PORTABLE_SETTINGS,
                            input);
                }
                importedLanguage = importedSettings.get(
                        AppPresentationSettingKeys.LANGUAGE);
            }

            PairingImport pairingImport = null;
            if (selection.contains(
                    ConfigurationArchiveComponent.PAIRING_DATA)) {
                pairingImport = validatePairingImport(work);
            }

            SettingsBackupCodec.Snapshot rollbackSettings = null;
            if (importedSettings != null) {
                rollbackSettings = captureCurrentSettings(work);
            }
            PairingRollback pairingRollback = pairingImport == null
                    ? null
                    : captureCurrentPairing(work);

            boolean settingsApplied = false;
            boolean pairingApplied = false;
            try {
                if (pairingImport != null) {
                    pairingApplied = true;
                    applyPairingImport(pairingImport);
                }
                if (importedSettings != null) {
                    settingsApplied = true;
                    if (!importedSettings.applyTo(repository)) {
                        throw new IOException(
                                "Unable to commit imported application settings");
                    }
                }
            }
            catch (Exception error) {
                rollback(
                        rollbackSettings,
                        settingsApplied,
                        pairingRollback,
                        pairingApplied,
                        error);
                throw error;
            }
            finally {
                if (pairingImport != null) {
                    pairingImport.close();
                }
            }

            return new ImportResult(selection, importedLanguage);
        }
        finally {
            deleteRecursively(work);
            preparedImport.close();
        }
    }

    private ConfigurationArchiveManifest createManifest(
            Map<ConfigurationArchiveComponent, Map<String, File>> sources)
            throws Exception {
        EnumMap<ConfigurationArchiveComponent,
                Map<String, ConfigurationArchiveManifest.Entry>> entries =
                new EnumMap<>(ConfigurationArchiveComponent.class);
        for (Map.Entry<ConfigurationArchiveComponent, Map<String, File>>
                componentEntry : sources.entrySet()) {
            LinkedHashMap<String, ConfigurationArchiveManifest.Entry>
                    componentFiles = new LinkedHashMap<>();
            for (String name : componentEntry.getKey().getEntryNames()) {
                File file = componentEntry.getValue().get(name);
                if (file == null || !file.isFile()) {
                    throw new IOException(
                            "Missing configuration source: " + name);
                }
                componentFiles.put(
                        name,
                        new ConfigurationArchiveManifest.Entry(
                                name,
                                file.length(),
                                sha256(file)));
            }
            entries.put(componentEntry.getKey(), componentFiles);
        }
        return new ConfigurationArchiveManifest(
                System.currentTimeMillis(),
                context.getPackageName(),
                BuildConfig.VERSION_NAME,
                entries);
    }

    private void writeArchive(
            File destination,
            ConfigurationArchiveManifest manifest,
            Map<ConfigurationArchiveComponent, Map<String, File>> sources)
            throws IOException {
        AtomicFile atomicFile = new AtomicFile(destination);
        FileOutputStream fileOutput = null;
        try {
            fileOutput = atomicFile.startWrite();
            try (ZipOutputStream zip = new ZipOutputStream(
                    new BufferedOutputStream(
                            new NonClosingOutputStream(fileOutput)))) {
                zip.setLevel(Deflater.BEST_COMPRESSION);
                zip.putNextEntry(new ZipEntry(
                        ConfigurationArchiveManifest.ENTRY_NAME));
                OutputStreamWriter writer = new OutputStreamWriter(
                        new NonClosingOutputStream(zip),
                        StandardCharsets.UTF_8);
                manifest.write(writer);
                zip.closeEntry();

                for (ConfigurationArchiveComponent component :
                        ConfigurationArchiveComponent.values()) {
                    Map<String, File> componentFiles = sources.get(component);
                    if (componentFiles == null) {
                        continue;
                    }
                    for (String name : component.getEntryNames()) {
                        zip.putNextEntry(new ZipEntry(name));
                        try (InputStream input = new BufferedInputStream(
                                new FileInputStream(componentFiles.get(name)))) {
                            copy(input, zip, maximumBytesFor(name));
                        }
                        zip.closeEntry();
                    }
                }
                zip.finish();
            }
            atomicFile.finishWrite(fileOutput);
        }
        catch (IOException | RuntimeException error) {
            if (fileOutput != null) {
                atomicFile.failWrite(fileOutput);
            }
            throw error;
        }
    }

    private ConfigurationArchiveManifest validateArchive(File archive)
            throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            HashSet<String> actualNames = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                count++;
                if (count > 16 ||
                        entry.isDirectory() ||
                        !isRootEntryName(entry.getName()) ||
                        !actualNames.add(entry.getName())) {
                    throw new IOException(
                            "Configuration archive contains invalid entries");
                }
            }

            ZipEntry manifestEntry = zip.getEntry(
                    ConfigurationArchiveManifest.ENTRY_NAME);
            if (manifestEntry == null) {
                throw new IOException(
                        "Configuration archive has no manifest");
            }
            ConfigurationArchiveManifest manifest;
            byte[] manifestBytes = readZipEntry(
                    zip,
                    manifestEntry,
                    MAXIMUM_MANIFEST_BYTES);
            try (InputStreamReader reader = new InputStreamReader(
                    new ByteArrayInputStream(manifestBytes),
                    StandardCharsets.UTF_8)) {
                manifest = ConfigurationArchiveManifest.read(reader);
            }

            HashSet<String> expectedNames = new HashSet<>(
                    manifest.getAllEntryNames());
            expectedNames.add(ConfigurationArchiveManifest.ENTRY_NAME);
            if (!expectedNames.equals(actualNames)) {
                throw new IOException(
                        "Configuration archive entries do not match its manifest");
            }
            for (ConfigurationArchiveComponent component :
                    manifest.getComponents()) {
                for (String name : component.getEntryNames()) {
                    ConfigurationArchiveManifest.Entry metadata =
                            manifest.requireEntry(component, name);
                    validateZipEntry(zip, metadata);
                }
            }
            return manifest;
        }
    }

    private void validateZipEntry(
            ZipFile zip,
            ConfigurationArchiveManifest.Entry metadata)
            throws Exception {
        ZipEntry entry = zip.getEntry(metadata.getPath());
        if (entry == null || entry.isDirectory()) {
            throw new IOException(
                    "Missing configuration archive entry: " +
                            metadata.getPath());
        }
        if (metadata.getSize() > maximumBytesFor(metadata.getPath())) {
            throw new IOException(
                    "Configuration archive entry is too large: " +
                            metadata.getPath());
        }
        if (entry.getSize() >= 0 &&
                entry.getSize() != metadata.getSize()) {
            throw new IOException(
                    "Configuration archive entry size mismatch: " +
                            metadata.getPath());
        }
        MessageDigest digest = sha256Digest();
        long size;
        try (InputStream input = new BufferedInputStream(
                zip.getInputStream(entry))) {
            size = digest(input, digest, maximumBytesFor(metadata.getPath()));
        }
        if (size != metadata.getSize() ||
                !hex(digest.digest()).equals(metadata.getSha256())) {
            throw new IOException(
                    "Configuration archive entry integrity check failed: " +
                            metadata.getPath());
        }
    }

    private void extractSelected(
            File archive,
            ConfigurationArchiveManifest manifest,
            Set<ConfigurationArchiveComponent> selected,
            File destination)
            throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            for (ConfigurationArchiveComponent component : selected) {
                for (String name : component.getEntryNames()) {
                    ZipEntry entry = zip.getEntry(name);
                    ConfigurationArchiveManifest.Entry metadata =
                            manifest.requireEntry(component, name);
                    File output = new File(destination, name);
                    MessageDigest digest = sha256Digest();
                    long size;
                    try (InputStream input = new BufferedInputStream(
                            zip.getInputStream(entry));
                         OutputStream fileOutput = new BufferedOutputStream(
                                 new FileOutputStream(output))) {
                        size = copyAndDigest(
                                input,
                                fileOutput,
                                digest,
                                maximumBytesFor(name));
                    }
                    if (size != metadata.getSize() ||
                            !hex(digest.digest()).equals(
                                    metadata.getSha256())) {
                        throw new IOException(
                                "Configuration archive changed while importing");
                    }
                }
            }
        }
    }

    private PairingImport validatePairingImport(File work)
            throws Exception {
        File hosts = new File(work, HOSTS_ENTRY_NAME);
        File certificate = new File(work, CERTIFICATE_FILE_NAME);
        File privateKey = new File(work, PRIVATE_KEY_FILE_NAME);
        validateIdentity(certificate, privateKey);
        ComputerDatabaseManager importDatabase =
                new ComputerDatabaseManager(context, hosts);
        return new PairingImport(
                importDatabase,
                certificate,
                privateKey);
    }

    private SettingsBackupCodec.Snapshot captureCurrentSettings(File work)
            throws IOException {
        File rollback = new File(work, "rollback-settings.json");
        try (OutputStream output = new BufferedOutputStream(
                new FileOutputStream(rollback))) {
            SettingsBackupCodec.write(
                    repository,
                    PORTABLE_SETTINGS,
                    output);
        }
        try (InputStream input = new BufferedInputStream(
                new FileInputStream(rollback))) {
            return SettingsBackupCodec.read(
                    PORTABLE_SETTINGS,
                    input);
        }
    }

    private PairingRollback captureCurrentPairing(File work)
            throws Exception {
        File hosts = new File(work, "rollback-hosts.db");
        ComputerDatabaseManager manager = null;
        try {
            manager = new ComputerDatabaseManager(context);
            manager.writePortableSnapshot(hosts);
        }
        finally {
            closeDatabase(manager);
        }
        return new PairingRollback(
                hosts,
                readOwnedFileIfPresent(CERTIFICATE_FILE_NAME),
                readOwnedFileIfPresent(PRIVATE_KEY_FILE_NAME));
    }

    private void applyPairingImport(PairingImport pairingImport)
            throws Exception {
        ComputerDatabaseManager destination = null;
        try {
            destination = new ComputerDatabaseManager(context);
            destination.restoreFrom(pairingImport.database);
        }
        finally {
            closeDatabase(destination);
        }
        replaceIdentity(
                pairingImport.certificate,
                pairingImport.privateKey);
    }

    private void rollback(
            SettingsBackupCodec.Snapshot settings,
            boolean settingsApplied,
            PairingRollback pairing,
            boolean pairingApplied,
            Exception original) {
        if (settingsApplied &&
                settings != null &&
                !settings.applyTo(repository)) {
            original.addSuppressed(new IOException(
                    "Unable to roll back application settings"));
        }
        if (pairingApplied && pairing != null) {
            try {
                restorePairing(pairing);
            }
            catch (Exception rollbackError) {
                original.addSuppressed(rollbackError);
            }
        }
    }

    private void restorePairing(PairingRollback rollback)
            throws Exception {
        ComputerDatabaseManager source = null;
        ComputerDatabaseManager destination = null;
        try {
            source = new ComputerDatabaseManager(
                    context,
                    rollback.hosts);
            destination = new ComputerDatabaseManager(context);
            destination.restoreFrom(source);
        }
        finally {
            closeDatabase(source);
            closeDatabase(destination);
        }
        restoreOptionalFile(
                new File(context.getFilesDir(), CERTIFICATE_FILE_NAME),
                rollback.certificate);
        restoreOptionalFile(
                new File(context.getFilesDir(), PRIVATE_KEY_FILE_NAME),
                rollback.privateKey);
        AndroidCryptoProvider.invalidateCachedIdentity();
    }

    private void replaceIdentity(File certificate, File privateKey)
            throws IOException {
        byte[] certificateBytes = readFile(
                certificate,
                MAXIMUM_IDENTITY_FILE_BYTES);
        byte[] privateKeyBytes = readFile(
                privateKey,
                MAXIMUM_IDENTITY_FILE_BYTES);
        replaceBytesAtomically(
                new File(context.getFilesDir(), CERTIFICATE_FILE_NAME),
                certificateBytes);
        replaceBytesAtomically(
                new File(context.getFilesDir(), PRIVATE_KEY_FILE_NAME),
                privateKeyBytes);
        AndroidCryptoProvider.invalidateCachedIdentity();
    }

    private void replaceBytesAtomically(File destination, byte[] value)
            throws IOException {
        AtomicFile atomicFile = new AtomicFile(destination);
        FileOutputStream output = null;
        try {
            output = atomicFile.startWrite();
            output.write(value);
            output.flush();
            atomicFile.finishWrite(output);
        }
        catch (IOException | RuntimeException error) {
            if (output != null) {
                atomicFile.failWrite(output);
            }
            throw error;
        }
    }

    private void restoreOptionalFile(File destination, byte[] value)
            throws IOException {
        if (value != null) {
            replaceBytesAtomically(destination, value);
            return;
        }
        if (destination.exists() && !destination.delete()) {
            throw new IOException(
                    "Unable to remove rolled-back identity file: " +
                            destination.getName());
        }
    }

    private void validateIdentity(File certificate, File privateKey)
            throws Exception {
        byte[] certificateBytes = readFile(
                certificate,
                MAXIMUM_IDENTITY_FILE_BYTES);
        byte[] privateKeyBytes = readFile(
                privateKey,
                MAXIMUM_IDENTITY_FILE_BYTES);
        X509Certificate parsedCertificate = (X509Certificate)
                CertificateFactory.getInstance("X.509")
                        .generateCertificate(
                                new ByteArrayInputStream(certificateBytes));
        PrivateKey parsedKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        byte[] challenge =
                "Moonlight configuration identity check"
                        .getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(parsedKey);
        signer.update(challenge);
        byte[] signature = signer.sign();
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(parsedCertificate.getPublicKey());
        verifier.update(challenge);
        if (!verifier.verify(signature)) {
            throw new IOException(
                    "Client certificate and private key do not match");
        }
    }

    private void ensureClientIdentityExists() throws IOException {
        AndroidCryptoProvider provider = new AndroidCryptoProvider(context);
        if (provider.getClientCertificate() == null ||
                provider.getClientPrivateKey() == null) {
            throw new IOException(
                    "Unable to create the client identity");
        }
    }

    private SettingsRepository exportSettingsRepository() {
        String language = AndroidAppLocale.configuredLanguage(context);
        return new OverrideSettingsRepository(
                repository,
                Collections.singletonMap(
                        AppPresentationSettingKeys.LANGUAGE.getName(),
                        language));
    }

    private static Collection<SettingKey<?>> createPortableSettings() {
        ArrayList<SettingKey<?>> keys = new ArrayList<>();
        for (SettingKey<?> key : SettingsKeyCatalog.all()) {
            // SAF directory access is an OS permission, not portable app
            // configuration. Importing the URI without its grant would leave
            // a broken directory selection on the destination device.
            if (!TransferSettingKeys.CLIPBOARD_FILE_DIRECTORY_URI
                    .getName()
                    .equals(key.getName())) {
                keys.add(key);
            }
        }
        return Collections.unmodifiableList(keys);
    }

    private File uniqueArchiveFile() throws IOException {
        String timestamp = new SimpleDateFormat(
                "yyyyMMdd-HHmmss",
                Locale.ROOT)
                .format(new Date());
        File file = new File(
                requireWorkRoot(),
                "Moonlight-configuration-" + timestamp + ".zip");
        if (!file.exists()) {
            return file;
        }
        return new File(
                requireWorkRoot(),
                "Moonlight-configuration-" + timestamp + "-" +
                        UUID.randomUUID().toString().substring(0, 8) +
                        ".zip");
    }

    private File createWorkDirectory(String prefix) throws IOException {
        File directory = new File(
                requireWorkRoot(),
                prefix + "-" + UUID.randomUUID());
        if (!directory.mkdir()) {
            throw new IOException(
                    "Unable to create configuration work directory");
        }
        return directory;
    }

    private File requireWorkRoot() throws IOException {
        File root = new File(context.getCacheDir(), WORK_DIRECTORY);
        if (!root.isDirectory() && !root.mkdirs()) {
            throw new IOException(
                    "Unable to create configuration archive directory");
        }
        return root;
    }

    private InputStream openInput(Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException(
                    "Document provider returned no readable stream");
        }
        return new BufferedInputStream(input);
    }

    private byte[] readOwnedFileIfPresent(String name) throws IOException {
        File file = new File(context.getFilesDir(), name);
        return file.isFile()
                ? readFile(file, MAXIMUM_IDENTITY_FILE_BYTES)
                : null;
    }

    private static byte[] readFile(File file, long maximumBytes)
            throws IOException {
        if (!file.isFile()) {
            throw new IOException("Missing file: " + file.getName());
        }
        try (InputStream input = new BufferedInputStream(
                new FileInputStream(file));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output, maximumBytes);
            return output.toByteArray();
        }
    }

    private static byte[] readZipEntry(
            ZipFile zip,
            ZipEntry entry,
            long maximumBytes)
            throws IOException {
        try (InputStream input = new BufferedInputStream(
                zip.getInputStream(entry));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output, maximumBytes);
            return output.toByteArray();
        }
    }

    private static long maximumBytesFor(String name) throws IOException {
        switch (name) {
            case SETTINGS_ENTRY_NAME:
                return MAXIMUM_SETTINGS_BYTES;
            case HOSTS_ENTRY_NAME:
                return MAXIMUM_HOST_DATABASE_BYTES;
            case CERTIFICATE_FILE_NAME:
            case PRIVATE_KEY_FILE_NAME:
                return MAXIMUM_IDENTITY_FILE_BYTES;
            default:
                throw new IOException(
                        "Unsupported configuration archive entry: " + name);
        }
    }

    private static void copy(
            InputStream input,
            OutputStream output,
            long maximumBytes)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Configuration operation canceled");
            }
            total += count;
            if (total > maximumBytes) {
                throw new IOException(
                        "Configuration data exceeds the allowed size");
            }
            output.write(buffer, 0, count);
        }
    }

    private static long copyAndDigest(
            InputStream input,
            OutputStream output,
            MessageDigest digest,
            long maximumBytes)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Configuration operation canceled");
            }
            total += count;
            if (total > maximumBytes) {
                throw new IOException(
                        "Configuration data exceeds the allowed size");
            }
            digest.update(buffer, 0, count);
            output.write(buffer, 0, count);
        }
        return total;
    }

    private static long digest(
            InputStream input,
            MessageDigest digest,
            long maximumBytes)
            throws IOException {
        return copyAndDigest(
                input,
                NullOutputStream.INSTANCE,
                digest,
                maximumBytes);
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = sha256Digest();
        try (InputStream input = new BufferedInputStream(
                new FileInputStream(file))) {
            digest(input, digest, maximumBytesFor(file.getName()));
        }
        return hex(digest.digest());
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-256 is unavailable", error);
        }
    }

    private static String hex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte octet : value) {
            builder.append(String.format(
                    Locale.ROOT,
                    "%02x",
                    octet & 0xff));
        }
        return builder.toString();
    }

    private static boolean isRootEntryName(String name) {
        return name != null &&
                !name.isEmpty() &&
                !name.contains("/") &&
                !name.contains("\\") &&
                !name.equals(".") &&
                !name.equals("..");
    }

    private static Map<String, File> singletonFiles(
            String name,
            File file) {
        LinkedHashMap<String, File> files = new LinkedHashMap<>();
        files.put(name, file);
        return files;
    }

    private static void closeDatabase(ComputerDatabaseManager manager) {
        if (manager != null) {
            try {
                manager.close();
            }
            catch (RuntimeException error) {
                LimeLog.warning(
                        "Unable to close configuration database: " +
                                error.getMessage());
            }
        }
    }

    static void deleteRecursively(File target) {
        if (target == null) {
            return;
        }
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (target.exists() && !target.delete()) {
            LimeLog.warning(
                    "Unable to remove configuration work file: " +
                            target.getName());
        }
    }

    static final class PreparedImport implements AutoCloseable {
        private final File archive;
        private final ConfigurationArchiveManifest manifest;
        private boolean closed;

        private PreparedImport(
                File archive,
                ConfigurationArchiveManifest manifest) {
            this.archive = archive;
            this.manifest = manifest;
        }

        Set<ConfigurationArchiveComponent> getComponents() {
            ensureOpen();
            return manifest.getComponents();
        }

        void ensureOpen() {
            if (closed) {
                throw new IllegalStateException(
                        "Prepared configuration import is closed");
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (archive.exists() && !archive.delete()) {
                LimeLog.warning(
                        "Unable to remove prepared configuration archive");
            }
        }
    }

    static final class ImportResult {
        private final Set<ConfigurationArchiveComponent> imported;
        private final String importedLanguage;

        private ImportResult(
                Set<ConfigurationArchiveComponent> imported,
                String importedLanguage) {
            this.imported = Collections.unmodifiableSet(
                    EnumSet.copyOf(imported));
            this.importedLanguage = importedLanguage;
        }

        Set<ConfigurationArchiveComponent> getImported() {
            return imported;
        }

        String getImportedLanguage() {
            return importedLanguage;
        }
    }

    private static final class PairingImport implements AutoCloseable {
        private final ComputerDatabaseManager database;
        private final File certificate;
        private final File privateKey;

        private PairingImport(
                ComputerDatabaseManager database,
                File certificate,
                File privateKey) {
            this.database = database;
            this.certificate = certificate;
            this.privateKey = privateKey;
        }

        @Override
        public void close() {
            closeDatabase(database);
        }
    }

    private static final class PairingRollback {
        private final File hosts;
        private final byte[] certificate;
        private final byte[] privateKey;

        private PairingRollback(
                File hosts,
                byte[] certificate,
                byte[] privateKey) {
            this.hosts = hosts;
            this.certificate = certificate;
            this.privateKey = privateKey;
        }
    }

    private static final class OverrideSettingsRepository
            implements SettingsRepository {
        private final SettingsRepository delegate;
        private final Map<String, Object> overrides;

        private OverrideSettingsRepository(
                SettingsRepository delegate,
                Map<String, Object> overrides) {
            this.delegate = delegate;
            this.overrides = new HashMap<>(overrides);
        }

        @Override
        public boolean contains(SettingKey<?> key) {
            return overrides.containsKey(key.getName()) ||
                    delegate.contains(key);
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            if (overrides.containsKey(key.getName())) {
                return key.normalizeStoredValue(
                        overrides.get(key.getName()));
            }
            return delegate.get(key);
        }

        @Override
        public Editor edit() {
            throw new UnsupportedOperationException(
                    "Export settings repository is read-only");
        }
    }

    private static final class NonClosingOutputStream extends OutputStream {
        private final OutputStream delegate;

        private NonClosingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
        }

        @Override
        public void write(byte[] value, int offset, int length)
                throws IOException {
            delegate.write(value, offset, length);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }
    }

    private static final class NullOutputStream extends OutputStream {
        private static final NullOutputStream INSTANCE =
                new NullOutputStream();

        @Override
        public void write(int value) {
        }

        @Override
        public void write(byte[] value, int offset, int length) {
        }
    }
}
