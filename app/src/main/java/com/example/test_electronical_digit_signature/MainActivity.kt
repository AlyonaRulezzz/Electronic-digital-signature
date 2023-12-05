package com.example.test_electronical_digit_signature

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_electronical_digit_signature.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity1"

    private lateinit var binding: ActivityMainBinding
    private var signturePath = ""
    private var signtureFileName = "noDocument_signature"
    private var filePath = ""
    private lateinit var privateKey: PrivateKey
    private lateinit var publicKey: PublicKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnFilePicker.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
//                showFileChooser()
                val firstResult: Deferred<String> = async { showFileChooser() }
                delay(10_000)
                // Запускаем вторую функцию только после завершения первой
                val secondResult = afterShowFileChooser(firstResult.await())
            }
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

//            // Регистрируем Bouncy Castle в качестве провайдера безопасности
////            Security.addProvider(BouncyCastleProvider())
//
            // Создаем генератор ключевой пары с использованием алгоритма, например, RSA
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
            keyPairGenerator.initialize(2048) // Укажите требуемую длину ключа

            // Генерируем ключевую пару
            val keyPair: KeyPair = keyPairGenerator.generateKeyPair()

            // Получаем приватный и публичный ключи
            privateKey = keyPair.private
            publicKey= keyPair.public

        }

    private fun afterShowFileChooser(await: String) {
        // Создаем подпись
        //            val signature = createSignature("путь_к_файлу_для_подписи.txt", privateKey)
        val signature = createSignature(filePath, privateKey)
        //
        // Сохраняем подпись в файл
        //            Files.write(Paths.get("путь_к_файлу_подписи.txt"), signature)
        //            Files.write(Paths.get("/document/primary:Download"), signature)
        //            File(signturePath).writeBytes(signature)
        println("signature1 = ${signature.contentToString()}")
        //            File("document/signaturePath.txt").writeBytes(signature)
        // Укажите путь и имя файла, который вы хотите создать
        //            createFile(filePath)
        createFile(this@MainActivity, signtureFileName, signature)


        //
        // Проверяем подпись
        //            val isSignatureValid = verifySignature("путь_к_файлу_для_подписи.txt", publicKey, signature)
        //            val isSignatureValid = verifySignature(publicKey, ???"signature.txt")
        val isSignatureValid = verifySignature(publicKey, signature)
        if (isSignatureValid) {
            println("Подпись верна.")
        } else {
            println("Подпись не верна.")
        }
    }

    //    private fun showFileChooser() {
    private suspend fun showFileChooser(): String {
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
            binding.tvFileInfo.text = "fileName = ${file.name}, path = $path".trimIndent()
            //
            signturePath = path.replace(file.name, "sign.txt")
        }
    }

    //
        private fun createSignature(filePath: String, privateKey: PrivateKey): ByteArray {
//        private fun createSignature(privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA", "BC")
            signature.initSign(privateKey)

//            val fileBytes = Files.readAllBytes(Paths.get(filePath))
//            val fileBytes = "Paths.get(filePath)".toByteArray()
            try {
//                val fileHash = getFileHash(filePath)
//                val hashByteArray = hexStringToByteArray(fileHash)
                val hashByteArray = File(filePath).hashCode().toString().toByteArray()

                // Теперь у вас есть массив байтов, представляющих хэш файла
                println("Хэш файла в виде массива байтов: ${hashByteArray.contentToString()}")
//                println("Хэш файла в виде массива байтов...")
                signature.update(hashByteArray)
            } catch (e: Exception) {
                println("Ошибка  кодирования хэш-кода: ${e.message}")
                Toast.makeText(this, "Ошибка кодирования хэш-кода", Toast.LENGTH_SHORT).show()
            }
//            signature.update(hashByteArray)

            return signature.sign()
        }
//
//        fun verifySignature(filePath: String, publicKey: PublicKey, signature: ByteArray): Boolean {
        fun verifySignature(publicKey: PublicKey, signature: ByteArray): Boolean {
            val verifier = Signature.getInstance("SHA256withRSA", "BC")
            verifier.initVerify(publicKey)

//            val fileBytes = Files.readAllBytes(Paths.get(filePath))
            val fileBytes = "Paths.get(filePath)".toByteArray()
            verifier.update(fileBytes)

            return verifier.verify(signature)
        }


//    fun createFile(filePath: String) {
//        val path: Path = Paths.get(filePath)
//
//        try {
//            Files.createFile(path)
//            println("Файл успешно создан: $filePath")
//        } catch (e: Exception) {
//            println("Ошибка при создании файла: ${e.message}")
//        }
//    }
    private fun createFile(context: Context, fileName: String, signature: ByteArray) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
//            put(MediaStore.MediaColumns.MIME_TYPE, "sig")
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
    fun getFileHash(filePath: String): String {
        val fileBytes = Files.readAllBytes(Paths.get(filePath))
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(fileBytes)

        // Преобразование массива байтов хэша в шестнадцатеричное представление
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        val result = ByteArray(hexString.length / 2)

        for (i in 0 until hexString.length step 2) {
            val byte = hexString.substring(i, i + 2).toInt(16).toByte()
            result[i / 2] = byte
        }

        return result
    }

}