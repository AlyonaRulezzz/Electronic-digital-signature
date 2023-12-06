package com.example.test_electronical_digit_signature

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.test_electronical_digit_signature.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.X509CertificateObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.File
import java.io.FileReader
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1

    private val PASSWORD = "1"

    private lateinit var binding: ActivityMainBinding
    private var signtureFileName = "noDocument_signature"
    private var filePath = ""
    private var isSigningError = false
    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.removeProvider(provider.name)
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE)
        }

        binding.btnFilePicker.setOnClickListener {
            initViews()
            showFileChooser()
        }

        binding.btnSign.setOnClickListener {
            signCheck()
        }

        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            isSigningError = text != PASSWORD
        }
    }

    private fun signCheck() {
        if (privateKey != null && publicKey != null) {
            if ((filePath != "") && (binding.etPassword.text.toString() == PASSWORD)) {
                isSigningError = false
                afterShowFileChooser()
            } else {
                isSigningError = true
            }

            if (isSigningError) {
                binding.tvSignStatus.text = getString(R.string.signing_error)
            } else {
                binding.tvSignStatus.text = getString(R.string.success_signing)
            }
        } else {
            binding.tvSignStatus.text = getString(R.string.signing_error)
        }
    }

    private fun initViews() {
        filePath = ""
        isSigningError = false
        binding.tvFileInfo.text = ""
        binding.etPassword.setText("")
        binding.tvSignStatus.text = ""
    }

    //
    private fun afterShowFileChooser() {
        privateKey?.let{
            publicKey?.let {
                val signature = createSignature(filePath, privateKey!!)
                createFile(this@MainActivity, signtureFileName, signature)
            }
        }
    }

    private fun showFileChooser(): String {
        binding.tvPublicKeyStatus.text = ""
        binding.tvPrivateKeyStatus.text = ""

//        searchPrivateKey()
//        searchPublicKey()
        generateKeys()

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100)
        } catch (exception: Exception) {
            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show()
        }
        return filePath
    }

    private fun generateKeys() {
        CoroutineScope(Dispatchers.IO).launch {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
            keyPairGenerator.initialize(2048)
            val keyPair: KeyPair = keyPairGenerator.generateKeyPair()
            privateKey = keyPair.private
            publicKey = keyPair.public
        }
    }

    private fun searchPrivateKey(){
        CoroutineScope(Dispatchers.IO).launch {
            val directoryPath = Environment.getExternalStorageDirectory().path
            val directory = File(directoryPath)
            val extensionToFind = "pem"
            val pemFiles = findFilesByExtension(directory, extensionToFind)
            if (pemFiles.isNotEmpty()) {
                for (pemFile in pemFiles) {
                    println(pemFile.absolutePath)
                }
                val pemFilePath = pemFiles[0].absolutePath
                privateKey = getPrivateKeyFromPEMFile(pemFilePath)
            } else {
                withContext(Dispatchers.Main) {
                    binding.tvPrivateKeyStatus.text = this@MainActivity.getString(R.string.no_pivate_key)
                }
            }
        }
    }

    private fun searchPublicKey() {
        CoroutineScope(Dispatchers.IO).launch {
            val directoryPath = Environment.getExternalStorageDirectory().path
            val extensionToFind = "cer"
            val directory = File(directoryPath)
            val cerFiles = findFilesByExtension(directory, extensionToFind)
            if (cerFiles.isNotEmpty()) {
                for (cerFile in cerFiles) {
                    println(cerFile.absolutePath)
                }
                val certificateFilePath = cerFiles[0].absolutePath
                publicKey = getPublicKeyFromCertificateFile(certificateFilePath)
                val isSignatureValid = publicKey?.let { verifyKeys("Hello world!", it) }
                if (isSignatureValid == false) {
                    withContext(Dispatchers.Main) {
                        binding.tvPublicKeyStatus.text =
                            this@MainActivity.getText(R.string.no_public_key)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.tvPublicKeyStatus.text =
                        this@MainActivity.getText(R.string.no_public_key)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        CoroutineScope(Dispatchers.IO).launch {
            if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
                val uri: Uri? = data.data
                val path: String = uri?.path.toString()
                filePath = path
                val file = File(path)
                signtureFileName = file.name
                withContext(Dispatchers.Main) {
                    binding.tvFileInfo.text = "Документ = ${file.name}, путь = $path".trimIndent()
                }
            }
        }
    }

        private fun createSignature(filePath: String, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA", "BC")
            CoroutineScope(Dispatchers.IO).launch {
                signature.initSign(privateKey)
                try {
                    val hashByteArray = File(filePath).hashCode().toString().toByteArray()
                    signature.update(hashByteArray)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, getString(R.string.hashcode_error),
                            Toast.LENGTH_SHORT).show()
                    }
                    isSigningError = true
                }
            }
            return signature.sign()
        }
//
        private fun verifySignature(filePath: String, publicKey: PublicKey, signature: ByteArray): Boolean {
            val verifier = Signature.getInstance("SHA256withRSA", "BC")
            verifier.initVerify(publicKey)

            val fileBytes = File(filePath).hashCode().toString().toByteArray()
            verifier.update(fileBytes)

            return verifier.verify(signature)
        }

    private fun verifyKeys(string: String, publicKey: PublicKey): Boolean {
         privateKey?.let{
            val signature = Signature.getInstance("SHA256withRSA", "BC")
            signature.initSign(privateKey)
            val stringBytes = string.toByteArray()
            signature.update(stringBytes)
            signature.sign()

            val verifier = Signature.getInstance("SHA256withRSA", "BC")
            verifier.initVerify(publicKey)
            verifier.update(stringBytes)
            return verifier.verify(signature.sign())
        }
        return false
    }

    private fun createFile(context: Context, fileName: String, signature: ByteArray) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${signtureFileName.substringBeforeLast('.')}.sig")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        uri?.let {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(signature)
            }
        }
    }

    private fun getPublicKeyFromCertificateFile(certificateFilePath: String): PublicKey? {
        return try {
            val pemReader = PemReader(FileReader(certificateFilePath))
            val certHolder = pemReader.readPemObject() as X509CertificateObject

            val certFactory = CertificateFactory.getInstance("X.509")
            val x509Certificate = certFactory.generateCertificate(certHolder.encoded.inputStream()) as X509Certificate

            x509Certificate.publicKey
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getPrivateKeyFromPEMFile(pemFilePath: String): PrivateKey? {
        return try {
            val pemReader = PemReader(FileReader(pemFilePath))
            val pemObject = pemReader.readPemObject()

            val privateKeyBytes = pemObject.content
            val keyFactory: KeyFactory = KeyFactory.getInstance("RSA", BouncyCastleProvider())
            val privateKeySpec: EncodedKeySpec = X509EncodedKeySpec(privateKeyBytes)
            keyFactory.generatePrivate(privateKeySpec)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findFilesByExtension(directory: File, extension: String): List<File> {
        val result = mutableListOf<File>()

        if (directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        result.addAll(findFilesByExtension(file, extension))
//                        result.addAll(findFilesByExtension(file, file.name.substringAfterLast('.')))
                    } else {
                        // Проверяем расширение файла
                        if (file.extension.equals(extension, ignoreCase = true)) {
//                        if (file.name.substringAfterLast('.').equals(extension, ignoreCase = true)) {
                            result.add(file)
                        }
                    }
                }
            }
        }
        return result
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.tvSignStatus.text = getString(R.string.no_access_to_storage)
                }
            }
        }
    }
}