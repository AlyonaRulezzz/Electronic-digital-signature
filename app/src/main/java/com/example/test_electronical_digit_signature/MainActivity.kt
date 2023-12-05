package com.example.test_electronical_digit_signature

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_electronical_digit_signature.databinding.ActivityMainBinding
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity1"

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnFilePicker.setOnClickListener { showFileChooser() }

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
            val privateKey: PrivateKey = keyPair.private
            val publicKey: PublicKey = keyPair.public


////        val filePath = "\\home\\milkfist\\02.txt"
////        val filePath = "//home//milkfist//02.txt"
//        val filePath = "/home/milkfist/02.txt"
////        val filePath = "//home//milkfist//AndroidStudioProjects//Test_Electronical_Digit_Signature//app//ic_launcher_background.xml"
////        val filePath = "\\home\\milkfist\\AndroidStudioProjects\\Test_Electronical_Digit_Signature\\app\\ic_launcher_background.xml"
//        val path: Path = Paths.get(filePath)
//
//        if (Files.exists(path)) {
//            if (Files.isReadable(path)) {
//                println("Файл можно читать.")
//            } else {
//                println("У программы нет разрешения на чтение файла.")
//            }
//
//            if (Files.isWritable(path)) {
//                println("В файл можно писать.")
//            } else {
//                println("У программы нет разрешения на запись в файл.")
//            }
//        } else {
//            println("Файл не существует.")
//        }


//            // Создаем подпись
////            val signature = createSignature("путь_к_файлу_для_подписи.txt", privateKey)
//            val signature = createSignature("/home/milkfist/Загрузки/", privateKey)
            val signature = createSignature(privateKey)
//
            // Сохраняем подпись в файл
//            Files.write(Paths.get("путь_к_файлу_подписи.txt"), signature)
//
            // Проверяем подпись
//            val isSignatureValid = verifySignature("путь_к_файлу_для_подписи.txt", publicKey, signature)
            val isSignatureValid = verifySignature(publicKey, signature)
            if (isSignatureValid) {
                println("Подпись верна.")
            } else {
                println("Подпись не верна.")
            }
        }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100)
        } catch (exception: Exception) {
            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            val path: String = uri?.path.toString()
            val file = File(path)
            binding.tvFileInfo.text = "fileName = ${file.name}, path = $path".trimIndent()
        }
    }

    //
//        private fun createSignature(filePath: String, privateKey: PrivateKey): ByteArray {
        private fun createSignature(privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA", "BC")
            signature.initSign(privateKey)

//            val fileBytes = Files.readAllBytes(Paths.get(filePath))
            val fileBytes = "Paths.get(filePath)".toByteArray()
            signature.update(fileBytes)

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

}