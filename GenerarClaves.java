import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class GenerarClaves {
    public static void main(String[] args) throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        String privada = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n";

        String publica = "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n";

        Files.writeString(
            Path.of("privateKey.pem"),
            privada,
            StandardCharsets.US_ASCII
        );

        Files.writeString(
            Path.of("publicKey.pem"),
            publica,
            StandardCharsets.US_ASCII
        );

        System.out.println("Claves RSA generadas correctamente.");
    }
}
