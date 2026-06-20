package com.exemplo.fluidez.system

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.PrivateKeyUsageExtension
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo

/** Gerencia as chaves/certificado e a conexão com o adbd local. */
class AdbConnectionManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val privKey: PrivateKey
    private val cert: Certificate

    init {
        api = Build.VERSION.SDK_INT
        setHostAddress("127.0.0.1")

        val keyFile = File(context.filesDir, "adb_key")
        val certFile = File(context.filesDir, "adb_cert")
        if (keyFile.exists() && certFile.exists()) {
            privKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
            cert = certFile.inputStream().use {
                CertificateFactory.getInstance("X.509").generateCertificate(it)
            }
        } else {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
            val kp: KeyPair = kpg.generateKeyPair()
            privKey = kp.private
            cert = generateCertificate(kp.public, privKey)
            keyFile.writeBytes(privKey.encoded)
            certFile.writeBytes(cert.encoded)
        }
    }

    private fun generateCertificate(publicKey: PublicKey, signWith: PrivateKey): Certificate {
        val algorithmName = "SHA512withRSA"
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000)

        val ext = CertificateExtensions()
        ext.set(
            "SubjectKeyIdentifier",
            SubjectKeyIdentifierExtension(KeyIdentifier(publicKey).identifier)
        )
        ext.set("PrivateKeyUsage", PrivateKeyUsageExtension(notBefore, notAfter))

        val x500 = X500Name("CN=Fluidez")
        val info = X509CertInfo()
        info.set("version", CertificateVersion(2))
        info.set("serialNumber", CertificateSerialNumber(Random().nextInt() and Int.MAX_VALUE))
        info.set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithmName)))
        info.set("subject", CertificateSubjectName(x500))
        info.set("key", CertificateX509Key(publicKey))
        info.set("validity", CertificateValidity(notBefore, notAfter))
        info.set("issuer", CertificateIssuerName(x500))
        info.set("extensions", ext)

        val certImpl = X509CertImpl(info)
        certImpl.sign(signWith, algorithmName)
        return certImpl
    }

    override fun getPrivateKey(): PrivateKey = privKey
    override fun getCertificate(): Certificate = cert
    override fun getDeviceName(): String = "Fluidez"

    companion object {
        @Volatile
        private var INSTANCE: AdbConnectionManager? = null

        fun getInstance(context: Context): AdbConnectionManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdbConnectionManager(context.applicationContext).also { INSTANCE = it }
            }

        /** Retorna a instância já criada, sem forçar a criação (e a geração de chaves). */
        fun peek(): AdbConnectionManager? = INSTANCE
    }
}
