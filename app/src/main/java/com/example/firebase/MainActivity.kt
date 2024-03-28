package com.example.firebase

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.firebase.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var auth:FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private var REQUEST_CODE_SIGN_IN=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth= FirebaseAuth.getInstance()
        binding.btnSignIn.setOnClickListener {
            signInbyFirebase()
        }
        binding.btnLogin.setOnClickListener {
            LoginByFirebase()
        }
        binding.btnUpdate.setOnClickListener {
            UpdateProfile()
        }
        binding.btnSignWithEmail.setOnClickListener {
            LoginThroughEmail()
        }
    }

    private fun signInbyFirebase(){
        var email = binding.etEmail.text.toString()
        var password=binding.etPass.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.createUserWithEmailAndPassword(email,password).await()
                    withContext(Dispatchers.Main){
                      checkedstate()
                    }

                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
                    }

                }
            }
        }
    }private fun LoginByFirebase(){
        var email = binding.etEmail.text.toString()
        var password=binding.etPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.signInWithEmailAndPassword(email,password).await()
                    withContext(Dispatchers.Main){
                      checkedstate()
                    }

                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
                    }

                }
            }
        }
    }
    private fun UpdateProfile(){
        auth.currentUser?.let {user->
            var ename=binding.etEmail.text.toString()
            var photourl = Uri.parse("android.resource://$packageName/${R.drawable.ind}")
            var profileupdates= UserProfileChangeRequest.Builder()
                .setPhotoUri(photourl)
                .setDisplayName(ename)
                .build()
            CoroutineScope(Dispatchers.IO).launch{
                try {
                   user.updateProfile(profileupdates).await()
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,"Successfully Updated the profile",Toast.LENGTH_LONG).show()
                    }
                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
                    }

                }

            }
        }
    }

    private fun checkedstate(){
        var user = auth.currentUser
        if (user ==null){
            binding.tvResult.text = "You are not Logged In"
        }else{
            binding.tvResult.text = "You are  Logged In"
            binding.etEmail.setText(user.displayName)
            binding.ivImage.setImageURI(user.photoUrl)
        }
    }
    private fun LoginThroughEmail(){
          var options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              .requestIdToken(getString(R.string.webview_Client))
              .requestEmail()
              .build()
        val signinClient = GoogleSignIn.getClient(this,options)
        signinClient.signInIntent.also {
            startActivityForResult(it,REQUEST_CODE_SIGN_IN)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val account=GoogleSignIn.getSignedInAccountFromIntent(data).result

        account?.let { 
            googlrAuthForFirebase(it)
        }
    }

   private fun googlrAuthForFirebase(account: GoogleSignInAccount){
       val credentials = GoogleAuthProvider.getCredential(account.idToken,null)
       CoroutineScope(Dispatchers.IO).launch {
           try {
               auth.signInWithCredential(credentials).await()
               withContext(Dispatchers.Main){
                   Toast.makeText(this@MainActivity,"Signin Successfully Done",Toast.LENGTH_LONG).show()
               }
           }catch (e:Exception){
               withContext(Dispatchers.Main){
                   Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
               }

           }
       }
   }
}