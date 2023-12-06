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
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.example.test_electronical_digit_signature.databinding.ActivityMainBinding
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

    private val TAG = "MainActivity1"
    private val PASSWORD = "1"

    private lateinit var binding: ActivityMainBinding
    private var signtureFileName = "noDocument_signature"
    private var filePath = ""
    private var isSigningError = false
    private lateinit var privateKey: PrivateKey
    private lateinit var publicKey: PublicKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnFilePicker.setOnClickListener {
            filePath = ""
            isSigningError = false
            binding.tvFileInfo.text = ""
            binding.etPassword.setText("")
            binding.tvSignStatus.text = ""
            showFileChooser()
        }

        binding.btnSign.setOnClickListener {
            if ( (filePath != "") && (binding.etPassword.text.toString() == PASSWORD) ) {
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

        binding.etPassword.doOnTextChanged { text, start, before, count ->
            isSigningError = text != PASSWORD
        }

//
        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 0)
//
            // Создаем генератор ключевой пары с использованием алгоритма, например, RSA
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
            keyPairGenerator.initialize(2048) // Укажите требуемую длину ключа

            // Генерируем ключевую пару
            val keyPair: KeyPair = keyPairGenerator.generateKeyPair()

            // Получаем приватный и публичный ключи
            privateKey = keyPair.private
            publicKey= keyPair.public
            println("Public key: $publicKey")
            println("Private key: $privateKey")

//
            val certificateFilePath = "путь_к_вашему_файлу.cer"
            val publicKey = getPublicKeyFromCertificateFile(certificateFilePath)

            if (publicKey != null) {
                println("Открытый ключ: $publicKey")
            } else {
                println("Не удалось извлечь открытый ключ.")
            }

//            val pemFilePath = "путь_к_вашему_файлу.key"
            val pemFilePath = "путь_к_вашему_файлу.pem"
            val privateKey = getPrivateKeyFromPEMFile(pemFilePath)

            if (privateKey != null) {
                println("Приватный ключ: $privateKey")
            } else {
                println("Не удалось извлечь приватный ключ.")
            }
//
//        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            // У вас уже есть разрешение, выполняйте операции чтения файлов
//            val directory = Environment.getExternalStorageDirectory()
//            val allFiles = findFilesByExtension(directory, extension = "txt")
//
//            if (allFiles.isNotEmpty()) {
//                println("Найденные файлы:")
//                for (file in allFiles) {
//                    println(file.absolutePath)
//                }
//            } else {
//                println("Файлы не найдены.")
//            }
//        } else {
            // Запрашиваем разрешение у пользователя
            requestPermissions(arrayOf(READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_READ_EXTERNAL_STORAGE)
//        }

//        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                println("Доступ к внешнему хранилищу и разрешение на доступ к нему есть")
//            } else {
//                println("Разрешение на доступ к внешнему хранилищу отсутствует")
//            }
//        } else {
//            println("Доступа к внешнему хранилищу нет")
//        }
//        // Путь к корневой директории, где вы хотите начать поиск
////        val directoryPath = Environment.getRootDirectory().path  // находит
////        val directoryPath = getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)
////            val directoryPath = this.getExternalFilesDir(null)
////        val directoryPath = Environment.getExternalStorageDirectory().path
//            val directoryPath = Environment.DIRECTORY_DOWNLOADS
//
//            val extensionToFind = "txt"
//
//            val directory = File(directoryPath)
////            val directory = this.getExternalFilesDir(null)!!
////            val directory = getExternalFilesDirs(null)[0]
//            val pemFiles = findFilesByExtension(directory, extensionToFind)
//
//            if (pemFiles.isNotEmpty()) {
//                println("Найденные файлы с расширением $extensionToFind:")
//                for (pemFile in pemFiles) {
//                    println(pemFile.absolutePath)
//                }
//            } else {
//                println("Файлы с расширением $extensionToFind не найдены.")
//            }
        }
//
    private fun afterShowFileChooser() {
        // Создаем подпись
        val signature = createSignature(filePath, privateKey)
        println("signature1 = ${signature.contentToString()}")
        // Сохраняем подпись в файл
        createFile(this@MainActivity, signtureFileName, signature)

        // Проверяем подпись
        val isSignatureValid = verifySignature(filePath, publicKey, signature)
        if (isSignatureValid) {
            println("Подпись верна.")
        } else {
            println("Подпись не верна.")
        }
    }

    private fun showFileChooser(): String {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                println("Доступ к внешнему хранилищу и разрешение на доступ к нему есть")
            } else {
                println("Разрешение на доступ к внешнему хранилищу отсутствует")
            }
        } else {
            println("Доступа к внешнему хранилищу нет")
        }
        // Путь к корневой директории, где вы хотите начать поиск
//        val directoryPath = Environment.getRootDirectory().path  // находит
//        val directoryPath = getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)
//            val directoryPath = this.getExternalFilesDir(null)
//        val directoryPath = Environment.getExternalStorageDirectory().path
        val directoryPath = Environment.DIRECTORY_DOWNLOADS

        val extensionToFind = "txt"

//        val directory = File(directoryPath)
//            val directory = this.getExternalFilesDir(null)!!
            val directory = getExternalFilesDirs(null)[0]
        val pemFiles = findFilesByExtension(directory, extensionToFind)

        if (pemFiles.isNotEmpty()) {
            println("Найденные файлы с расширением $extensionToFind:")
            for (pemFile in pemFiles) {
                println(pemFile.absolutePath)
            }
        } else {
            println("Файлы с расширением $extensionToFind не найдены.")
        }
        //
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            val path: String = uri?.path.toString()
            filePath = path
            val file = File(path)
            signtureFileName = file.name
            binding.tvFileInfo.text = "fileName = ${file.name}, path = $path".trimIndent()
        }
    }

    //
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
        fun verifySignature(filePath: String, publicKey: PublicKey, signature: ByteArray): Boolean {
            val verifier = Signature.getInstance("SHA256withRSA", "BC")
            verifier.initVerify(publicKey)

            val fileBytes = File(filePath).hashCode().toString().toByteArray()
            verifier.update(fileBytes)

            return verifier.verify(signature)
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
        }
        return null
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
            val privateKey: PrivateKey = keyFactory.generatePrivate(privateKeySpec)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // Разрешение получено, выполняйте операции чтения файлов
//                    val directory = Environment.getExternalStorageDirectory()
//                    val allFiles = findFilesByExtension(directory, extension = "txt")
//
//                    if (allFiles.isNotEmpty()) {
//                        println("Найденные файлы:")
//                        for (file in allFiles) {
//                            println(file.absolutePath)
//                        }
//                    } else {
//                        println("Файлы не найдены.")
//                    }
//                } else {
//                    println("Разрешение не получено, обработайте ситуацию")
//                }
//            }
//        }
//    }
}