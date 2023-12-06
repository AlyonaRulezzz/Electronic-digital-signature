package com.example.test_electronical_digit_signature

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import com.example.test_electronical_digit_signature.databinding.ActivityMainBinding
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.X509CertificateObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.File
import java.io.FileReader
import java.security.KeyFactory
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

    private val TAG = "MainActivity1"
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

        binding.btnFilePicker.setOnClickListener {
            initViews()
            showFileChooser()
        }

        binding.btnSign.setOnClickListener {
            if (privateKey != null && publicKey != null) {
                if ((filePath != "") && (binding.etPassword.text.toString() == PASSWORD)) {
                    isSigningError = false
                    afterShowFileChooser()
                } else {
                    isSigningError = true
                }

                if (isSigningError) {
                    binding.tvSignStatus.text = "Ошибка подписания документа"
                } else {
                    binding.tvSignStatus.text = "Подписание документа завершено успешно"
                }
            }
        }

        binding.etPassword.doOnTextChanged { text, start, before, count ->
            isSigningError = text != PASSWORD
        }

//
        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.removeProvider(provider.name)
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            println(
                "checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED = " +
                        "${checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED}"
            )
            // Запрашиваем разрешение у пользователя
            requestPermissions(
                arrayOf(READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE)
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
        // Создаем подпись
        privateKey?.let{
            publicKey?.let {
                val signature = createSignature(filePath, privateKey!!)
                println("signature1 = ${signature.contentToString()}")
                // Сохраняем подпись в файл
                createFile(this@MainActivity, signtureFileName, signature)
            }
        }
    }

    private fun showFileChooser(): String {
        binding.tvPublicKeyStatus.text = ""
        binding.tvPrivateKeyStatus.text = ""

        searchPrivateKey()
        searchPublicKey()

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

    private fun searchPrivateKey(){
        // Поиск приватного ключа
        val directoryPath = Environment.getExternalStorageDirectory().path
        val directory = File(directoryPath)
        val extensionToFind = "pem"
        val pemFiles = findFilesByExtension(directory, extensionToFind)
        if (pemFiles.isNotEmpty()) {
            println("Найденные файлы с расширением $extensionToFind:")
            for (pemFile in pemFiles) {
                println(pemFile.absolutePath)
            }
            val pemFilePath = pemFiles[0].absolutePath
            privateKey = getPrivateKeyFromPEMFile(pemFilePath)
            println("Закрытый ключ: $privateKey")
        } else {
            println("Файлы с расширением $extensionToFind не найдены.")
            println("Не удалось извлечь закрытый ключ.")
            binding.tvPrivateKeyStatus.text = this.getString(R.string.no_pivate_key)
        }
    }

    private fun searchPublicKey() {
        // Путь к корневой директории, где вы хотите начать поиск публичного ключа
        val directoryPath = Environment.getExternalStorageDirectory().path
        val extensionToFind = "cer"
        val directory = File(directoryPath)
        val cerFiles = findFilesByExtension(directory, extensionToFind)
        if (cerFiles.isNotEmpty()) {
            println("Найденные файлы с расширением $extensionToFind:")
            for (cerFile in cerFiles) {
                println(cerFile.absolutePath)
            }
            val certificateFilePath = cerFiles[0].absolutePath
            publicKey = getPublicKeyFromCertificateFile(certificateFilePath)
            // Проверяем ключи
            val isSignatureValid = publicKey?.let { verifyKeys("Hello world!", it) }
            if (isSignatureValid == true) {
                println("Подпись верна.")
                println("Открытый ключ: $publicKey")
            } else {
                println("Подпись не верна.")
                println("Не удалось извлечь открытый ключ.")
                binding.tvPublicKeyStatus.text = this.getText(R.string.no_public_key)
            }
        } else {
            println("Файлы с расширением $extensionToFind не найдены.")
            println("Не удалось извлечь открытый ключ.")
            binding.tvPublicKeyStatus.text = this.getText(R.string.no_public_key)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            val path: String = uri?.path.toString()
            filePath = path
            val file = File(path)
            signtureFileName = file.name
            binding.tvFileInfo.text = "Документ = ${file.name}, путь = $path".trimIndent()
        }
    }

        private fun createSignature(filePath: String, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA", "BC")
            signature.initSign(privateKey)
            try {
                val hashByteArray = File(filePath).hashCode().toString().toByteArray()

                // Теперь у вас есть массив байтов, представляющих хэш файла
                println("Хэш файла в виде массива байтов: ${hashByteArray.contentToString()}")
//                println("Хэш файла в виде массива байтов...")
                signature.update(hashByteArray)
            } catch (e: Exception) {
                println("Ошибка  кодирования хэш-кода: ${e.message}")
                Toast.makeText(this, "Ошибка кодирования хэш-кода", Toast.LENGTH_SHORT).show()
                isSigningError = true
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
//            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${signtureFileName.substringBeforeLast('.')}.sig")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        uri?.let {
            resolver.openOutputStream(uri)?.use { outputStream ->
                // Напишите данные в поток
//                outputStream.write("Привет, мир!".toByteArray())
                outputStream.write(signature)
            }

            println("файл успешно создан: ${uri.path}")
        }
    }
    //
    private fun getPublicKeyFromCertificateFile(certificateFilePath: String): PublicKey? {
        try {
            val pemReader = PemReader(FileReader(certificateFilePath))
            val certHolder = pemReader.readPemObject() as X509CertificateObject

            // Convert X509CertificateHolder to X509Certificate
            val certFactory = CertificateFactory.getInstance("X.509")
            val x509Certificate = certFactory.generateCertificate(certHolder.encoded.inputStream()) as X509Certificate

            // Extract public key from X509Certificate
            return x509Certificate.publicKey
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getPrivateKeyFromPEMFile(pemFilePath: String): PrivateKey? {
        try {
            val pemReader = PemReader(FileReader(pemFilePath))
            val pemObject = pemReader.readPemObject()

            // Convert PEM object to PrivateKey
            val privateKeyBytes = pemObject.content
            // Create a PrivateKey object using the decoded private key
            val keyFactory: KeyFactory = KeyFactory.getInstance("RSA", BouncyCastleProvider())
            val privateKeySpec: EncodedKeySpec = X509EncodedKeySpec(privateKeyBytes)
            return keyFactory.generatePrivate(privateKeySpec)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun findFilesByExtension(directory: File, extension: String): List<File> {
        val result = mutableListOf<File>()

        if (directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        // Рекурсивно вызываем функцию для поддиректории
                        result.addAll(findFilesByExtension(file, extension))
//                        result.addAll(findFilesByExtension(file, file.name.substringAfterLast('.')))
//                        println(file.name)
                    } else {
                        // Проверяем расширение файла
//                        if (file.extension.equals(extension, ignoreCase = true)) {
//                        if (file.name.substringAfterLast('.').equals(extension, ignoreCase = true)) {
//                        if (file.name.contains(".txt", ignoreCase = true)) {
                            result.add(file)
//                            println(file.name)
//                        }
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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("Разрешение получено")
                } else {
                    println("Разрешение не получено")
                    binding.tvSignStatus.text = "Для продолжения работы необходим доступ к файлам на устройстве."
                }
            }
        }
    }
}