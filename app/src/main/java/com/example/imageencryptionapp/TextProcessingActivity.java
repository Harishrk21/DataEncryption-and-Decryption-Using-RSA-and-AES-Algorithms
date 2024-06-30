package com.example.imageencryptionapp;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;  // Added import for Base64

public class TextProcessingActivity extends AppCompatActivity {

    private EditText inputEditText;
    private Button encryptButton;
    private Button decryptButton;
    private TextView outputTextView;

    private KeyPair keyPair;
    private SecretKey aesKey;
    private byte[] encryptedAESKey;
    private String encryptedTextBase64;  // Modified to store Base64-encoded string

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_processing);

        inputEditText = findViewById(R.id.inputEditText);
        encryptButton = findViewById(R.id.encryptButton);
        decryptButton = findViewById(R.id.decryptButton);
        outputTextView = findViewById(R.id.outputTextView);

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performEncryption();
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDecryption();
            }
        });

        // Generate RSA key pair and AES key during initialization
        generateKeys();
    }

    private void generateKeys() {
        try {
            // Generate RSA key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();

            // Generate AES key
            KeyGenerator aesKeyGenerator = KeyGenerator.getInstance("AES");
            aesKeyGenerator.init(256);
            aesKey = aesKeyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performEncryption() {
        try {
            // Encrypt AES key with RSA public key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

            // Text to be encrypted
            String plaintext = inputEditText.getText().toString();

            // Encrypt text using AES
            Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedText = aesCipher.doFinal(plaintext.getBytes());

            // Convert byte array to Base64 string for display
            encryptedTextBase64 = Base64.encodeToString(encryptedText, Base64.DEFAULT);

            inputEditText.setText("");

            // Display encrypted text with label
            outputTextView.setText("Encrypted Text: " + encryptedTextBase64);
            outputTextView.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performDecryption() {
        try {
            // Decrypt AES key with RSA private key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedAESKey = rsaCipher.doFinal(encryptedAESKey);

            // Use the original AES key size for SecretKeySpec
            SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedAESKey, "AES");

            // Decode Base64 string to byte array
            byte[] encryptedTextBytes = Base64.decode(encryptedTextBase64, Base64.DEFAULT);

            // Decrypt text using AES
            Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedText = aesCipher.doFinal(encryptedTextBytes);

            // Display decrypted text with label
            outputTextView.setText("Decrypted text: " + new String(decryptedText));
            outputTextView.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
