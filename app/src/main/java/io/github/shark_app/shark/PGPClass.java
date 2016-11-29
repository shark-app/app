package io.github.shark_app.shark;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.spongycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Created by Divay Prakash on 30-Nov-16.
 */

public class PGPClass {
    public static PGPPublicKey getPublicKeyFromString(String str) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(str.getBytes());
        inputStream = org.spongycastle.openpgp.PGPUtil.getDecoderStream(inputStream);
        JcaPGPPublicKeyRingCollection jcaPGPPublicKeyRingCollection = new JcaPGPPublicKeyRingCollection(inputStream);
        inputStream.close();
        PGPPublicKey pgpPublicKey = null;
        Iterator<PGPPublicKeyRing> iterator = jcaPGPPublicKeyRingCollection.getKeyRings();
        while (pgpPublicKey == null && iterator.hasNext()) {
            PGPPublicKeyRing kRing = iterator.next();
            Iterator<PGPPublicKey> iteratorKey = kRing.getPublicKeys();
            while (pgpPublicKey == null && iteratorKey.hasNext()) {
                pgpPublicKey = iteratorKey.next();
            }
        }
        return pgpPublicKey;
    }

    public static PGPSecretKey getSecretKeyFromString(String str) throws Exception{
        InputStream inputStream = new ByteArrayInputStream(str.getBytes());
        inputStream = org.spongycastle.openpgp.PGPUtil.getDecoderStream(inputStream);
        JcaPGPSecretKeyRingCollection jcaPGPSecretKeyRingCollection = new JcaPGPSecretKeyRingCollection(inputStream);
        inputStream.close();
        PGPSecretKey pgpSecretKey = null;
        Iterator<PGPSecretKeyRing> iterator = jcaPGPSecretKeyRingCollection.getKeyRings();
        while (pgpSecretKey == null && iterator.hasNext()) {
            PGPSecretKeyRing kRing = iterator.next();
            Iterator<PGPSecretKey> iteratorKey = kRing.getSecretKeys();
            while (pgpSecretKey == null && iteratorKey.hasNext()) {
                pgpSecretKey = iteratorKey.next();
            }
        }
        return pgpSecretKey;
    }

    public static byte[] signPublicKey(PGPSecretKey secretKey, String secretKeyPass, PGPPublicKey keyToBeSigned, String notationName, String notationValue, boolean armor) throws Exception {
        OutputStream outputStream = new ByteArrayOutputStream();
        if (armor) {
            outputStream = new ArmoredOutputStream(outputStream);
        }
        PGPPrivateKey pgpPrivateKey = secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("SC").build(secretKeyPass.toCharArray()));
        PGPSignatureGenerator pgpSignatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA1).setProvider("SC"));
        pgpSignatureGenerator.init(PGPSignature.DIRECT_KEY, pgpPrivateKey);
        BCPGOutputStream bcpgOutputStream = new BCPGOutputStream(outputStream);
        pgpSignatureGenerator.generateOnePassVersion(false).encode(bcpgOutputStream);
        PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
        pgpSignatureSubpacketGenerator.setNotationData(true, true, notationName, notationValue);
        PGPSignatureSubpacketVector packetVector = pgpSignatureSubpacketGenerator.generate();
        pgpSignatureGenerator.setHashedSubpackets(packetVector);
        bcpgOutputStream.flush();
        if (armor) {
            outputStream.close();
        }
        return PGPPublicKey.addCertification(keyToBeSigned, pgpSignatureGenerator.generate()).getEncoded();
    }
}
