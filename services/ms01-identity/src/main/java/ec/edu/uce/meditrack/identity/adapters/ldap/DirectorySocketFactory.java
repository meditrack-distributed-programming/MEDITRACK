package ec.edu.uce.meditrack.identity.adapters.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * TLS sockets that trust the certificate authority of the directory and nothing else.
 *
 * <p>The Java directory API only accepts the name of a socket factory class and calls its static
 * {@link #getDefault()} method. For that reason the trusted authority is held in a static field; the
 * service talks to a single directory. The class has to be public for the same reason.
 *
 * <p>The host name of the server certificate is still verified by the directory API.
 */
public final class DirectorySocketFactory extends SSLSocketFactory {

    private static volatile SSLSocketFactory trusted =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

    private final SSLSocketFactory delegate;

    private DirectorySocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    /** Called by the Java directory API. */
    public static SocketFactory getDefault() {
        return new DirectorySocketFactory(trusted);
    }

    /**
     * Selects the trusted certificate authority.
     *
     * @param caCertificate PEM file; an empty value keeps the public authorities known by Java
     */
    static void trust(String caCertificate) {
        trusted = caCertificate.isBlank()
                ? (SSLSocketFactory) SSLSocketFactory.getDefault()
                : socketFactoryTrusting(Path.of(caCertificate));
    }

    private static SSLSocketFactory socketFactoryTrusting(Path pemFile) {
        try (InputStream pem = Files.newInputStream(pemFile)) {
            KeyStore authorities = KeyStore.getInstance(KeyStore.getDefaultType());
            authorities.load(null, null);
            int index = 0;
            for (Certificate certificate :
                    CertificateFactory.getInstance("X.509").generateCertificates(pem)) {
                authorities.setCertificateEntry("directory-ca-" + index++, certificate);
            }
            if (index == 0) {
                throw new IllegalStateException("No certificate found in " + pemFile);
            }
            TrustManagerFactory trustManagers =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagers.init(authorities);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Cannot load the directory certificate authority from " + pemFile, e);
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    // The directory API creates an unconnected socket to apply the connection timeout
    @Override
    public Socket createSocket() throws IOException {
        return delegate.createSocket();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException {
        return delegate.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        return delegate.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    @Override
    public Socket createSocket(
            InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return delegate.createSocket(address, port, localAddress, localPort);
    }
}
