import java.io.*;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

// Interface for FileOperation decorators
interface FileOperation {
    void operate(File file) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException;
}

// Concrete decorator for compression
class ZipCompression implements FileOperation {
    private FileOperation fileOperation;

    public ZipCompression(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }

    @Override
    public void operate(File file) throws IOException {
        String filename = getFileNameWithoutExtension(file.getName());
        FileOutputStream fos = new FileOutputStream(filename + ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        FileInputStream fis = new FileInputStream(file);
        zipOut.putNextEntry(new ZipEntry(file.getName()));

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }

        fis.close();
        zipOut.close();
        fos.close();
    }

    private String getFileNameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
}

// Concrete decorator for encryption
class DESEncryption implements FileOperation {
    private FileOperation fileOperation;

    public DESEncryption(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }

    @Override
    public void operate(File file) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        String filename = getFileNameWithoutExtension(file.getName());
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(filename + ".des");

        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey secretKey = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        CipherOutputStream cos = new CipherOutputStream(fos, cipher);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            cos.write(bytes, 0, length);
        }

        fis.close();
        cos.close();
        fos.close();
    }

    private String getFileNameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
}

// Concrete decorator for checksum
class MD5Checksum implements FileOperation {
    private FileOperation fileOperation;

    public MD5Checksum(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }

    @Override
    public void operate(File file) throws IOException, NoSuchAlgorithmException {
        String filename = getFileNameWithoutExtension(file.getName());
        FileInputStream fis = new FileInputStream(file);
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }

        byte[] mdbytes = md.digest();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        System.out.println("MD5 Checksum: " + sb.toString());

        fis.close();
    }

    private String getFileNameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
}

public class jcompress {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java JCompress <filename> <operations>");
            return;
        }

        String filename = args[0];
        File file = new File(filename);

        // Chain the decorators based on the provided operations
        FileOperation fileOperation = new FileOperation() {
            @Override
            public void operate(File file) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
                // No operation
            }
        };

        for (int i = 1; i < args.length; i++) {
            
            switch (args[i]) {
                case "-zip":
                    fileOperation = new ZipCompression(fileOperation);
                    break;
                case "-DES":
                    fileOperation = new DESEncryption(fileOperation);
                    break;
                case "-MD5":
                    fileOperation = new MD5Checksum(fileOperation);
                    break;
                default:
                    System.out.println("Unknown operation: " + args[i]);
            }
            try {
                fileOperation.operate(file);
            } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }


    }
}
