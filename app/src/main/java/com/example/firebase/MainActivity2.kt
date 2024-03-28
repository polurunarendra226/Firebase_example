package com.example.firebase
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.firebase.databinding.ActivityMain2Binding
import com.example.firebase.databinding.ActivityMainBinding
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity2 : AppCompatActivity() {
    private  val REQUESt_CODE_PICK =0
    private var cuturi:Uri?=null

    private val imageref = Firebase.storage.reference
    private lateinit var binding:ActivityMain2Binding
    private val personCollectionref = Firebase.firestore.collection("persons")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
       // subscribeToRealtimeUpdates()
        binding.btnSaveToFirestore.setOnClickListener {
            var person = getOldPersondata()
            SaveToFirestore(person)
        }
        binding.btnRetriveData.setOnClickListener {retrivePersonData()  }
        binding.btnUpdateData.setOnClickListener {
            var oldperson = getOldPersondata()
            var newpersonmap = getNewPersonMap()
            getUpdatetheData(oldperson,newpersonmap)
        }

        var image = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
            cuturi=it
            binding.ivImageFirebase.setImageURI(it)
        }

        binding.ivImageFirebase.setOnClickListener {
            image.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        }
        binding.upload.setOnClickListener {
            UploadImageToFirebase("myImage")
        }
        binding.btnDowload.setOnClickListener {
            downloadfromfirebase("myImage")
        }

    }
    private fun downloadfromfirebase(filename: String)= CoroutineScope(Dispatchers.IO).launch {
        try {
            var maxSize = 5L * 1024 *1024
            var bytes = imageref.child("images/$filename").getBytes(maxSize).await()
            var bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.size)

            withContext(Dispatchers.Main){
                binding.ivImageFirebase.setImageBitmap(bmp)
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity2,e.message,Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun UploadImageToFirebase(filename:String)= CoroutineScope(Dispatchers.IO).launch {
        try {
            cuturi?.let {
                imageref.child("images/$filename").putFile(it).await()
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity2,"Successfully Uploaded",Toast.LENGTH_LONG).show()
                }
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity2,e.message,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getNewPersonMap():Map<String,Any>{
        var newfirst_name = binding.etNewfirstName.text.toString()
        var newlast_name = binding.etNewlastName.text.toString()
        var newage = binding.etNewAge.text.toString()

        var map = mutableMapOf<String,Any>()
        if (newfirst_name.isNotEmpty()){
            map["firest_name"]=newfirst_name
        }
        if (newlast_name.isNotEmpty()){
            map["last_name"]=newlast_name
        }
        if (newage.isNotEmpty()){
            map["age"]=newage.toInt()
        }
        return map
    }

    private fun getUpdatetheData(person: Person,newPersonMap:Map<String,Any>)= CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionref.whereEqualTo("last_name",person.last_name).whereEqualTo("firest_name",person.firest_name).whereEqualTo("age",person.age).get().await()
        if (personQuery.documents.isNotEmpty()){
            for (document in personQuery){
                try {
                    personCollectionref.document(document.id).set(
                        newPersonMap, SetOptions.merge()
                    ).await()
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity2,"Data Updated Successfullhy",Toast.LENGTH_LONG).show()
                    }
                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity2,e.message,Toast.LENGTH_LONG).show()
                    }
                }
            }
        }else{
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity2,"There is no data to Update",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getOldPersondata():Person{
        var first_name = binding.etFirstName.text.toString()
        var last_name = binding.etLastName.text.toString()
        var age = binding.etAge.text.toString().toInt()
        return Person(first_name,last_name,age)
    }

    private fun SaveToFirestore(person: Person){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                personCollectionref.add(person)
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity2,"successfully saved the data",Toast.LENGTH_LONG).show()
                }
            }catch (e:Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity2,e.message,Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun retrivePersonData()= CoroutineScope(Dispatchers.IO).launch {
        try {
            var querySnapshot = personCollectionref.get().await()
            var sb= StringBuilder()
            for (document in querySnapshot.documents){
                var person = document.toObject<Person>()
                sb.append("$person\n")
                withContext(Dispatchers.Main){
                   binding.tvPersonResult.text = sb.toString()
                }
            }

        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity2,e.message,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun subscribeToRealtimeUpdates(){
        personCollectionref.addSnapshotListener { querysnapshot, error ->
            error?.let {
                Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querysnapshot?.let {
                var sb= StringBuilder()
                for (document in it){
                    var person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                binding.tvPersonResult.text = sb.toString()
            }
        }
    }


}