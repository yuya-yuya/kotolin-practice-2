package com.example.sample2

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var translatorEJ: Translator
    lateinit var imageUri: Uri

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        textView.text = ""
        if (requestCode == 200 && resultCode == RESULT_OK) {//カメラ撮影後
            //imageViewに張り替える
            imageView.setImageURI(imageUri)
        } else if (requestCode == 300 && resultCode == RESULT_OK) {//ドキュメントの画像選択後
            //選択した画像をimageViewに張り替える
            imageView.setImageURI(data?.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.JAPANESE)
            .build()
        val translator = Translation.getClient(options)
        lifecycle.addObserver(translator)
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { textView.text = "翻訳可能"
            }
            .addOnFailureListener { exception ->
                textView.text = "辞書のダウンロードに失敗しました" }
        translatorEJ = translator
    }

    fun tapRecognitionButton(view: View) {
        val image = InputImage.fromBitmap(imageView.drawable.toBitmap(), 0)
        val recognizer = TextRecognition.getClient()
        lifecycle.addObserver(recognizer)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                editText.setText(visionText.text)
            }
            .addOnFailureListener { e ->
                editText.setText("画像認識に失敗しました")
            }
    }



    fun tapTranslateButton(view: View) {
        translatorEJ.translate(editText.text.toString())
            .addOnSuccessListener { translatedText ->
                textView.text = translatedText
            }
            .addOnFailureListener { exception ->
                textView.text = "翻訳に失敗しました"
            }
    }
    fun tapCameraButton(view: View) {
        //ストレージ利用許可が下りていない場合は許可を求める
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),2000)
            return
        }
        //保存する画像のファイル名の下準備
        val now = SimpleDateFormat("yyyyMMddHHmmss").format(Date(System.currentTimeMillis()))
        val fileName = "UseCameraActivityPhoto_" + now + ".jpg"
        //保存するデータの種類を指定
        var values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        //画像のデータを保存する位置(URI)を設定するため、作成したデータ(名前/種類)を指定する
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
        //カメラを起動
        var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent,200)
    }

    fun tapAlbumButton(view: View) {
        //アルバムから画像を選択できるようにする
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"//画像ファイルだけ見えるよう設定
        }
        startActivityForResult(intent, 300)
    }
}
